import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe, NgClass, PercentPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { RouterLink } from '@angular/router';
import { AnalisisFinancieroComponent } from './analisis-financiero.component';
import { forkJoin } from 'rxjs';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { AlertasPresupuestoService } from '../../core/alertas/alertas-presupuesto.service';
import { etiquetaNivel, porcentajeConsumo } from '../presupuestos/presupuesto.service';
import {
  DashboardResumen,
  DashboardService,
  GastoCategoria,
  ProyeccionFinanciera,
  mensajeErrorDashboard,
} from './dashboard.service';

/** Panel principal: indicadores del mes, gráfica de gastos por categoría y estado de los presupuestos. */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NavegacionComponent, DecimalPipe, PercentPipe, NgClass, RouterLink, AnalisisFinancieroComponent],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);
  private readonly alertas = inject(AlertasPresupuestoService);

  resumen: DashboardResumen | null = null;
  gastos: GastoCategoria[] = [];
  /** Proyección de Markov; si falla, la tarjeta simplemente no se muestra. */
  proyeccion: ProyeccionFinanciera | null = null;
  cargando = true;
  error: string | null = null;

  /** Presupuestos del mes; los carga la campanita de la barra de navegación. */
  readonly presupuestos = this.alertas.presupuestos;
  readonly porcentaje = porcentajeConsumo;
  readonly etiqueta = etiquetaNivel;

  readonly mesActual = new Date().toLocaleDateString('es', { month: 'long', year: 'numeric' });

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;

    forkJoin({
      resumen: this.dashboardService.obtenerResumen(),
      gastos: this.dashboardService.gastosPorCategoria(),
    }).subscribe({
      next: ({ resumen, gastos }) => {
        this.resumen = resumen;
        this.gastos = gastos;
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = mensajeErrorDashboard(err);
        this.cargando = false;
      },
    });

    this.dashboardService.proyeccion().subscribe({
      next: (proyeccion) => (this.proyeccion = proyeccion),
      error: () => (this.proyeccion = null),
    });
  }

  /** Probabilidad de que el próximo mes conserve el estado actual, según la matriz de Markov. */
  get etiquetaProyeccion(): string {
    return this.proyeccion?.estadoActual === 'ESTABLE'
      ? 'de mantener tu estabilidad financiera'
      : 'de seguir en déficit';
  }

  /** Color del balance: verde si el mes termina con saldo positivo, coral si no. */
  get claseBalance(): string {
    return (this.resumen?.balanceMes ?? 0) >= 0 ? 'positivo' : 'negativo';
  }

  get totalGastos(): number {
    return this.gastos.reduce((suma, g) => suma + g.total, 0);
  }

  /** Largo de la barra: proporcional a la categoría con más gasto (la mayor ocupa el 100 %). */
  anchoBarra(gasto: GastoCategoria): number {
    const maximo = this.gastos[0]?.total ?? 0;
    return maximo > 0 ? (gasto.total / maximo) * 100 : 0;
  }

  /** Parte del gasto total del mes que representa la categoría. */
  participacion(gasto: GastoCategoria): number {
    return this.totalGastos > 0 ? (gasto.total / this.totalGastos) * 100 : 0;
  }

  /** Ancho de la barra de un presupuesto, limitado a 100 %. */
  anchoPresupuesto(consumo: number): number {
    return Math.min(100, consumo);
  }
}
