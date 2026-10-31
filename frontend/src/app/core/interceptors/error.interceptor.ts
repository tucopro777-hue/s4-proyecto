import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';

/**
 * Interceptor global de errores HTTP (Persona 3 · robustez del frontend).
 *
 * Traduce cualquier HttpErrorResponse a un mensaje claro y amigable ANTES de
 * que llegue al componente, cubriendo los casos que el servidor no puede
 * describir (sin conexión, 500, etc.). El error traducido viaja en
 * err.error.mensaje como string, así todos los componentes muestran un solo
 * formato sin repetir lógica de switch.
 */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  return next(req).pipe(
    catchError((error: HttpErrorResponse) => {
      // Traduce y adjunta el mensaje; luego re-lanza para que el componente decida qué mostrar
      const mensaje = traducirErrorHttp(error);
      const enriquecido = new HttpErrorResponse({
        error: mensaje,
        headers: error.headers,
        status: error.status,
        statusText: error.statusText,
        url: error.url ?? undefined,
      });
      return throwError(() => enriquecido);
    }),
  );
};

/** Traduce un error HTTP a un mensaje para el usuario. */
function traducirErrorHttp(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos enviados: son inválidos o están incompletos.';
    case 401:
      return 'Tu sesión ha expirado. Vuelve a iniciar sesión.';
    case 403:
      return 'No tienes permisos para realizar esta operación.';
    case 404:
      return 'El recurso solicitado no existe.';
    case 409:
      return 'La operación entra en conflicto con el estado actual.';
    case 422:
      return 'La operación no puede completarse con los datos actuales.';
    case 500:
      return 'Error interno del servidor. Inténtalo de nuevo más tarde.';
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}
