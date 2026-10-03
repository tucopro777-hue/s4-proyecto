import {
  Briefcase,
  Car,
  Ellipsis,
  Film,
  GraduationCap,
  HeartPulse,
  Home,
  type LucideIcon,
  ShoppingCart,
  TrendingUp,
  Wallet,
} from 'lucide-react';
import type { CategoriaGasto, CategoriaIngreso } from '@/types';

type Categoria = CategoriaGasto | CategoriaIngreso;

interface CategoriaVisual {
  icon: LucideIcon;
  bg: string;
  color: string;
  label: string;
}

const MAPA_CATEGORIAS: Record<Categoria, CategoriaVisual> = {
  alimentacion: { icon: ShoppingCart, bg: 'bg-orange/15', color: 'text-orange', label: 'Alimentación' },
  transporte: { icon: Car, bg: 'bg-teal/15', color: 'text-teal', label: 'Transporte' },
  vivienda: { icon: Home, bg: 'bg-lime/15', color: 'text-lime', label: 'Vivienda' },
  entretenimiento: { icon: Film, bg: 'bg-red/15', color: 'text-red', label: 'Entretenimiento' },
  salud: { icon: HeartPulse, bg: 'bg-red/15', color: 'text-red', label: 'Salud' },
  educacion: { icon: GraduationCap, bg: 'bg-teal/15', color: 'text-teal', label: 'Educación' },
  salario: { icon: Wallet, bg: 'bg-lime/15', color: 'text-lime', label: 'Salario' },
  freelance: { icon: Briefcase, bg: 'bg-teal/15', color: 'text-teal', label: 'Freelance' },
  inversion: { icon: TrendingUp, bg: 'bg-lime/15', color: 'text-lime', label: 'Inversión' },
  otros: { icon: Ellipsis, bg: 'bg-muted/15', color: 'text-muted', label: 'Otros' },
};

export function obtenerVisualCategoria(categoria: Categoria): CategoriaVisual {
  return MAPA_CATEGORIAS[categoria] ?? MAPA_CATEGORIAS.otros;
}
