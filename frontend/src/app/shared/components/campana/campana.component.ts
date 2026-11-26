import { Component, ElementRef, HostListener, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { AlertasPresupuestoService } from '../../../core/alertas/alertas-presupuesto.service';
import { etiquetaNivel, porcentajeConsumo } from '../../../features/presupuestos/presupuesto.service';

/**
 * Campanita de la barra de navegación: avisa de los presupuestos del mes que están
 * al límite (80 % o más) o excedidos. El número indica cuántos requieren atención.
 */
@Component({
  selector: 'app-campana',
  standalone: true,
  imports: [DecimalPipe, RouterLink],
  templateUrl: './campana.component.html',
  styleUrl: './campana.component.scss',
})
export class CampanaComponent implements OnInit {
  private readonly servicio = inject(AlertasPresupuestoService);
  private readonly elemento: ElementRef<HTMLElement> = inject(ElementRef);

  readonly alertas = this.servicio.alertas;
  readonly hayExcedidos = this.servicio.hayExcedidos;
  readonly porcentaje = porcentajeConsumo;
  readonly etiqueta = etiquetaNivel;

  abierta = false;

  ngOnInit(): void {
    this.servicio.refrescar();
  }

  alternar(): void {
    this.abierta = !this.abierta;
  }

  /** Cierra el panel al pulsar en cualquier otro punto de la página. */
  @HostListener('document:click', ['$event'])
  alPulsarFuera(evento: MouseEvent): void {
    if (this.abierta && !this.elemento.nativeElement.contains(evento.target as Node)) {
      this.abierta = false;
    }
  }

  @HostListener('document:keydown.escape')
  alPulsarEscape(): void {
    this.abierta = false;
  }
}
