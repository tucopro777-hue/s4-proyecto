import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoTransaccion = 'INGRESO' | 'GASTO';

/** Movimiento tal como lo devuelve el backend (TransaccionResponse). */
export interface Transaccion {
  id: number;
  monto: number;
  tipo: TipoTransaccion;
  descripcion: string | null;
  /** Fecha ISO yyyy-MM-dd. */
  fecha: string;
  billeteraId: number;
  billeteraNombre: string;
  categoriaId: number;
  categoriaNombre: string;
}

/** Datos para registrar un movimiento (TransaccionRequest). */
export interface TransaccionRequest {
  monto: number;
  tipo: TipoTransaccion;
  descripcion: string | null;
  fecha: string;
  billeteraId: number;
  categoriaId: number;
}

@Injectable({ providedIn: 'root' })
export class TransaccionService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/transacciones';

  listar(): Observable<Transaccion[]> {
    return this.http.get<Transaccion[]>(this.apiUrl);
  }

  registrar(datos: TransaccionRequest): Observable<Transaccion> {
    return this.http.post<Transaccion>(this.apiUrl, datos);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorTransaccion(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos: la billetera o la categoría no son válidas para este movimiento.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 422:
      return 'La billetera no tiene saldo suficiente para este gasto.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
