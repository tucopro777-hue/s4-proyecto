import { Bell, LogOut } from 'lucide-react';
import type { User } from '@/types';

interface DashboardHeaderProps {
  user: User;
  onLogout: () => void;
}

function obtenerIniciales(nombre: string): string {
  return nombre
    .split(' ')
    .slice(0, 2)
    .map((palabra) => palabra.charAt(0))
    .join('')
    .toUpperCase();
}

function obtenerSaludo(): string {
  const hora = new Date().getHours();
  if (hora < 12) return 'Buenos días';
  if (hora < 19) return 'Buenas tardes';
  return 'Buenas noches';
}

export default function DashboardHeader({ user, onLogout }: DashboardHeaderProps) {
  return (
    <header className="flex items-center justify-between px-5 pt-6 pb-2">
      <div className="flex items-center gap-3 min-w-0">
        <div className="w-11 h-11 rounded-full bg-teal/15 border border-teal/25 flex items-center justify-center flex-shrink-0">
          <span className="font-body font-semibold text-sm text-teal">
            {obtenerIniciales(user.nombre)}
          </span>
        </div>
        <div className="min-w-0">
          <p className="font-body text-xs text-muted leading-none mb-1">{obtenerSaludo()}</p>
          <h2 className="font-body font-semibold text-white text-[15px] truncate leading-none">
            {user.nombre}
          </h2>
        </div>
      </div>

      <div className="flex items-center gap-1.5 flex-shrink-0">
        <button
          type="button"
          className="w-10 h-10 rounded-full flex items-center justify-center text-muted hover:text-white hover:bg-card transition-colors"
          aria-label="Notificaciones"
        >
          <Bell className="w-5 h-5" />
        </button>
        <button
          type="button"
          onClick={onLogout}
          className="w-10 h-10 rounded-full flex items-center justify-center text-muted hover:text-red hover:bg-card transition-colors"
          aria-label="Cerrar sesión"
        >
          <LogOut className="w-5 h-5" />
        </button>
      </div>
    </header>
  );
}
