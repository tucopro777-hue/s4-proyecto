import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Meta de ahorro tal como la devuelve el backend (MetaAhorroResponse). */
export interface MetaAhorro {
  id: number;
  nombre: string;
  montoObjetivo: number;
  montoActual: number;
  /** Fecha ISO yyyy-MM-dd, o null si no tiene fecha límite. */
  fechaLimite: string | null;
  /** Progreso en porcentaje; puede pasar de 100. */
  porcentaje: number;
  completada: boolean;
}

/** Datos para crear o editar una meta (MetaAhorroRequest). */
export interface MetaAhorroRequest {
  nombre: string;
  montoObjetivo: number;
  fechaLimite: string | null;
}

@Injectable({ providedIn: 'root' })
export class MetaAhorroService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/metas';

  listar(): Observable<MetaAhorro[]> {
    return this.http.get<MetaAhorro[]>(this.apiUrl);
  }

  crear(datos: MetaAhorroRequest): Observable<MetaAhorro> {
    return this.http.post<MetaAhorro>(this.apiUrl, datos);
  }

  /** Cambia nombre, objetivo y fecha límite; el monto acumulado no se modifica. */
  actualizar(id: number, datos: MetaAhorroRequest): Observable<MetaAhorro> {
    return this.http.put<MetaAhorro>(`${this.apiUrl}/${id}`, datos);
  }

  /** Único camino que suma dinero a la meta: un aporte explícito. */
  aportar(id: number, monto: number): Observable<MetaAhorro> {
    return this.http.post<MetaAhorro>(`${this.apiUrl}/${id}/aportes`, { monto });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorMeta(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos introducidos.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 404:
      return 'La meta ya no existe.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
