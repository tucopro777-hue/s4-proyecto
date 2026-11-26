import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Billetera tal como la devuelve el backend (BilleteraResponse). */
export interface Billetera {
  id: number;
  nombre: string;
  saldoInicial: number;
  saldoActual: number;
  divisa: string;
  activa: boolean;
}

/** Datos para registrar una billetera (BilleteraRequest). */
export interface BilleteraRequest {
  nombre: string;
  saldoInicial: number;
  divisa: string;
}

@Injectable({ providedIn: 'root' })
export class BilleteraService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/billeteras';

  listar(): Observable<Billetera[]> {
    return this.http.get<Billetera[]>(this.apiUrl);
  }

  crear(datos: BilleteraRequest): Observable<Billetera> {
    return this.http.post<Billetera>(this.apiUrl, datos);
  }

  /** Desactivación lógica en el backend. Exige la contraseña del usuario para confirmar. */
  desactivar(id: number, password: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { body: { password } });
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorBilletera(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 403:
      return 'Contraseña incorrecta.';
    case 404:
      return 'La billetera ya no existe.';
    case 409:
      return error.error?.message ?? 'Ya tienes una billetera activa con ese nombre.';
    case 400:
      return error.error?.message ?? 'Revisa los datos introducidos.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
