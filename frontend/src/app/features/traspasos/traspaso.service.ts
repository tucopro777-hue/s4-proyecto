import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Traspaso tal como lo devuelve el backend (TraspasoResponse). */
export interface Traspaso {
  id: number;
  monto: number;
  descripcion: string | null;
  /** Fecha ISO yyyy-MM-dd. */
  fecha: string;
  billeteraOrigenId: number;
  billeteraOrigenNombre: string;
  billeteraDestinoId: number;
  billeteraDestinoNombre: string;
}

/** Datos para registrar un traspaso (TraspasoRequest). */
export interface TraspasoRequest {
  billeteraOrigenId: number;
  billeteraDestinoId: number;
  monto: number;
  descripcion: string | null;
  fecha: string;
}

@Injectable({ providedIn: 'root' })
export class TraspasoService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/traspasos';

  listar(): Observable<Traspaso[]> {
    return this.http.get<Traspaso[]>(this.apiUrl);
  }

  /** Transferencia atómica en el backend: o se aplica completa, o no se aplica nada. */
  transferir(datos: TraspasoRequest): Observable<Traspaso> {
    return this.http.post<Traspaso>(this.apiUrl, datos);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorTraspaso(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos: las billeteras deben ser distintas, activas y de la misma divisa.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 422:
      return 'La billetera de origen no tiene saldo suficiente para este traspaso.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
