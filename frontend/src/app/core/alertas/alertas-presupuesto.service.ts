import { Injectable, computed, inject, signal } from '@angular/core';
import { Presupuesto, PresupuestoService } from '../../features/presupuestos/presupuesto.service';

/**
 * Estado compartido de los presupuestos del mes en curso. Lo consumen la campanita de la
 * barra de navegación y el panel; las pantallas que cambian el consumo (movimientos,
 * presupuestos) llaman a refrescar() para que el aviso se actualice al instante.
 */
@Injectable({ providedIn: 'root' })
export class AlertasPresupuestoService {
  private readonly presupuestoService = inject(PresupuestoService);

  private readonly estado = signal<Presupuesto[]>([]);

  /** Todos los presupuestos del mes en curso. */
  readonly presupuestos = this.estado.asReadonly();

  /** Solo los que están al límite (80 % o más) o excedidos; primero los excedidos. */
  readonly alertas = computed(() =>
    this.estado()
      .filter((p) => p.nivel !== 'OK')
      .sort((a, b) => Number(b.nivel === 'EXCEDIDO') - Number(a.nivel === 'EXCEDIDO')),
  );

  readonly hayExcedidos = computed(() => this.alertas().some((p) => p.nivel === 'EXCEDIDO'));

  refrescar(): void {
    const hoy = new Date();
    this.presupuestoService.listar(hoy.getFullYear(), hoy.getMonth() + 1).subscribe({
      next: (lista) => this.estado.set(lista),
      // Un fallo aquí no debe romper la pantalla: cada pantalla ya muestra sus propios errores
      error: () => undefined,
    });
  }

  /** Se llama al cerrar sesión para no mostrar avisos de otro usuario. */
  limpiar(): void {
    this.estado.set([]);
  }
}
