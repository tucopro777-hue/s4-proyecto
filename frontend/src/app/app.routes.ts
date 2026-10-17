import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  {
    path: 'login',
    loadComponent: () =>
      import('./features/auth/login/login.component').then((m) => m.LoginComponent),
  },
  {
    path: 'registro',
    loadComponent: () =>
      import('./features/auth/registro/registro.component').then((m) => m.RegistroComponent),
  },
  {
    path: 'dashboard',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/dashboard/dashboard.component').then((m) => m.DashboardComponent),
  },
  {
    path: 'billeteras',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/billeteras/billeteras.component').then((m) => m.BilleteraComponent),
  },
  {
    path: 'categorias',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/categorias/categorias.component').then((m) => m.CategoriasComponent),
  },
  {
    path: 'transacciones',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/transacciones/transacciones.component').then((m) => m.TransaccionesComponent),
  },
  {
    path: 'presupuestos',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/presupuestos/presupuestos.component').then((m) => m.PresupuestosComponent),
  },
  {
    path: 'metas',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./features/metas/metas.component').then((m) => m.MetasComponent),
  },
  { path: '**', redirectTo: 'login' },
];
