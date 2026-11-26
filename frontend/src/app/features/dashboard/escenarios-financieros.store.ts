import { Injectable } from '@angular/core';
import { Alternativa } from './analisis-financiero.service';

export interface EscenariosConfigurados {
  monto: number;
  alternativas: Alternativa[];
  ejemplo: boolean;
}

/** Parámetros temporales por sesión y divisa; nunca guarda probabilidades ni recomendaciones. */
@Injectable({ providedIn: 'root' })
export class EscenariosFinancierosStore {
  private token: string | null | undefined;
  private datos = new Map<string, EscenariosConfigurados>();
  private ultimaDivisa: string | undefined;
  private sesion(token: string | null) {
    if (this.token !== token) { this.datos.clear(); this.ultimaDivisa = undefined; this.token = token; }
  }
  preferida(token: string | null) { this.sesion(token); return this.ultimaDivisa; }
  leer(token: string | null, divisa: string): EscenariosConfigurados | undefined {
    this.sesion(token);
    const datos = this.datos.get(divisa);
    return datos ? structuredClone(datos) : undefined;
  }
  guardar(token: string | null, divisa: string, datos: EscenariosConfigurados) {
    this.sesion(token); this.ultimaDivisa = divisa; this.datos.set(divisa, structuredClone(datos));
  }
}
