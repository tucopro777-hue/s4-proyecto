import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

// Placeholder: se sustituirá por el panel real de gastos y presupuestos
@Component({
  selector: 'app-dashboard',
  standalone: true,
  template: `
    <main class="auth-page">
      <section class="auth-card">
        <header class="auth-header">
          <span class="auth-marca">Finanzas</span>
          <h1>Bienvenido</h1>
          <p>El panel de gastos y presupuestos está en construcción.</p>
        </header>
        <button type="button" class="btn-primary" (click)="cerrarSesion()">Cerrar sesión</button>
      </section>
    </main>
  `,
})
export class DashboardComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
