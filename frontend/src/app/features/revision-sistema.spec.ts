import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors, HttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { PresupuestosComponent } from './presupuestos/presupuestos.component';
import { TransaccionesComponent } from './transacciones/transacciones.component';
import { TraspasosComponent } from './traspasos/traspasos.component';
import { errorInterceptor } from '../core/interceptors/error.interceptor';
import { mensajeErrorMeta } from './metas/meta.service';

describe('Regresiones de operaciones y consultas', () => {
  let http: HttpTestingController;
  const api = 'http://localhost:8080/api/';
  beforeEach(() => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting(), provideRouter([])] });
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  it('mantiene el mes seleccionado aunque la carga inicial termine después', () => {
    const c = TestBed.createComponent(PresupuestosComponent).componentInstance;
    c.ngOnInit();
    const inicial = http.expectOne(r => r.url === api + 'presupuestos');
    http.expectOne(r => r.url === api + 'categorias').flush([]);
    c.periodo.controls.mes.setValue(2);
    const actual = http.expectOne(r => r.url === api + 'presupuestos');
    actual.flush([{ id: 2 }]);
    inicial.flush([{ id: 1 }]);
    expect(c.presupuestos.map(p => p.id)).toEqual([2]);
    expect(c.cargando).toBeFalse();
  });

  it('ignora el error de una consulta de presupuestos que ya fue reemplazada', () => {
    const c = TestBed.createComponent(PresupuestosComponent).componentInstance;
    c.periodo.controls.mes.setValue(1);
    const anterior = http.expectOne(r => r.url === api + 'presupuestos');
    c.periodo.controls.mes.setValue(2);
    http.expectOne(r => r.url === api + 'presupuestos').flush([{ id: 2 }]);
    anterior.flush({}, { status: 500, statusText: 'Error' });
    expect(c.error).toBeNull();
    expect(c.presupuestos.map(p => p.id)).toEqual([2]);
  });

  it('evita que un filtro antiguo reemplace los movimientos actuales', () => {
    const c = TestBed.createComponent(TransaccionesComponent).componentInstance;
    c.filtrosForm.controls.tipo.setValue('INGRESO'); c.buscar();
    const anterior = http.expectOne(r => r.url.startsWith(api + 'transacciones'));
    c.filtrosForm.controls.tipo.setValue('GASTO'); c.buscar();
    http.expectOne(r => r.url.startsWith(api + 'transacciones')).flush([{ id: 2 }]);
    anterior.flush([{ id: 1 }]);
    expect(c.transacciones.map(t => t.id)).toEqual([2]);
  });

  it('consulta un mes anterior incluyendo su último día y permite volver a todo el historial', () => {
    const c = TestBed.createComponent(TransaccionesComponent).componentInstance;
    c.mesHistorial.setValue('2024-02'); c.verMes();
    const consulta = http.expectOne(r => r.url === api + 'transacciones');
    expect(consulta.request.params.get('desde')).toBe('2024-02-01');
    expect(consulta.request.params.get('hasta')).toBe('2024-02-29');
    consulta.flush([]);
    expect(c.filtrosActivos).toBeTrue();
    c.limpiar();
    http.expectOne(api + 'transacciones').flush([]);
    expect(c.mesHistorial.value).toBe('');
    expect(c.filtrosActivos).toBeFalse();
  });

  it('envía una sola transferencia y permite reintentar después de un error', () => {
    const c = TestBed.createComponent(TraspasosComponent).componentInstance;
    c.form.patchValue({ billeteraOrigenId: 1, billeteraDestinoId: 2, monto: 10 });
    c.transferir(); c.transferir();
    http.expectOne(api + 'traspasos').flush({}, { status: 422, statusText: 'Error' });
    expect(c.guardando).toBeFalse();
    c.transferir();
    http.expectOne(api + 'traspasos').flush({}, { status: 422, statusText: 'Error' });
  });

  it('envía un solo movimiento mientras espera respuesta', () => {
    const c = TestBed.createComponent(TransaccionesComponent).componentInstance;
    c.form.patchValue({ billeteraId: 1, categoriaId: 1, monto: 10 });
    c.registrar(); c.registrar();
    http.expectOne(api + 'transacciones').flush({}, { status: 422, statusText: 'Error' });
    expect(c.guardando).toBeFalse();
  });

  it('muestra un ingreso con fecha antigua aunque hubiera filtros y más de veinte movimientos', () => {
    const c = TestBed.createComponent(TransaccionesComponent).componentInstance;
    c.filtrosForm.controls.tipo.setValue('GASTO');
    c.filtrosActivos = true;
    c.form.patchValue({ billeteraId: 1, categoriaId: 1, monto: 10, tipo: 'INGRESO', fecha: '2026-01-01' });
    // Elegir el tipo limpia la categoría; el usuario la selecciona después.
    c.form.controls.categoriaId.setValue(1);
    c.registrar();
    http.expectOne(api + 'transacciones').flush({ id: 99, alertaPresupuesto: null });
    http.expectOne(api + 'billeteras').flush([]);
    http.expectOne(api + 'categorias').flush([]);
    const consulta = http.expectOne(api + 'transacciones');
    expect(consulta.request.params.keys()).toEqual([]);
    consulta.flush([...Array.from({ length: 25 }, (_, i) => ({ id: i + 1 })), { id: 99 }]);
    expect(c.filtrosActivos).toBeFalse();
    expect(c.pagina).toBe(1);
    expect(c.recientes.some(t => t.id === 99)).toBeTrue();
    c.cambiarPagina(0);
    expect(c.recientes.length).toBe(20);
    c.cambiarPagina(1);
    expect(c.recientes.length).toBe(6);
    c.limpiar();
    http.expectOne(api + 'transacciones').flush([]);
    expect(c.pagina).toBe(0);
  });

  it('muestra el motivo real de un aporte rechazado después del interceptor', () => {
    let mensaje = '';
    TestBed.inject(HttpClient).post(api + 'metas/1/aportes', {}).subscribe({ error: e => mensaje = mensajeErrorMeta(e) });
    http.expectOne(api + 'metas/1/aportes').flush({ message: 'La billetera no tiene saldo suficiente.' }, { status: 422, statusText: 'Error' });
    expect(mensaje).toBe('La billetera no tiene saldo suficiente.');
  });

  it('no expone mensajes internos del servidor', () => {
    let mensaje = '';
    TestBed.inject(HttpClient).get(api + 'metas').subscribe({ error: e => mensaje = e.error });
    http.expectOne(api + 'metas').flush({ message: 'SQLException: datos internos' }, { status: 500, statusText: 'Error' });
    expect(mensaje).not.toContain('SQLException');
  });
});
