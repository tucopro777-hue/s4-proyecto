import type { Movimiento } from '@/types';
import MovimientoItem from './MovimientoItem';

interface MovimientosListProps {
  movimientos: Movimiento[];
  isLoading: boolean;
}

function MovimientoSkeleton() {
  return (
    <div className="flex items-center gap-3 py-3 animate-pulse">
      <div className="w-10 h-10 rounded-full bg-border flex-shrink-0" />
      <div className="flex-1 space-y-2">
        <div className="h-3.5 w-32 bg-border rounded" />
        <div className="h-2.5 w-20 bg-border/60 rounded" />
      </div>
      <div className="h-3.5 w-16 bg-border rounded" />
    </div>
  );
}

export default function MovimientosList({ movimientos, isLoading }: MovimientosListProps) {
  return (
    <section className="px-5 mt-6 pb-8">
      <div className="flex items-center justify-between mb-1">
        <h3 className="font-body font-semibold text-white text-[15px]">Últimos movimientos</h3>
        <button type="button" className="font-body text-xs text-teal hover:text-teal/80 transition-colors">
          Ver todos
        </button>
      </div>

      <div className="divide-y divide-border/60">
        {isLoading ? (
          <>
            <MovimientoSkeleton />
            <MovimientoSkeleton />
            <MovimientoSkeleton />
          </>
        ) : movimientos.length === 0 ? (
          <p className="font-body text-sm text-muted py-6 text-center">
            Todavía no registraste movimientos este mes.
          </p>
        ) : (
          movimientos.map((movimiento) => (
            <MovimientoItem key={movimiento.id} movimiento={movimiento} />
          ))
        )}
      </div>
    </section>
  );
}
