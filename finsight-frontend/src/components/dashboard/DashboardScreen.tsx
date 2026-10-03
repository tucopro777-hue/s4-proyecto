import { useEffect, useState } from 'react';
import type { Movimiento, ResumenFinanciero, User } from '@/types';
import { financeService } from '@/services/financeService';
import DashboardHeader from './DashboardHeader';
import SummaryCard from './SummaryCard';
import QuickActions from './QuickActions';
import MovimientosList from './MovimientosList';
import BottomNav from '@/components/common/BottomNav';

interface DashboardScreenProps {
  user: User;
  onLogout: () => void;
}

export default function DashboardScreen({ user, onLogout }: DashboardScreenProps) {
  const [resumen, setResumen] = useState<ResumenFinanciero | null>(null);
  const [movimientos, setMovimientos] = useState<Movimiento[]>([]);
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    let cancelado = false;

    async function cargarDatos() {
      setIsLoading(true);
      const [resumenData, movimientosData] = await Promise.all([
        financeService.getResumen(),
        financeService.getMovimientos(),
      ]);

      if (!cancelado) {
        setResumen(resumenData);
        setMovimientos(movimientosData);
        setIsLoading(false);
      }
    }

    cargarDatos();
    return () => {
      cancelado = true;
    };
  }, []);

  const handleRegistrarGasto = () => {
    // TODO: abrir formulario / modal de registro de gasto
    console.log('Registrar gasto');
  };

  const handleRegistrarIngreso = () => {
    // TODO: abrir formulario / modal de registro de ingreso
    console.log('Registrar ingreso');
  };

  return (
    <div className="min-h-dvh bg-bg pb-24">
      <DashboardHeader user={user} onLogout={onLogout} />

      <SummaryCard
        resumen={resumen ?? { saldoTotal: 0, ingresosMes: 0, gastosMes: 0 }}
        isLoading={isLoading}
      />

      <QuickActions
        onRegistrarGasto={handleRegistrarGasto}
        onRegistrarIngreso={handleRegistrarIngreso}
      />

      <MovimientosList movimientos={movimientos} isLoading={isLoading} />

      <BottomNav />
    </div>
  );
}
