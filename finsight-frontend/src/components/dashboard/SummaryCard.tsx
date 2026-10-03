import { ArrowDownLeft, ArrowUpRight } from 'lucide-react';
import type { ResumenFinanciero } from '@/types';
import { formatearMoneda } from '@/utils/validation';

interface SummaryCardProps {
  resumen: ResumenFinanciero;
  isLoading: boolean;
}

export default function SummaryCard({ resumen, isLoading }: SummaryCardProps) {
  if (isLoading) {
    return (
      <div className="mx-5 mt-4 rounded-2xl bg-card border border-border p-6 animate-pulse">
        <div className="h-3 w-24 bg-border rounded mb-3" />
        <div className="h-9 w-40 bg-border rounded mb-6" />
        <div className="grid grid-cols-2 gap-3">
          <div className="h-16 bg-border/60 rounded-xl" />
          <div className="h-16 bg-border/60 rounded-xl" />
        </div>
      </div>
    );
  }

  return (
    <div className="mx-5 mt-4 rounded-2xl bg-card border border-border p-6">
      <p className="font-body text-xs text-muted uppercase tracking-wide">Saldo total</p>
      <p className="font-mono text-[2.1rem] leading-tight text-white mt-2 tabular-nums">
        {formatearMoneda(resumen.saldoTotal)}
      </p>

      <div className="grid grid-cols-2 gap-3 mt-5">
        <div className="rounded-xl bg-bg/60 border border-border/60 px-4 py-3">
          <div className="flex items-center gap-1.5 mb-1">
            <ArrowUpRight className="w-3.5 h-3.5 text-teal" />
            <span className="font-body text-xs text-muted">Ingresos</span>
          </div>
          <p className="font-mono text-base text-teal tabular-nums">
            {formatearMoneda(resumen.ingresosMes)}
          </p>
        </div>

        <div className="rounded-xl bg-bg/60 border border-border/60 px-4 py-3">
          <div className="flex items-center gap-1.5 mb-1">
            <ArrowDownLeft className="w-3.5 h-3.5 text-red" />
            <span className="font-body text-xs text-muted">Gastos</span>
          </div>
          <p className="font-mono text-base text-red tabular-nums">
            {formatearMoneda(resumen.gastosMes)}
          </p>
        </div>
      </div>
    </div>
  );
}
