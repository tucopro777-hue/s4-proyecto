import { useAuth } from '@/hooks/useAuth';
import LoginScreen from '@/components/auth/LoginScreen';
import DashboardScreen from '@/components/dashboard/DashboardScreen';

export default function App() {
  const { session, isLoading, error, login, logout } = useAuth();

  if (session) {
    return <DashboardScreen user={session.user} onLogout={logout} />;
  }

  return <LoginScreen onLogin={login} isLoading={isLoading} serverError={error} />;
}
