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

/** Proyección de Markov del próximo mes (ProyeccionFinancieraResponse). Probabilidades de 0 a 1. */
export interface ProyeccionFinanciera {
  datosSuficientes: boolean;
  mesesAnalizados: number;
  estadoActual: 'ESTABLE' | 'DEFICIT' | null;
  probabilidadMantener: number | null;
  probabilidadEstableSiguiente: number | null;
  probabilidadDeficitSiguiente: number | null;
  matriz: {
    deficitADeficit: number;
    deficitAEstable: number;
    estableADeficit: number;
    estableAEstable: number;
  } | null;
}

/** Valor monetario esperado de una decisión para el próximo mes (VmeResponse.Alternativa). */
export interface AlternativaVme {
  nombre: string;
  reduccionGastos: number;
  pagoEstable: number;
  pagoDeficit: number;
  vme: number;
}

/** Comparación de decisiones por valor monetario esperado (VmeResponse). */
export interface VmeResultado {
  datosSuficientes: boolean;
  estadoActual: 'ESTABLE' | 'DEFICIT' | null;
  alternativas: AlternativaVme[];
  mejorAlternativa: string | null;
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

  /** Probabilidades del próximo mes según la cadena de Markov sobre los últimos 6 meses. */
  proyeccion(): Observable<ProyeccionFinanciera> {
    return this.http.get<ProyeccionFinanciera>(`${this.apiUrl}/proyeccion`);
  }

  /** Valor monetario esperado de mantener el gasto o recortarlo, para el próximo mes. */
  vme(): Observable<VmeResultado> {
    return this.http.get<VmeResultado>(`${this.apiUrl}/vme`);
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
