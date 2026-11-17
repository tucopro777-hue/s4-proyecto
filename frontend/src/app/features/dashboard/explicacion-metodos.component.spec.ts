import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { ExplicacionMetodosComponent } from './explicacion-metodos.component';
import { AnalisisFinancieroComponent } from './analisis-financiero.component';
import { EscenariosFinancierosStore, EscenariosConfigurados } from './escenarios-financieros.store';
import { Markov } from './analisis-financiero.service';

describe('Pantalla separada de métodos', () => {
  const url = 'http://localhost:8080/api/dashboard/analisis';
  const m: Markov = { divisa: 'BOB', mesActual: '2026-11', estadoActual: null, mesBase: '2026-11',
    mesPredicho: '2026-12', estadoBase: 'ESTABLE', historial: [],
    matriz: [{ origen: 'ESTABLE', aEstable: 4, aDeficit: 1, total: 5, pEstable: .8, pDeficit: .2 }],
    probabilidades: { estable: .8, deficit: .2 }, explicacion: 'Historial de prueba' };
  beforeEach(() => TestBed.configureTestingModule({ imports: [ExplicacionMetodosComponent, AnalisisFinancieroComponent, HttpClientTestingModule], providers: [provideRouter([])] }));

  function cargar() {
    const http = TestBed.inject(HttpTestingController);
    http.expectOne(`${url}/divisas`).flush(['BOB']);
    http.expectOne(`${url}/markov?divisa=BOB`).flush(m);
    http.expectOne(`${url}/disponible?divisa=BOB`).flush(1000);
    return http;
  }

  it('mantiene la configuración y elimina el apartado matemático solicitado', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar(); f.detectChanges();
    expect(f.nativeElement.querySelector('h1').textContent).toBe('Explicación de los métodos');
    expect(f.nativeElement.querySelectorAll('.campos-escenario input').length).toBe(6);
    expect(f.nativeElement.querySelectorAll('table,.detalle').length).toBe(0);
    expect(f.nativeElement.textContent).not.toContain('Detalles matemáticos para tu exposición');
    expect(f.nativeElement.textContent).toContain('Prueba tus opciones de dinero');
    expect([...f.nativeElement.querySelectorAll('legend')].map((e: any) => e.textContent.trim()))
      .toEqual(['Guardar', 'Ahorrar', 'Invertir']);
    expect(f.nativeElement.textContent).toContain('Si te va bien económicamente');
    expect(f.nativeElement.textContent).toContain('Si gastas más de lo que ganas');
    expect(f.nativeElement.querySelector('#como-markov').textContent).toBe('¿Cómo calculamos tu futuro financiero?');
    expect(f.nativeElement.querySelector('#como-vme').textContent).toBe('¿Cómo elegimos la mejor opción para tu dinero?');
    http.verify();
  });

  it('muestra solo el aviso breve de Markov cuando no hay predicción', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar();
    f.componentInstance.markov = { ...m, probabilidades: null }; f.detectChanges();
    const seccion = f.nativeElement.querySelector('[aria-labelledby="como-markov"]');
    expect(seccion.querySelectorAll('p').length).toBe(1);
    expect(seccion.querySelector('p').textContent.trim()).toBe('Necesitamos más meses registrados para calcular una predicción.');
    expect(f.nativeElement.querySelectorAll('.detalle').length).toBe(0);
    http.verify();
  });

  it('restaura escenarios al volver al Dashboard y vuelve a consultar el backend al evaluar', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar();
    f.componentInstance.cargarEjemplo();
    const req = http.expectOne(`${url}/vme`);
    f.componentInstance.invalidar(); req.flush({}); f.destroy();
    const dashboard = TestBed.createComponent(AnalisisFinancieroComponent); dashboard.detectChanges(); cargar(); dashboard.detectChanges();
    expect(dashboard.componentInstance.valido).toBeTrue();
    expect(dashboard.componentInstance.montoEvaluar).toBe(500);
    expect(dashboard.nativeElement.querySelectorAll('table,fieldset,details').length).toBe(0);
    expect(dashboard.componentInstance.vme).toBeNull();
    dashboard.componentInstance.evaluar();
    const nuevo = http.expectOne(`${url}/vme`);
    expect(nuevo.request.body.probabilidades).toBeUndefined();
    dashboard.componentInstance.invalidar(); nuevo.flush({}); http.verify();
  });

  it('no mezcla escenarios de diferentes sesiones o monedas y entrega copias independientes', () => {
    const store = TestBed.inject(EscenariosFinancierosStore);
    const datos: EscenariosConfigurados = { monto: 500, ejemplo: true, alternativas: [] };
    store.guardar('sesion-ana', 'BOB', datos);
    datos.monto = 999;
    expect(store.leer('sesion-ana', 'BOB')?.monto).toBe(500);
    expect(store.leer('sesion-ana', 'USD')).toBeUndefined();
    expect(store.leer('sesion-luis', 'BOB')).toBeUndefined();
    expect(store.preferida('sesion-luis')).toBeUndefined();
  });

  it('explica el bloqueo, acepta ceros y muestra el resultado después de escribir y comparar', async () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar();
    f.componentInstance.avanzadoAbierto = true; f.detectChanges(); await f.whenStable();
    const escribir = (selector: string, valor: string) => {
      const input = f.nativeElement.querySelector(selector) as HTMLInputElement;
      input.value = valor; input.dispatchEvent(new Event('input', { bubbles: true })); f.detectChanges();
    };
    ['#cubres-0','#faltante-0','#cubres-1','#faltante-1','#cubres-2','#faltante-2'].forEach(s => escribir(s,'0'));
    expect(f.nativeElement.querySelector('#motivo-comparacion').textContent).toContain('Indica cuánto dinero');
    escribir('#monto-metodos','400'); await f.whenStable(); f.detectChanges();
    const boton = f.nativeElement.querySelector('.acciones button') as HTMLButtonElement;
    expect(boton.disabled).toBeFalse(); boton.click();
    const req = http.expectOne(`${url}/vme`);
    expect(req.request.body.alternativas[0].resultadoEstable).toBe(0);
    req.flush({ divisa:'BOB',disponible:1000,reservaMinima:600,markov:m,
      alternativas:req.request.body.alternativas.map((a: any) => ({ ...a,liquidezRestante:1000-a.asignacion,
        patrimonioEstable:1000,patrimonioDeficit:1000,viable:true,motivo:'Cumple',vme:1000 })),
      recomendadas:req.request.body.alternativas.map((a: any) => a.nombre),explicacion:'Empate' });
    f.detectChanges();
    expect(f.nativeElement.querySelector('.recomendacion').textContent).toContain('Mejor opción:');
    expect(f.nativeElement.querySelector('.recomendacion').textContent).toContain('1,000.00');
    expect(boton.disabled).toBeFalse(); http.verify();
  });

  it('distingue monto excesivo, importes pendientes, decimales inválidos y falta de historial', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar();
    const c = f.componentInstance;
    c.montoEvaluar = 1001; expect(c.motivoComparacion).toContain('superar tu saldo');
    c.montoEvaluar = 400; expect(c.motivoComparacion).toContain('6 importes pendientes');
    c.alternativas.forEach(a => { a.resultadoEstable=0; a.resultadoDeficit=0; });
    c.alternativas[1].resultadoEstable=.001; expect(c.motivoComparacion).toContain('dos decimales');
    c.alternativas[1].resultadoEstable=0; c.markov={...m,probabilidades:null};
    expect(c.motivoComparacion).toContain('más historial en BOB');
    c.evaluar();http.expectNone(`${url}/vme`);http.verify();
  });

  it('recupera el botón y muestra el error si falla la comparación', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent); f.detectChanges(); const http = cargar();
    f.componentInstance.cargarEjemplo();
    http.expectOne(`${url}/vme`).flush('No se pudo completar la comparación', {status:500,statusText:'Error'});
    expect(f.componentInstance.evaluando).toBeFalse();
    expect(f.componentInstance.error).toContain('No se pudo completar');
    f.componentInstance.invalidar();expect(f.componentInstance.error).toBe('');http.verify();
  });

  it('envía dos casos de demostración diferentes sin sustituir probabilidades', () => {
    const f = TestBed.createComponent(ExplicacionMetodosComponent);f.detectChanges();const http=cargar();
    f.componentInstance.cargarCasoDemo('ahorro');
    const ahorro=http.expectOne(`${url}/vme`);
    expect(ahorro.request.body.alternativas[1].resultadoEstable).toBe(60);
    expect(ahorro.request.body.alternativas[2].resultadoDeficit).toBe(-200);
    expect(ahorro.request.body.probabilidades).toBeUndefined();
    f.componentInstance.invalidar();ahorro.flush({});
    f.componentInstance.cargarCasoDemo('inversion');
    const inversion=http.expectOne(`${url}/vme`);
    expect(inversion.request.body.alternativas[2].resultadoEstable).toBe(200);
    expect(inversion.request.body.alternativas[2].resultadoDeficit).toBe(-20);
    f.componentInstance.invalidar();inversion.flush({});http.verify();
  });
});
