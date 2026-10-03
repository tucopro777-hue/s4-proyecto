import type { Movimiento } from '@/types';
import { obtenerVisualCategoria } from '@/utils/categoryIcons';
import { formatearFecha, formatearMoneda } from '@/utils/validation';

interface MovimientoItemProps {
  movimiento: Movimiento;
}

export default function MovimientoItem({ movimiento }: MovimientoItemProps) {
  const visual = obtenerVisualCategoria(movimiento.categoria);
  const Icon = visual.icon;
  const esIngreso = movimiento.tipo === 'ingreso';

  return (
    <div className="flex items-center gap-3 py-3">
      <div className={`w-10 h-10 rounded-full ${visual.bg} flex items-center justify-center flex-shrink-0`}>
        <Icon className={`w-4.5 h-4.5 ${visual.color}`} />
      </div>

      <div className="min-w-0 flex-1">
        <p className="font-body text-[15px] text-white truncate leading-tight">
          {movimiento.descripcion}
        </p>
        <p className="font-body text-xs text-muted mt-0.5">
          {visual.label} · {formatearFecha(movimiento.fecha)}
        </p>
      </div>

      <p
        className={`font-mono text-sm tabular-nums flex-shrink-0 ${
          esIngreso ? 'text-teal' : 'text-red'
        }`}
      >
        {esIngreso ? '+' : '-'}
        {formatearMoneda(movimiento.monto)}
      </p>
    </div>
  );
}
