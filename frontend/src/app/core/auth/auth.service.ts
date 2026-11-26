import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';
import { Observable, tap } from 'rxjs';

export interface RegistroRequest {
  nombre: string;
  email: string;
  password: string;
}

export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthResponse {
  token: string;
  tipo: string;
  email: string;
  nombre: string;
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);

  private readonly apiUrl = 'http://localhost:8080/api/auth';
  private readonly claveToken = 'finanzas_token';

  registro(datos: RegistroRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/registro`, datos)
      .pipe(tap((respuesta) => this.guardarToken(respuesta.token)));
  }

  login(datos: LoginRequest): Observable<AuthResponse> {
    return this.http
      .post<AuthResponse>(`${this.apiUrl}/login`, datos)
      .pipe(tap((respuesta) => this.guardarToken(respuesta.token)));
  }

  logout(): void {
    localStorage.removeItem(this.claveToken);
  }

  obtenerToken(): string | null {
    return localStorage.getItem(this.claveToken);
  }

  estaAutenticado(): boolean {
    return this.obtenerToken() !== null;
  }

  private guardarToken(token: string): void {
    localStorage.setItem(this.claveToken, token);
  }
}

/** Traduce un error HTTP del backend a un mensaje para el usuario. */
export function mensajeErrorAuth(error: HttpErrorResponse): string {
  switch (error.status) {
    case 0:
      return 'No se pudo conectar con el servidor. Verifica que el backend esté activo.';
    case 400:
      return 'Revisa los datos introducidos.';
    case 401:
      return 'Email o contraseña incorrectos.';
    case 409:
      return 'Ese email ya está registrado.';
    case 423:
      return mensajeCuentaBloqueada(error);
    default:
      return 'Ocurrió un error inesperado. Inténtalo de nuevo.';
  }
}

/** La cuenta bloqueada llega con Retry-After (segundos) que indica cuánto falta para reintentar. */
function mensajeCuentaBloqueada(error: HttpErrorResponse): string {
  const segundos = Number(error.headers?.get('Retry-After'));
  if (!Number.isFinite(segundos) || segundos <= 0) {
    return 'Demasiados intentos fallidos. Inténtalo de nuevo más tarde.';
  }
  const minutos = Math.max(1, Math.ceil(segundos / 60));
  return `Demasiados intentos fallidos. Tu cuenta está bloqueada temporalmente; inténtalo de nuevo en ${minutos} ${minutos === 1 ? 'minuto' : 'minutos'}.`;
}
