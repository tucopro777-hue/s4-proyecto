import { Component, EventEmitter, HostListener, Input, Output } from '@angular/core';

/**
 * Ventana emergente reutilizable. El contenido se proyecta con <ng-content>, así que el
 * formulario sigue perteneciendo a la pantalla que lo declara (y conserva sus estilos).
 * Se cierra con el botón ×, la tecla Escape o pulsando fuera de la ventana.
 */
@Component({
  selector: 'app-modal',
  standalone: true,
  template: `
    @if (abierto) {
      <div class="fondo" (mousedown)="alPulsarFondo($event)">
        <section class="ventana" role="dialog" aria-modal="true" [attr.aria-label]="titulo">
          <header class="cabecera">
            <h2>{{ titulo }}</h2>
            <button type="button" class="cerrar" aria-label="Cerrar" (click)="cerrar.emit()">×</button>
          </header>
          <div class="cuerpo">
            <ng-content />
          </div>
        </section>
      </div>
    }
  `,
  styles: [
    `
      .fondo {
        position: fixed;
        inset: 0;
        z-index: 100;
        display: flex;
        align-items: flex-start;
        justify-content: center;
        padding: 6vh 16px 24px;
        overflow-y: auto;
        background: rgba(0, 0, 0, 0.65);
        backdrop-filter: blur(3px);
        animation: aparecer 0.15s ease-out;
      }

      .ventana {
        width: 100%;
        max-width: 480px;
        background: var(--color-superficie);
        border: 1px solid var(--color-borde);
        border-radius: 16px;
        box-shadow: 0 24px 60px rgba(0, 0, 0, 0.6);
        animation: subir 0.18s ease-out;
      }

      .cabecera {
        display: flex;
        align-items: center;
        justify-content: space-between;
        gap: 12px;
        padding: 18px 22px;
        border-bottom: 1px solid var(--color-borde);
      }

      h2 {
        margin: 0;
        font-size: 1.1rem;
      }

      .cerrar {
        width: 32px;
        height: 32px;
        border: none;
        border-radius: 8px;
        background: transparent;
        color: var(--color-texto-suave);
        font-size: 1.5rem;
        line-height: 1;
        cursor: pointer;
      }

      .cerrar:hover,
      .cerrar:focus-visible {
        background: var(--color-fondo);
        color: var(--color-texto);
        outline: none;
      }

      .cuerpo {
        padding: 22px;
      }

      @keyframes aparecer {
        from {
          opacity: 0;
        }
      }

      @keyframes subir {
        from {
          opacity: 0;
          transform: translateY(12px);
        }
      }

      @media (prefers-reduced-motion: reduce) {
        .fondo,
        .ventana {
          animation: none;
        }
      }
    `,
  ],
})
export class ModalComponent {
  @Input() titulo = '';
  @Input() abierto = false;
  @Output() cerrar = new EventEmitter<void>();

  @HostListener('document:keydown.escape')
  alPulsarEscape(): void {
    if (this.abierto) {
      this.cerrar.emit();
    }
  }

  /** Solo cierra si la pulsación empezó en el fondo, no al arrastrar una selección desde dentro. */
  alPulsarFondo(evento: MouseEvent): void {
    if (evento.target === evento.currentTarget) {
      this.cerrar.emit();
    }
  }
}
