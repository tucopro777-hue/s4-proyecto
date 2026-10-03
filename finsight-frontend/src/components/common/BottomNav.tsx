import { Home, PieChart, Plus, Target, User } from 'lucide-react';
import { useState } from 'react';

const ITEMS = [
  { id: 'inicio', label: 'Inicio', icon: Home },
  { id: 'reportes', label: 'Reportes', icon: PieChart },
  { id: 'agregar', label: '', icon: Plus },
  { id: 'metas', label: 'Metas', icon: Target },
  { id: 'perfil', label: 'Perfil', icon: User },
] as const;

export default function BottomNav() {
  const [activo, setActivo] = useState<string>('inicio');

  return (
    <nav
      className="fixed bottom-0 left-0 right-0 bg-card border-t border-border px-2 pt-2"
      style={{ paddingBottom: 'max(0.5rem, env(safe-area-inset-bottom))' }}
    >
      <div className="flex items-center justify-between max-w-sm mx-auto">
        {ITEMS.map(({ id, label, icon: Icon }) => {
          const esCentral = id === 'agregar';
          const estaActivo = activo === id;

          if (esCentral) {
            return (
              <button
                key={id}
                type="button"
                onClick={() => setActivo(id)}
                aria-label="Agregar movimiento"
                className="w-12 h-12 rounded-full bg-lime flex items-center justify-center -translate-y-3 active:scale-95 transition-transform shadow-lg shadow-lime/20"
              >
                <Icon className="w-6 h-6 text-bg" />
              </button>
            );
          }

          return (
            <button
              key={id}
              type="button"
              onClick={() => setActivo(id)}
              className="flex flex-col items-center gap-1 px-3 py-1.5"
              aria-current={estaActivo ? 'page' : undefined}
            >
              <Icon className={`w-5 h-5 ${estaActivo ? 'text-lime' : 'text-muted'}`} />
              <span className={`font-body text-[10px] ${estaActivo ? 'text-lime' : 'text-muted'}`}>
                {label}
              </span>
            </button>
          );
        })}
      </div>
    </nav>
  );
}
