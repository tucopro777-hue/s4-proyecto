import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoCategoria = 'INGRESO' | 'GASTO';

/** Categoría tal como la devuelve el backend (CategoriaResponse). */
export interface Categoria {
  id: number;
  nombre: string;
  tipo: TipoCategoria;
  color: string;
  icono: string;
}

/** Datos para registrar una categoría (CategoriaRequest). */
export interface CategoriaRequest {
  nombre: string;
  tipo: TipoCategoria;
  color: string;
  icono: string;
}

export interface OpcionIcono {
  clave: string;
  simbolo: string;
  etiqueta: string;
}

/** Catálogo de iconos disponibles. La clave es lo que se guarda en el backend. */
export const ICONOS_CATEGORIA: OpcionIcono[] = [
  { clave: 'cart', simbolo: '🛒', etiqueta: 'Compras' },
  { clave: 'food', simbolo: '🍔', etiqueta: 'Comida' },
  { clave: 'home', simbolo: '🏠', etiqueta: 'Hogar' },
  { clave: 'bills', simbolo: '💡', etiqueta: 'Servicios' },
  { clave: 'transport', simbolo: '🚌', etiqueta: 'Transporte' },
  { clave: 'health', simbolo: '💊', etiqueta: 'Salud' },
  { clave: 'education', simbolo: '📚', etiqueta: 'Educación' },
  { clave: 'leisure', simbolo: '🎬', etiqueta: 'Ocio' },
  { clave: 'gift', simbolo: '🎁', etiqueta: 'Regalos' },
  { clave: 'salary', simbolo: '💼', etiqueta: 'Salario' },
  { clave: 'savings', simbolo: '💰', etiqueta: 'Ahorro' },
  { clave: 'other', simbolo: '🏷️', etiqueta: 'Otros' },
];

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/categorias';

  listar(tipo?: TipoCategoria): Observable<Categoria[]> {
    let params = new HttpParams();
    if (tipo) {
      params = params.set('tipo', tipo);
    }
    return this.http.get<Categoria[]>(this.apiUrl, { params });
  }

  crear(datos: CategoriaRequest): Observable<Categoria> {
    return this.http.post<Categoria>(this.apiUrl, datos);
  }

  /** Desactivación lógica en el backend; la categoría deja de listarse. */
  desactivar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorCategoria(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos introducidos.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 404:
      return 'La categoría ya no existe.';
    case 409:
      return 'Ya tienes una categoría con ese nombre.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
