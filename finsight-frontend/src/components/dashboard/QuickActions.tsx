import { Minus, Plus } from 'lucide-react';

interface QuickActionsProps {
  onRegistrarGasto: () => void;
  onRegistrarIngreso: () => void;
}

export default function QuickActions({ onRegistrarGasto, onRegistrarIngreso }: QuickActionsProps) {
  return (
    <div className="grid grid-cols-2 gap-3 px-5 mt-4">
      <button
        type="button"
        onClick={onRegistrarGasto}
        className="flex items-center justify-center gap-2 rounded-xl bg-card border border-border py-3.5 font-body font-medium text-sm text-white active:scale-[0.98] transition-transform"
      >
        <span className="w-6 h-6 rounded-full bg-red/15 flex items-center justify-center flex-shrink-0">
          <Minus className="w-3.5 h-3.5 text-red" />
        </span>
        Registrar gasto
      </button>

      <button
        type="button"
        onClick={onRegistrarIngreso}
        className="flex items-center justify-center gap-2 rounded-xl bg-card border border-border py-3.5 font-body font-medium text-sm text-white active:scale-[0.98] transition-transform"
      >
        <span className="w-6 h-6 rounded-full bg-teal/15 flex items-center justify-center flex-shrink-0">
          <Plus className="w-3.5 h-3.5 text-teal" />
        </span>
        Registrar ingreso
      </button>
    </div>
  );
}
