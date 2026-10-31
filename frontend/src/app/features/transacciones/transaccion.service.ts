import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export type TipoTransaccion = 'INGRESO' | 'GASTO';

/** Aviso de presupuesto que devuelve el backend al registrar un gasto. */
export interface AlertaPresupuesto {
  /** ADVERTENCIA (80 % o más) o EXCEDIDO (por encima del límite). */
  nivel: 'ADVERTENCIA' | 'EXCEDIDO';
  mensaje: string;
  saldoConsumido: number;
  montoLimite: number;
}

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
  /** Solo viene al registrar un gasto que afecta a un presupuesto; si no, null. */
  alertaPresupuesto: AlertaPresupuesto | null;
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

/** Filtros opcionales para el listado de movimientos (Persona 3). */
export interface FiltrosTransaccion {
  /** Fecha inicial del rango, yyyy-MM-dd. */
  desde?: string | null;
  /** Fecha final del rango, yyyy-MM-dd. */
  hasta?: string | null;
  categoriaId?: number | null;
  tipo?: TipoTransaccion | null;
}

@Injectable({ providedIn: 'root' })
export class TransaccionService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/transacciones';

  listar(): Observable<Transaccion[]> {
    return this.http.get<Transaccion[]>(this.apiUrl);
  }

  /**
   * Lista movimientos con filtros avanzados. Los parámetros vacíos, nulos o
   * indefinidos se OMITEN del HttpParams para que el backend aplique el listado
   * completo o solo los filtros activos.
   */
  listarConFiltros(filtros: FiltrosTransaccion): Observable<Transaccion[]> {
    let params = new HttpParams();
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    if (filtros.categoriaId != null) {
      params = params.set('categoriaId', String(filtros.categoriaId));
    }
    if (filtros.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    return this.http.get<Transaccion[]>(this.apiUrl, { params });
  }

  registrar(datos: TransaccionRequest): Observable<Transaccion> {
    return this.http.post<Transaccion>(this.apiUrl, datos);
  }

  /** Anulación lógica: revierte el saldo de la billetera y el consumo del presupuesto. */
  anular(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
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
    case 404:
      return 'El movimiento ya no existe.';
    case 409:
      return 'Este movimiento ya estaba anulado.';
    case 422:
      return 'La billetera no tiene saldo suficiente para esta operación.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
