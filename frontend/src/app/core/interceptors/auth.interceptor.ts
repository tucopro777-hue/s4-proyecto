import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../auth/auth.service';

const PREFIJO_API = 'http://localhost:8080/api/';

/** Adjunta el JWT guardado a las peticiones dirigidas al backend. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const token = inject(AuthService).obtenerToken();

  if (!token || !req.url.startsWith(PREFIJO_API)) {
    return next(req);
  }

  return next(req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }));
};
