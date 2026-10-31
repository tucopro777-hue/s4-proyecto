import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { DashboardResumen, DashboardService, mensajeErrorDashboard } from './dashboard.service';

/** Panel principal: muestra los indicadores del mes en tarjetas KPI. */
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NavegacionComponent, DecimalPipe],
  templateUrl: './dashboard.component.html',
  styleUrl: './dashboard.component.scss',
})
export class DashboardComponent implements OnInit {
  private readonly dashboardService = inject(DashboardService);

  resumen: DashboardResumen | null = null;
  cargando = true;
  error: string | null = null;

  ngOnInit(): void {
    this.cargar();
  }

  cargar(): void {
    this.cargando = true;
    this.error = null;

    this.dashboardService.obtenerResumen().subscribe({
      next: (datos) => {
        this.resumen = datos;
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.error = mensajeErrorDashboard(err);
        this.cargando = false;
      },
    });
  }

  /** Color del balance: verde si el mes termina con saldo positivo, coral si no. */
  get claseBalance(): string {
    return (this.resumen?.balanceMes ?? 0) >= 0 ? 'positivo' : 'negativo';
  }
}
