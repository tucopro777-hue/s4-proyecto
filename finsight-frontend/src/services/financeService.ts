import type { Movimiento, ResumenFinanciero } from '@/types';

/**
 * Datos de ejemplo. Sustituir por llamadas a la API real, p. ej.:
 * GET /api/movimientos, GET /api/resumen — servidas por Spring Boot.
 */
const MOVIMIENTOS_MOCK: Movimiento[] = [
  {
    id: '1',
    tipo: 'gasto',
    categoria: 'alimentacion',
    descripcion: 'Supermercado Ketal',
    monto: 245.5,
    fecha: '2026-09-18',
  },
  {
    id: '2',
    tipo: 'ingreso',
    categoria: 'salario',
    descripcion: 'Sueldo mensual',
    monto: 6500,
    fecha: '2026-09-15',
  },
  {
    id: '3',
    tipo: 'gasto',
    categoria: 'transporte',
    descripcion: 'Gasolina',
    monto: 120,
    fecha: '2026-09-14',
  },
  {
    id: '4',
    tipo: 'gasto',
    categoria: 'entretenimiento',
    descripcion: 'Cine con amigos',
    monto: 65,
    fecha: '2026-09-12',
  },
  {
    id: '5',
    tipo: 'ingreso',
    categoria: 'freelance',
    descripcion: 'Proyecto diseño web',
    monto: 1200,
    fecha: '2026-09-10',
  },
  {
    id: '6',
    tipo: 'gasto',
    categoria: 'salud',
    descripcion: 'Farmacia',
    monto: 89.9,
    fecha: '2026-09-08',
  },
];

export const financeService = {
  async getMovimientos(): Promise<Movimiento[]> {
    await new Promise((resolve) => setTimeout(resolve, 400));
    return [...MOVIMIENTOS_MOCK].sort(
      (a, b) => new Date(b.fecha).getTime() - new Date(a.fecha).getTime()
    );
  },

  async getResumen(): Promise<ResumenFinanciero> {
    await new Promise((resolve) => setTimeout(resolve, 300));
    const ingresosMes = MOVIMIENTOS_MOCK.filter((m) => m.tipo === 'ingreso').reduce(
      (sum, m) => sum + m.monto,
      0
    );
    const gastosMes = MOVIMIENTOS_MOCK.filter((m) => m.tipo === 'gasto').reduce(
      (sum, m) => sum + m.monto,
      0
    );

    return {
      saldoTotal: 12480.75,
      ingresosMes,
      gastosMes,
    };
  },
};
