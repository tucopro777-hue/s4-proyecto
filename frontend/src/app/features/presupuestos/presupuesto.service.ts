import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

/** OK, ADVERTENCIA (80 % o más) o EXCEDIDO (por encima del límite). */
export type NivelPresupuesto = 'OK' | 'ADVERTENCIA' | 'EXCEDIDO';

/** Presupuesto mensual de una categoría de gasto (PresupuestoResponse). */
export interface Presupuesto {
  id: number;
  categoriaId: number;
  categoriaNombre: string;
  montoLimite: number;
  saldoConsumido: number;
  anio: number;
  mes: number;
  nivel: NivelPresupuesto;
}

/** Datos para crear un presupuesto (PresupuestoRequest). */
export interface PresupuestoRequest {
  categoriaId: number;
  montoLimite: number;
  anio: number;
  mes: number;
}

@Injectable({ providedIn: 'root' })
export class PresupuestoService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/presupuestos';

  listar(anio: number, mes: number): Observable<Presupuesto[]> {
    const params = new HttpParams().set('anio', anio).set('mes', mes);
    return this.http.get<Presupuesto[]>(this.apiUrl, { params });
  }

  crear(datos: PresupuestoRequest): Observable<Presupuesto> {
    return this.http.post<Presupuesto>(this.apiUrl, datos);
  }

  /** Cambia solo el límite; el consumo acumulado se conserva. */
  actualizar(id: number, montoLimite: number): Observable<Presupuesto> {
    return this.http.put<Presupuesto>(`${this.apiUrl}/${id}`, { montoLimite });
  }

  eliminar(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorPresupuesto(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos: la categoría debe ser de gasto y estar activa.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 404:
      return 'El presupuesto ya no existe.';
    case 409:
      return 'Ya tienes un presupuesto para esa categoría en ese mes.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
