import { useCallback, useEffect, useState } from 'react';
import { authService } from '@/services/authService';
import type { AuthSession, LoginFormValues } from '@/types';

interface UseAuthReturn {
  session: AuthSession | null;
  isLoading: boolean;
  error: string | null;
  login: (values: LoginFormValues) => Promise<boolean>;
  logout: () => void;
}

export function useAuth(): UseAuthReturn {
  const [session, setSession] = useState<AuthSession | null>(null);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    setSession(authService.getSession());
  }, []);

  const login = useCallback(async (values: LoginFormValues): Promise<boolean> => {
    setIsLoading(true);
    setError(null);
    try {
      const newSession = await authService.login(values);
      setSession(newSession);
      return true;
    } catch (err) {
      setError(err instanceof Error ? err.message : 'No se pudo iniciar sesión.');
      return false;
    } finally {
      setIsLoading(false);
    }
  }, []);

  const logout = useCallback(() => {
    authService.logout();
    setSession(null);
  }, []);

  return { session, isLoading, error, login, logout };
}
