import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AuthService } from '../../../core/auth/auth.service';

/** Barra de navegación principal, compartida por las pantallas autenticadas. */
@Component({
  selector: 'app-navegacion',
  standalone: true,
  imports: [RouterLink, RouterLinkActive],
  templateUrl: './navegacion.component.html',
  styleUrl: './navegacion.component.scss',
})
export class NavegacionComponent {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  get cuentaDemo(): boolean {
    try {
      const payload = this.auth.obtenerToken()?.split('.')[1];
      return !!payload && JSON.parse(atob(payload.replace(/-/g,'+').replace(/_/g,'/'))).sub === 'demo@finanzas.test';
    } catch { return false; }
  }

  cerrarSesion(): void {
    this.auth.logout();
    this.router.navigateByUrl('/login');
  }
}
