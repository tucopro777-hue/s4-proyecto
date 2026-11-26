"""Carga local, aditiva e idempotente. Sin borrado, cambio de claves ni datos personales.
Requiere backend con perfil demo-local. Sin --aplicar solo comprueba el entorno.
"""
import argparse
import calendar
import collections
import datetime as dt
import json
import os
from pathlib import Path
import re
import subprocess
import urllib.request
import urllib.error
from decimal import Decimal as D

ROOT = Path(__file__).resolve().parent.parent
API = 'http://localhost:8080/api'
MARCA = 'DEMO COMPLETA v1'
EMAIL = 'demo@finanzas.test'
REPORTE = ROOT / 'inicio' / 'demo-completa-reporte.json'
ANCLA = ROOT / 'inicio' / 'demo-completa-periodo.json'

def pedir(ruta, datos=None):
    cuerpo = None if datos is None else json.dumps(datos, ensure_ascii=False).encode('utf-8')
    req = urllib.request.Request(API + '/' + ruta, data=cuerpo,
        headers={'Content-Type': 'application/json', **CABECERAS})
    try:
        with urllib.request.urlopen(req, timeout=30) as r:
            return json.load(r)
    except urllib.error.HTTPError as e:
        detalle = json.loads(e.read().decode('utf-8'))
        raise RuntimeError(f'{ruta}: HTTP {e.code}: {detalle.get("message", "Solicitud rechazada")}') from None

def sql(consulta):
    exe = Path(r'C:\Program Files\PostgreSQL\16\bin\psql.exe')
    entorno = os.environ.copy()
    entorno['PGPASSWORD'] = os.environ.get('DB_PASSWORD', 'postgres')
    r = subprocess.run([str(exe), '-X', '-A', '-t', '-h', 'localhost', '-p', '5432',
        '-U', os.environ.get('DB_USER', 'postgres'), '-d', 'finanzas', '-v', 'ON_ERROR_STOP=1', '-c', consulta],
        env=entorno, capture_output=True, text=True, check=True)
    return r.stdout.strip()

def huellas_otros():
    tablas = ['usuarios','billeteras','categorias','transacciones','presupuestos','metas_ahorro','aportes_meta','traspasos']
    huellas = {}
    for tabla in tablas:
        condicion = "email <> 'demo@finanzas.test'" if tabla == 'usuarios' else "usuario_id <> (SELECT id FROM usuarios WHERE email='demo@finanzas.test')"
        huellas[tabla] = sql(f"SELECT count(*) || ':' || md5(coalesce(string_agg(to_jsonb(t)::text, '|' ORDER BY id),'')) FROM {tabla} t WHERE {condicion}")
    return huellas

