import { Component } from '@angular/core';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';

// Placeholder: se sustituirá por el panel real de gastos y presupuestos
@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NavegacionComponent],
  template: `
    <app-navegacion />
    <main class="auth-page">
      <section class="auth-card">
        <header class="auth-header">
          <span class="auth-marca">Finanzas</span>
          <h1>Bienvenido</h1>
          <p>El panel de gastos y presupuestos está en construcción.</p>
        </header>
      </section>
    </main>
  `,
  styles: [
    `
      .auth-page {
        min-height: calc(100vh - 64px);
      }
    `,
  ],
})
export class DashboardComponent {}
