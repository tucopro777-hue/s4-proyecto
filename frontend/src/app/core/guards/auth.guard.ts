import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from '../auth/auth.service';

/** Permite el acceso solo si hay un token guardado; si no, redirige a /login. */
export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  const router = inject(Router);

  return auth.estaAutenticado() ? true : router.createUrlTree(['/login']);
};
