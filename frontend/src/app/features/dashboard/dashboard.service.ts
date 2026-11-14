import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable } from 'rxjs';

/** Indicadores del panel tal como los devuelve el backend (DashboardResponse). */
export interface DashboardResumen {
  saldoTotal: number;
  ingresosMes: number;
  gastosMes: number;
  balanceMes: number;
  presupuestosExcedidos: number;
}

/** Gasto del mes en una categoría (GastoCategoriaResponse). */
export interface GastoCategoria {
  categoriaId: number;
  categoriaNombre: string;
  color: string;
  total: number;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/dashboard';

  obtenerResumen(): Observable<DashboardResumen> {
    return this.http.get<DashboardResumen>(`${this.apiUrl}/resumen`);
  }

  /** Gasto del mes en curso agrupado por categoría, de mayor a menor. */
  gastosPorCategoria(): Observable<GastoCategoria[]> {
    return this.http.get<GastoCategoria[]>(`${this.apiUrl}/gastos-por-categoria`);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorDashboard(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    default:
      return 'No se pudo cargar el resumen. Inténtalo de nuevo.';
  }
}
