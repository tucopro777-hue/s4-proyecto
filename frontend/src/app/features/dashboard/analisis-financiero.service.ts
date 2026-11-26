import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type Estado = 'ESTABLE' | 'DEFICIT';
export interface Mes { mes: string; ingresos: number; gastos: number; estado: Estado; }
export interface Markov {
  divisa: string; mesActual: string; estadoActual: Mes | null;
  mesBase: string; mesPredicho: string; estadoBase: Estado | null; historial: Mes[];
  matriz: { origen: Estado; aEstable: number; aDeficit: number; total: number; pEstable: number | null; pDeficit: number | null }[];
  probabilidades: { estable: number; deficit: number } | null; explicacion: string;
}
export interface Alternativa { nombre: string; asignacion: number | null; resultadoEstable: number | null; resultadoDeficit: number | null; }
export interface PedidoVme { divisa: string; reservaMinima: number; alternativas: Alternativa[]; }
export interface Vme {
  divisa: string; disponible: number; reservaMinima: number; markov: Markov;
  alternativas: { nombre: string; asignacion: number; liquidezRestante: number;
    resultadoEstable: number; resultadoDeficit: number; patrimonioEstable: number; patrimonioDeficit: number;
    viable: boolean; motivo: string; vme: number | null }[];
  recomendadas: string[]; explicacion: string;
}

@Injectable({ providedIn: 'root' })
export class AnalisisFinancieroService {
  private http = inject(HttpClient);
  private url = 'http://localhost:8080/api/dashboard/analisis';
  divisas() { return this.http.get<string[]>(`${this.url}/divisas`); }
  disponible(divisa: string) { return this.http.get<number>(`${this.url}/disponible`, { params: { divisa } }); }
  markov(divisa: string) { return this.http.get<Markov>(`${this.url}/markov`, { params: { divisa } }); }
  evaluar(pedido: PedidoVme) { return this.http.post<Vme>(`${this.url}/vme`, pedido); }
  evaluarAutomaticamente(divisa: string, monto: number) {
    return this.http.post<Vme>(`${this.url}/vme/automatico`, { divisa, monto });
  }
}