def mes_desde(ancla, diferencia):
    indice = ancla.year * 12 + ancla.month - 1 + diferencia
    return dt.date(indice // 12, indice % 12 + 1, 1)

def importe(n):
    return float(D(str(n)).quantize(D('.01')))

def suma_filas(filas, campo='monto'):
    return sum((D(str(f[campo])) for f in filas), D(0))

def cargar():
    antes_otros = huellas_otros()
    hoy = dt.date.today()
    if ANCLA.exists():
        ancla = dt.date.fromisoformat(json.loads(ANCLA.read_text())['mes'])
    else:
        ancla = hoy.replace(day=1)
        ANCLA.write_text(json.dumps({'mes':ancla.isoformat(), 'version':1}), encoding='utf-8')
    if ancla > hoy: raise RuntimeError('El período demo no puede estar en el futuro')
    inicial = {r:pedir(r) for r in ['billeteras','categorias','transacciones','metas','traspasos']}
    creados = collections.Counter()
    cats = {c['nombre']:c for c in inicial['categorias']}
    for nombre,tipo,icono in [('Salario','INGRESO','salary'),('Trabajos independientes','INGRESO','salary'),
            ('Ingresos adicionales','INGRESO','other'),('Comida','GASTO','food'),('Transporte','GASTO','transport'),
            ('Servicios','GASTO','bills'),('Educación','GASTO','education'),('Entretenimiento','GASTO','leisure'),
            ('Salud','GASTO','health'),('Vivienda','GASTO','home')]:
        if nombre not in cats:
            cats[nombre] = pedir('categorias', dict(nombre=nombre,tipo=tipo,color='#F0B90B',icono=icono))
            creados['categorias'] += 1
        if cats[nombre]['tipo'] != tipo: raise RuntimeError('Categoría demo de tipo incompatible: '+nombre)
    wallets = {b['nombre']:b for b in inicial['billeteras']}
    for nombre in ['Caja BOB','Banco BOB','DEMO Ahorros BOB']:
        if nombre not in wallets:
            wallets[nombre] = pedir('billeteras',dict(nombre=nombre,saldoInicial=0,divisa='BOB'))
            creados['billeteras'] += 1
        if wallets[nombre]['divisa'] != 'BOB': raise RuntimeError('Billetera demo en moneda incorrecta')
    banco, caja, ahorro = [wallets[n] for n in ['Banco BOB','Caja BOB','DEMO Ahorros BOB']]
    txs = inicial['transacciones'][:]
    traspasos = inicial['traspasos'][:]
    comprobaciones_traspasos = []

    def saldo(b):
        return next(D(str(x['saldoActual'])) for x in pedir('billeteras') if x['id']==b['id'])

    def transferir(origen,destino,monto,fecha,clave):
        descripcion = f'{MARCA} {clave}'
        if any(t['descripcion']==descripcion for t in traspasos): return
        antes = pedir('billeteras')
        suma_antes = suma_filas([b for b in antes if b['divisa']=='BOB'],'saldoActual')
        origen_antes = next(D(str(b['saldoActual'])) for b in antes if b['id']==origen['id'])
        destino_antes = next(D(str(b['saldoActual'])) for b in antes if b['id']==destino['id'])
        t = pedir('traspasos',dict(billeteraOrigenId=origen['id'],billeteraDestinoId=destino['id'],
            monto=importe(monto),fecha=fecha.isoformat(),descripcion=descripcion))
        despues = pedir('billeteras')
        assert suma_antes == suma_filas([b for b in despues if b['divisa']=='BOB'],'saldoActual'), 'El traspaso alteró el total'
        assert saldo(origen)==origen_antes-D(str(monto)) and saldo(destino)==destino_antes+D(str(monto))
        traspasos.append(t);creados['traspasos'] += 1
        comprobaciones_traspasos.append(dict(descripcion=descripcion,monto=importe(monto),totalConservado=True))

    def movimiento(categoria,tipo,monto,fecha,clave,billetera):
        descripcion = re.sub(r'\bdemo\b\s*', '', f'{MARCA} {clave}', flags=re.I).strip()
        if any(re.sub(r'\bdemo\b\s*', '', t['descripcion'], flags=re.I).strip()
               == re.sub(r'\bdemo\b\s*', '', descripcion, flags=re.I).strip() for t in txs): return
        if fecha > hoy: raise RuntimeError('Movimiento futuro rechazado')
        if monto<=0: return
        t = pedir('transacciones',dict(monto=importe(monto),tipo=tipo,descripcion=descripcion,fecha=fecha.isoformat(),
            billeteraId=billetera['id'],categoriaId=cats[categoria]['id']))
        txs.append(t);creados['ingresos' if tipo=='INGRESO' else 'gastos'] += 1

    # Doce meses cerrados, más el mes en curso. Los registros anteriores se conservan.
    estados = ['E','D','E','E','D','E','E','D','D','E','D','E']
    for i in range(13):
        mes = mes_desde(ancla,i-12); periodo=mes.strftime('%Y-%m')
        corriente = i==12
        ingresos = {'Salario':4200,'Trabajos independientes':600+(i%3)*150,'Ingresos adicionales':300+(i%4)*100}
        def existentes(nombre,tipo):
            return suma_filas([t for t in txs if t['fecha'].startswith(periodo) and t['categoriaId']==cats[nombre]['id'] and t['tipo']==tipo])
        for k,(nombre,objetivo) in enumerate(ingresos.items()):
            dia=min([1,8,12][k], hoy.day) if corriente else [1,8,12][k]
            movimiento(nombre,'INGRESO',D(objetivo)-existentes(nombre,'INGRESO'),mes.replace(day=dia),f'{periodo} ingreso {nombre}',banco)
        deficit = not corriente and estados[i]=='D'
        gastos = {'Comida':650 if corriente else 1000,'Transporte':180 if corriente else 300,
            'Servicios':400,'Educación':220 if corriente else 300,'Entretenimiento':275 if corriente else 200,
            'Salud':150 if corriente else (650 if deficit else 200)}
        gastos = {n:max(D(v),existentes(n,'GASTO')) for n,v in gastos.items()}
        ingreso_total = suma_filas([t for t in txs if t['fecha'].startswith(periodo) and t['tipo']=='INGRESO'])
        objetivo_gasto = D(3075) if corriente else ingreso_total + (D(600) if deficit else D(-700))
        gastos['Vivienda'] = max(D(0),objetivo_gasto-sum(gastos.values()))
        pendientes = {n:max(D(0),v-existentes(n,'GASTO')) for n,v in gastos.items()}
        efectivo = sum(pendientes[n] for n in ['Comida','Transporte','Entretenimiento'])
        necesario=max(D(0),efectivo+D(100)-saldo(caja))
        if necesario: transferir(banco,caja,necesario,mes.replace(day=1),f'{periodo} efectivo')
        for k,(nombre,monto) in enumerate(pendientes.items()):
            billetera = caja if nombre in ['Comida','Transporte','Entretenimiento'] else banco
            dia=min(5+k*3,hoy.day) if corriente else 5+k*3
            movimiento(nombre,'GASTO',monto,mes.replace(day=dia),f'{periodo} gasto {nombre}',billetera)
        if not corriente:
            transferir(banco,ahorro,D(100),mes.replace(day=25),f'{periodo} ahorro')
        presupuestos = pedir(f'presupuestos?anio={mes.year}&mes={mes.month}')
        for nombre,limite in [('Comida',800),('Transporte',300),('Entretenimiento',250),('Servicios',400)]:
            if not any(p['categoriaId']==cats[nombre]['id'] for p in presupuestos):
                pedir('presupuestos',dict(categoriaId=cats[nombre]['id'],montoLimite=limite,anio=mes.year,mes=mes.month))
                creados['presupuestos']+=1
    metas=pedir('metas')
    for nombre,objetivo,avance,meses in [('DEMO Comprar computadora',7000,500,8),('DEMO Fondo de emergencia',5000,400,12),('DEMO Viaje',4000,250,6)]:
        nombre = re.sub(r'\bdemo\b\s*', '', nombre, flags=re.I).strip()
        meta=next((m for m in metas if re.sub(r'\bdemo\b\s*', '', m['nombre'], flags=re.I).strip()==nombre),None)
        if meta is None:
            fecha=mes_desde(ancla,meses)
            meta=pedir('metas',dict(nombre=nombre,montoObjetivo=objetivo,fechaLimite=fecha.isoformat(),porcentajeAutomatico=None))
            metas.append(meta);creados['metas']+=1
        diferencia=D(avance)-D(str(meta['montoActual']))
        if diferencia>0:
            pedir(f'metas/{meta["id"]}/aportes',dict(billeteraId=banco['id'],monto=importe(diferencia)))
            creados['aportesManuales']+=1
    # Conciliación de TODAS las billeteras demo según las entidades reales, solo lectura.
    conciliacion = sql("""SELECT count(*) FROM billeteras b WHERE usuario_id=(SELECT id FROM usuarios WHERE email='demo@finanzas.test') AND saldo_actual <>
      saldo_inicial + coalesce((SELECT sum(CASE WHEN tipo='INGRESO' THEN monto ELSE -monto END) FROM transacciones WHERE billetera_id=b.id AND NOT anulada),0)
      + coalesce((SELECT sum(monto) FROM traspasos WHERE billetera_destino_id=b.id),0)
      - coalesce((SELECT sum(monto) FROM traspasos WHERE billetera_origen_id=b.id),0)
      - coalesce((SELECT sum(monto) FROM aportes_meta WHERE billetera_id=b.id),0)""")
    assert conciliacion=='0', 'Hay billeteras que no concilian'
    markov=pedir('dashboard/analisis/markov?divisa=BOB')
    assert len(markov['historial'])>=12 and markov['probabilidades'], 'Historial insuficiente'
    frecuencias=[[0,0],[0,0]]
    for a,b in zip(markov['historial'],markov['historial'][1:]):
        if mes_desde(dt.date.fromisoformat(a['mes']+'-01'),1).strftime('%Y-%m')==b['mes']:
            frecuencias[a['estado']=='DEFICIT'][b['estado']=='DEFICIT']+=1
    for fila,esperado in zip(markov['matriz'],frecuencias):
        assert [fila['aEstable'],fila['aDeficit']]==esperado
    presupuestos_actuales=pedir(f'presupuestos?anio={ancla.year}&mes={ancla.month}')
    for p in presupuestos_actuales:
        real=suma_filas([t for t in txs if t['fecha'].startswith(ancla.strftime('%Y-%m')) and t['tipo']=='GASTO' and t['categoriaId']==p['categoriaId']])
        assert real==D(str(p['saldoConsumido'])), 'Consumo de presupuesto inconsistente'
    disponible=D(str(pedir('dashboard/analisis/disponible?divisa=BOB')))
    casos=[]
    # Mismas probabilidades de Markov; resultados monetarios explícitamente hipotéticos.
    for caso,cambios in [('A: favorecer ahorro',[(0,0),(60,10),(20,-200)]),('B: favorecer inversión',[(0,0),(10,0),(200,-20)])]:
        alternativas=[dict(nombre=n,asignacion=0 if i==0 else 500,resultadoEstable=e,resultadoDeficit=d)
            for i,(n,(e,d)) in enumerate(zip(['Guardar mi dinero','Ahorrar','Invertir'],cambios))]
        v=pedir('dashboard/analisis/vme',dict(divisa='BOB',reservaMinima=importe(disponible-500),alternativas=alternativas))
        assert v['markov']['probabilidades']==markov['probabilidades']
        casos.append(dict(caso=caso,demostracion=True,resultado=v))
    assert casos[0]['resultado']['recomendadas']==['Ahorrar'] and casos[1]['resultado']['recomendadas']==['Invertir']
    despues_otros=huellas_otros()
    assert antes_otros==despues_otros, 'Cambió información ajena a demo'
    dashboard=pedir('dashboard/resumen')
    assert D(str(dashboard['saldoTotal']))==disponible and dashboard['divisa']=='BOB', 'El Dashboard mezcla monedas'
    reporte=dict(entorno=ENTORNO,periodo=ancla.isoformat(),creados=dict(creados),
        totales=dict(ingresos=sum(t['tipo']=='INGRESO' for t in txs),gastos=sum(t['tipo']=='GASTO' for t in txs)),
        billeteras=pedir('billeteras'),categorias=pedir('categorias'),metas=pedir('metas'),
        presupuestosActuales=presupuestos_actuales,traspasos=pedir('traspasos'),
        traspasosVerificados=comprobaciones_traspasos,markov=markov,vme=casos,
        auditoria=dict(otrasCuentasSinCambios=True,billeterasDescuadradas=0,
            huellasAntes=antes_otros,huellasDespues=despues_otros),dashboard=dashboard)
    REPORTE.write_text(json.dumps(reporte,ensure_ascii=False,indent=2),encoding='utf-8')
    print(json.dumps(dict(entorno=ENTORNO,creados=dict(creados),totales=reporte['totales'],
        meses=len(markov['historial']),probabilidades=markov['probabilidades'],
        ganadoras=[c['resultado']['recomendadas'] for c in casos],
        auditoria=dict(otrasCuentasSinCambios=True,billeterasDescuadradas=0)),ensure_ascii=False))

if __name__=='__main__':
    parser=argparse.ArgumentParser(description=__doc__);parser.add_argument('--aplicar',action='store_true');args=parser.parse_args()
    # Reutiliza la configuración de acceso local existente. Nunca imprime claves ni tokens.
    fuente=(ROOT/'backend/src/main/java/com/finanzas/config/DatosDemoInicializador.java').read_text(encoding='utf-8')
    password=os.environ.get('FINDEX_DEMO_PASSWORD') or re.search(r'PASSWORD\s*=\s*"([^"]+)"',fuente).group(1)
    CABECERAS={}
    sesion=pedir('auth/login',dict(email=EMAIL,password=password))
    assert sesion['email']==EMAIL, 'Solo se permite la cuenta demo existente'
    CABECERAS={'Authorization':'Bearer '+sesion['token']}
    ENTORNO=pedir('demo/entorno')
    assert ENTORNO.get('cargaPermitida') is True and ENTORNO.get('base')=='finanzas'
    if not args.aplicar: print(json.dumps(ENTORNO));raise SystemExit(0)
    # Evita dos cargas simultáneas desde el mismo equipo. No hay opción de borrado.
    import msvcrt
    with (ROOT/'inicio/.demo-completa.lock').open('a+b') as bloqueo:
        bloqueo.write(b'0');bloqueo.flush();bloqueo.seek(0)
        msvcrt.locking(bloqueo.fileno(),msvcrt.LK_NBLCK,1)
        cargar()
