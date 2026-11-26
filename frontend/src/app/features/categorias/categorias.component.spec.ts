import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { CategoriasComponent } from './categorias.component';
import { mensajeErrorCategoria } from './categoria.service';
import { CategoriaService } from './categoria.service';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { errorInterceptor } from '../../core/interceptors/error.interceptor';

describe('Creación de categorías sin duplicados', () => {
  it('muestra el nombre conflictivo que devuelve el servidor', () => {
    const mensaje = 'Ya tienes la categoría "Comida". El nombre es igual o solo cambia por una letra añadida o faltante.';
    expect(mensajeErrorCategoria(new HttpErrorResponse({ status: 409, error: { message: mensaje } }))).toBe(mensaje);
  });

  it('ofrece un mensaje alternativo si el conflicto no tiene detalle', () => {
    expect(mensajeErrorCategoria(new HttpErrorResponse({ status: 409 }))).toContain('muy parecido');
  });

  it('conserva el mensaje del backend al atravesar el interceptor real', () => {
    TestBed.configureTestingModule({ providers: [provideHttpClient(withInterceptors([errorInterceptor])), provideHttpClientTesting()] });
    const http = TestBed.inject(HttpTestingController);
    const mensaje = 'Ya tienes la categoría "Comida".';
    let recibido = '';
    TestBed.inject(CategoriaService).crear({ nombre: 'Comidaa', tipo: 'GASTO', color: '#FF8800', icono: 'food' })
      .subscribe({ error: e => recibido = mensajeErrorCategoria(e) });
    http.expectOne('http://localhost:8080/api/categorias').flush({ message: mensaje }, { status: 409, statusText: 'Conflict' });
    expect(recibido).toBe(mensaje);
    http.verify();
  });

  it('no envía dos creaciones mientras la primera está pendiente', () => {
    TestBed.configureTestingModule({ imports: [CategoriasComponent, HttpClientTestingModule], providers: [provideRouter([])] });
    const fixture = TestBed.createComponent(CategoriasComponent);
    const http = TestBed.inject(HttpTestingController);
    fixture.detectChanges(); http.expectOne('http://localhost:8080/api/categorias').flush([]);
    const c = fixture.componentInstance;
    c.form.patchValue({ nombre: 'Comida' });
    c.registrar(); c.registrar();
    const req = http.expectOne('http://localhost:8080/api/categorias');
    expect(req.request.method).toBe('POST');
    req.flush({ message: 'Ya tienes la categoría "Comida".' }, { status: 409, statusText: 'Conflict' });
    expect(c.error).toContain('"Comida"'); expect(c.guardando).toBeFalse();
    http.verify();
  });
});
