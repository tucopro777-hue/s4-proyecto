import { TestBed, ComponentFixture } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { AnalisisFinancieroComponent } from './analisis-financiero.component';
import { Markov, Vme } from './analisis-financiero.service';
import { provideRouter } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

describe('AnalisisFinancieroComponent', () => {
  let fixture: ComponentFixture<AnalisisFinancieroComponent>;
  let component: AnalisisFinancieroComponent;
  let http: HttpTestingController;
  const url = 'http://localhost:8080/api/dashboard/analisis';
  const markov: Markov = {
    divisa: 'BOB', mesActual: '2026-07', estadoActual: { mes: '2026-07', ingresos: 100, gastos: 0, estado: 'ESTABLE' },
    mesBase: '2026-07', mesPredicho: '2026-08', estadoBase: 'ESTABLE', historial: [],
    matriz: [{ origen: 'ESTABLE', aEstable: 1, aDeficit: 1, total: 2, pEstable: .5, pDeficit: .5 },
      { origen: 'DEFICIT', aEstable: 0, aDeficit: 0, total: 0, pEstable: null, pDeficit: null }],
    probabilidades: { estable: .5, deficit: .5 }, explicacion: 'Resultado de prueba',
  };

  beforeEach(() => {
    TestBed.configureTestingModule({ imports: [AnalisisFinancieroComponent, HttpClientTestingModule], providers: [provideRouter([])] });
    fixture = TestBed.createComponent(AnalisisFinancieroComponent);
    component = fixture.componentInstance;
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());

  function iniciar() {
    fixture.detectChanges();
    http.expectOne(`${url}/divisas`).flush(['BOB', 'USD']);
    http.expectOne(`${url}/markov?divisa=BOB`).flush(markov);
    http.expectOne(`${url}/disponible?divisa=BOB`).flush(1000);
    fixture.detectChanges();
  }
  function parametros() {
    component.montoEvaluar = 400;
    component.alternativas.forEach((a, i) => {
      a.asignacion = i * 100; a.resultadoEstable = i * 20; a.resultadoDeficit = -i * 10;
    });
  }

  it('presenta lenguaje cotidiano y oculta los detalles matemáticos inicialmente', () => {
    iniciar();
    const texto = fixture.nativeElement.textContent;
    expect(texto).toContain('Tus ingresos cubren tus gastos');
    expect(texto).toContain('1,000.00'); expect(texto).toContain('SIMULACIÓN');
    expect(fixture.nativeElement.querySelectorAll('progress').length).toBe(0);
    expect(fixture.nativeElement.querySelectorAll('.opcion').length).toBe(0);
    expect(fixture.nativeElement.querySelectorAll('input:not(details input)').length).toBe(1);
    expect(fixture.nativeElement.querySelectorAll('details,table,fieldset,select').length).toBe(0);
    expect(fixture.nativeElement.querySelectorAll('a').length).toBe(0);
    expect([...fixture.nativeElement.querySelectorAll('.opciones-simples li')].map((e: any) => e.textContent.trim()))
      .toEqual(['Guardar', 'Ahorrar', 'Invertir']);
    expect(fixture.nativeElement.querySelectorAll('.probabilidades strong').length).toBe(2);
    fixture.nativeElement.querySelectorAll('details').forEach((d: HTMLDetailsElement) => expect(d.open).toBeFalse());
    fixture.nativeElement.querySelectorAll('table').forEach((t: HTMLTableElement) => expect(t.closest('details')).not.toBeNull());
    expect(component.valido).toBeFalse(); // Nunca rellena ganancias automáticamente.
  });

  it('el botón evalúa solo el monto y muestra el resultado simulado', () => {
    iniciar(); component.montoEvaluar = 500; fixture.detectChanges();
    fixture.nativeElement.querySelector('button.primario').click();
    const req = http.expectOne(`${url}/vme/automatico`);
    expect(req.request.body).toEqual({divisa: 'BOB', monto: 500});
    req.flush({divisa:'BOB', disponible:500, reservaMinima:0, markov,
      alternativas:[{nombre:'Guardar mi dinero',vme:500,viable:true},
        {nombre:'Ahorrar',vme:505,viable:true},{nombre:'Invertir',vme:490,viable:true}], recomendadas:['Ahorrar'],
      explicacion:'SIMULACIÓN: resultados ficticios.'});
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.resultado').textContent).toContain('Ahorrar');
    expect(fixture.nativeElement.querySelector('.resultado').textContent).toContain('505.00');
    expect(component.disponible).toBe(1000);
    const comparacion = fixture.nativeElement.querySelector('.comparacion-vme');
    expect(comparacion.textContent).toContain('Guardar: Bs 500.00');
    expect(comparacion.textContent).toContain('Ahorrar: Bs 505.00');
    expect(comparacion.textContent).toContain('Invertir: Bs 490.00');
    expect(comparacion.textContent).toContain('SIMULACIÓN');
    expect(comparacion.querySelectorAll('table,input').length).toBe(0);
    component.invalidar(); expect(component.vme).toBeNull();
    component.markov = {...markov, probabilidades:null}; component.evaluarMonto();
    http.expectNone(`${url}/vme/automatico`);
  });

  it('no permite evaluar sin probabilidades y valida dinero', () => {
    iniciar(); parametros();
    expect(component.valido).toBeTrue();
    component.montoEvaluar = -1; expect(component.valido).toBeFalse();
    component.montoEvaluar = .001; expect(component.valido).toBeFalse();
    component.montoEvaluar = 1001; expect(component.valido).toBeFalse();
    parametros(); component.markov = { ...markov, probabilidades: null };
    component.evaluar(); http.expectNone(`${url}/vme`);
  });

  it('envía escenarios sin probabilidades y descarta una respuesta tras editar parámetros', () => {
    iniciar(); parametros(); component.evaluar();
    const req = http.expectOne(`${url}/vme`);
    expect(req.request.method).toBe('POST'); expect(req.request.body.probabilidades).toBeUndefined();
    expect(req.request.body.divisa).toBe('BOB');
    expect(req.request.body.reservaMinima).toBe(600);
    expect(req.request.body.alternativas.map((a: {asignacion: number}) => a.asignacion)).toEqual([0, 400, 400]);
    component.invalidar();
    req.flush({ disponible: 1000, markov, alternativas: [], recomendadas: ['Antigua'] } as unknown as Vme);
    expect(component.vme).toBeNull();
  });

  it('cambiar divisa limpia resultados e ignora respuestas antiguas', () => {
    fixture.detectChanges(); http.expectOne(`${url}/divisas`).flush(['BOB', 'USD']);
    const anterior = http.expectOne(`${url}/markov?divisa=BOB`);
    const saldoAnterior = http.expectOne(`${url}/disponible?divisa=BOB`);
    component.divisa = 'USD'; component.cargar();
    http.expectOne(`${url}/markov?divisa=USD`).flush({ ...markov, divisa: 'USD' });
    http.expectOne(`${url}/disponible?divisa=USD`).flush(20);
    anterior.flush(markov); saldoAnterior.flush(1000);
    expect(component.markov?.divisa).toBe('USD'); expect(component.disponible).toBe(20);
  });

  it('muestra una explicación si el usuario no tiene billeteras', () => {
    fixture.detectChanges(); http.expectOne(`${url}/divisas`).flush([]); fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Necesitamos saldo e historial suficiente para comparar.');
    expect(component.cargando).toBeFalse();
  });

  it('muestra el error de conexión sin dejar el estado cargando', () => {
    fixture.detectChanges(); http.expectOne(`${url}/divisas`).error(new ProgressEvent('error'));
    fixture.detectChanges(); expect(component.error).toContain('No se pudo conectar'); expect(component.cargando).toBeFalse();
  });

  it('no dibuja probabilidades ni permite recomendar cuando falta historial', () => {
    iniciar(); parametros(); component.markov = { ...markov, probabilidades: null, historial: [] };
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('progress')).toBeNull();
    expect(fixture.nativeElement.querySelector('.probabilidades')).toBeNull();
    expect(fixture.nativeElement.querySelector('.sin-historial').textContent.trim()).toBe('Necesitamos más historial para conocer cómo podrían estar tus finanzas');
    component.evaluar(); http.expectNone(`${url}/vme`);
    expect(component.vme).toBeNull();
  });

  it('deriva una reserva en centavos y elimina supuestos al cambiar moneda', () => {
    iniciar(); parametros(); component.montoEvaluar = 333.33;
    expect(component.reservaMinima).toBe(666.67);
    component.divisa = 'USD'; component.cargar();
    http.expectOne(`${url}/markov?divisa=USD`).flush({ ...markov, divisa: 'USD' });
    http.expectOne(`${url}/disponible?divisa=USD`).flush(20);
    expect(component.montoEvaluar).toBeNull();
    expect(component.alternativas.every(a => a.resultadoEstable === null && a.resultadoDeficit === null)).toBeTrue();
  });

  it('muestra los VME, la ganadora y las operaciones reales recibidas del servidor', () => {
    iniciar(); parametros(); component.evaluar();
    const req = http.expectOne(`${url}/vme`);
    req.flush({ divisa: 'BOB', disponible: 1000, reservaMinima: 600, markov,
      alternativas: component.alternativas.map((a, i) => ({ ...a, asignacion: i ? 400 : 0, liquidezRestante: i ? 600 : 1000,
        patrimonioEstable: 1000 + a.resultadoEstable!, patrimonioDeficit: 1000 + a.resultadoDeficit!,
        viable: true, motivo: 'Cumple', vme: 1000 + i * 5 })),
      recomendadas: ['Destinar a inversión'], explicacion: 'Calculado en servidor' } as Vme);
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelectorAll('.recomendacion').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.recomendacion').textContent).toContain('Invertir');
    expect(fixture.nativeElement.querySelectorAll('.operacion').length).toBe(0);
    expect(fixture.nativeElement.querySelector('.recomendacion').textContent).toContain('1,010.00');
    expect(fixture.nativeElement.querySelectorAll('.mejor-opcion').length).toBe(1);
    expect(fixture.nativeElement.querySelector('.mejor-opcion').textContent).toBe('Invertir');
    expect(fixture.nativeElement.textContent).toContain('no es una ganancia garantizada');
  });

  it('explica el estado de gastos mayores y un empate sin imponer una opción', () => {
    iniciar();
    component.markov = { ...markov, estadoActual: { mes: '2026-07', ingresos: 100, gastos: 120, estado: 'DEFICIT' },
      probabilidades: { estable: .2, deficit: .8 } };
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Estás gastando más de lo que ganas');
    const porcentajes = fixture.nativeElement.querySelectorAll('.probabilidades strong');
    expect(porcentajes[0].textContent.trim()).toBe('20.0 %');
    expect(porcentajes[1].textContent.trim()).toBe('80.0 %');
    expect(component.consejo).toContain('Revisa tus gastos');
    component.vme = { recomendadas: ['A', 'B'], markov } as Vme;
    expect(component.explicacionRecomendacion).toContain('empate');
  });

  it('carga supuestos de ejemplo identificados y conserva las probabilidades del historial', () => {
    iniciar(); component.cargarEjemplo();
    const req = http.expectOne(`${url}/vme`);
    expect(component.escenariosDeEjemplo).toBeTrue();
    expect(req.request.body.reservaMinima).toBe(500);
    expect(req.request.body.alternativas.map((a: {resultadoEstable: number}) => a.resultadoEstable)).toEqual([0, 20, 100]);
    expect(req.request.body.probabilidades).toBeUndefined();
    component.invalidar(); req.flush({ disponible: 1000, markov } as Vme);
    fixture.detectChanges();
    expect(component.escenariosDeEjemplo).toBeTrue();
    expect(fixture.nativeElement.querySelectorAll('fieldset').length).toBe(0);
  });

  it('el ejemplo no inventa probabilidades si falta historial ni excede el saldo', () => {
    iniciar(); component.markov = { ...markov, probabilidades: null }; component.disponible = 100;
    component.cargarEjemplo();
    expect(component.montoEvaluar).toBe(100);
    expect(component.markov.probabilidades).toBeNull(); http.expectNone(`${url}/vme`);
  });

  it('el monto habilita la simulación sin exigir seis resultados manuales', () => {
    iniciar(); component.montoEvaluar = 500; fixture.detectChanges();
    const boton = fixture.nativeElement.querySelector('.evaluar button') as HTMLButtonElement;
    expect(boton.textContent?.trim()).toBe('Evaluar'); expect(boton.disabled).toBeFalse();
    expect(fixture.nativeElement.querySelector('#estado-evaluacion').textContent).toContain('Indica un monto y pulsa Evaluar.');
    expect(component.alternativas.every(a => a.resultadoEstable === null && a.resultadoDeficit === null)).toBeTrue();
    expect(fixture.nativeElement.querySelectorAll('.campos-escenario input').length).toBe(0);
    fixture.nativeElement.querySelectorAll('.campos-escenario input').forEach((i: HTMLInputElement) => expect(i.closest('details')?.open).toBeFalse());
    fixture.nativeElement.querySelectorAll('table,.formula,.operacion').forEach((e: HTMLElement) => expect(e.closest('details')?.classList.contains('detalle')).toBeTrue());
    component.evaluar(); http.expectNone(`${url}/vme`);
  });

  it('presenta el historial sin etiquetas de demostración', () => {
    const token = 'header.' + btoa(JSON.stringify({ sub: 'demo@finanzas.test' })) + '.signature';
    spyOn(TestBed.inject(AuthService), 'obtenerToken').and.returnValue(token);
    iniciar();
    expect(fixture.nativeElement.querySelector('.nota-demo')).toBeNull();
  });
});
