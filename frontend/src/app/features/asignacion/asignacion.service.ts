import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Meta recomendada por la asignación inteligente (MetaFinanciera). */
export interface MetaRecomendada {
  id: number;
  nombre: string;
  costo: number;
  prioridad: number;
}

/** Resultado de la asignación inteligente (AsignacionResponse). */
export interface AsignacionInteligente {
  recomendadas: MetaRecomendada[];
  totalAsignado: number;
  puntosPrioridad: number;
  sobrante: number;
}

@Injectable({ providedIn: 'root' })
export class AsignacionService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/asignacion';

  /** Pide al backend la combinación de metas que mejor aprovecha el dinero disponible. */
  recomendar(presupuestoDisponible: number): Observable<AsignacionInteligente> {
    return this.http.post<AsignacionInteligente>(`${this.apiUrl}/inteligente`, { presupuestoDisponible });
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorAsignacion(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 400:
    case 422:
      return error.error?.message ?? 'No se pudo calcular la asignación.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
