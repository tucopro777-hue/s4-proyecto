export interface User {
  id: string;
  nombre: string;
  correo: string;
  avatarUrl?: string;
}

export interface AuthSession {
  token: string;
  user: User;
  expiresAt: number;
}

export type TipoMovimiento = 'ingreso' | 'gasto';

export type CategoriaGasto =
  | 'alimentacion'
  | 'transporte'
  | 'vivienda'
  | 'entretenimiento'
  | 'salud'
  | 'educacion'
  | 'otros';

export type CategoriaIngreso = 'salario' | 'freelance' | 'inversion' | 'otros';

export interface Movimiento {
  id: string;
  tipo: TipoMovimiento;
  categoria: CategoriaGasto | CategoriaIngreso;
  descripcion: string;
  monto: number;
  fecha: string; // ISO date string
}

export interface ResumenFinanciero {
  saldoTotal: number;
  ingresosMes: number;
  gastosMes: number;
}

export interface LoginFormValues {
  correo: string;
  contrasena: string;
}

export interface LoginFormErrors {
  correo?: string;
  contrasena?: string;
}
