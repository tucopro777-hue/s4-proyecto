import type { AuthSession, LoginFormValues, User } from '@/types';

const STORAGE_KEY = 'finsight_session';
const SESSION_DURATION_MS = 1000 * 60 * 60 * 8; // 8 horas

/**
 * Genera un JWT simulado (no criptográficamente válido).
 * Sirve únicamente para desarrollo del frontend mientras se conecta
 * el backend real en Spring Boot, que emitirá tokens firmados.
 */
function generarTokenSimulado(correo: string): string {
  const header = btoa(JSON.stringify({ alg: 'HS256', typ: 'JWT' }));
  const payload = btoa(
    JSON.stringify({
      sub: correo,
      iat: Date.now(),
      exp: Date.now() + SESSION_DURATION_MS,
    })
  );
  const signature = btoa(`simulated-${Date.now()}`);
  return `${header}.${payload}.${signature}`;
}

function extraerNombreDeCorreo(correo: string): string {
  const parteLocal = correo.split('@')[0] ?? 'Usuario';
  return parteLocal
    .split(/[._-]/)
    .map((palabra) => palabra.charAt(0).toUpperCase() + palabra.slice(1))
    .join(' ');
}

export const authService = {
  /**
   * Simula una llamada de login al backend. Reemplazar por una llamada
   * real a POST /api/auth/login contra Spring Boot cuando esté disponible.
   */
  async login({ correo, contrasena }: LoginFormValues): Promise<AuthSession> {
    // Simula latencia de red
    await new Promise((resolve) => setTimeout(resolve, 700));

    if (!correo || !contrasena) {
      throw new Error('Correo y contraseña son obligatorios.');
    }

    const user: User = {
      id: crypto.randomUUID(),
      nombre: extraerNombreDeCorreo(correo),
      correo,
    };

    const session: AuthSession = {
      token: generarTokenSimulado(correo),
      user,
      expiresAt: Date.now() + SESSION_DURATION_MS,
    };

    this.saveSession(session);
    return session;
  },

  logout(): void {
    localStorage.removeItem(STORAGE_KEY);
  },

  saveSession(session: AuthSession): void {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(session));
  },

  getSession(): AuthSession | null {
    const raw = localStorage.getItem(STORAGE_KEY);
    if (!raw) return null;

    try {
      const session = JSON.parse(raw) as AuthSession;
      if (session.expiresAt < Date.now()) {
        this.logout();
        return null;
      }
      return session;
    } catch {
      this.logout();
      return null;
    }
  },

  isAuthenticated(): boolean {
    return this.getSession() !== null;
  },
};
