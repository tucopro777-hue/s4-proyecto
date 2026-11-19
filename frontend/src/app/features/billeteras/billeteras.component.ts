import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { ModalComponent } from '../../shared/components/modal/modal.component';
import { Billetera, BilleteraService, mensajeErrorBilletera } from './billetera.service';
import { AsignacionInteligente, AsignacionService, mensajeErrorAsignacion } from '../asignacion/asignacion.service';

/** Rechaza nombres vacíos o formados solo por espacios. */
function nombreNoVacio(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value ?? '';
  return valor.trim() ? null : { vacio: true };
}

@Component({
  selector: 'app-billeteras',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent, ModalComponent, DecimalPipe],
  templateUrl: './billeteras.component.html',
  styleUrl: './billeteras.component.scss',
})
export class BilleteraComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly billeteraService = inject(BilleteraService);
  private readonly asignacionService = inject(AsignacionService);

  billeteras: Billetera[] = [];
  cargando = true;
  calculandoAsignacion = false;
  guardando = false;
  desactivandoId: number | null = null;
  /** Billetera cuya desactivación espera la contraseña del usuario. */
  billeteraADesactivar: Billetera | null = null;
  errorDesactivar: string | null = null;
  error: string | null = null;
  exito: string | null = null;

  readonly divisas = ['BOB', 'USD', 'EUR'];

  readonly formDesactivar = this.fb.nonNullable.group({
    password: ['', [Validators.required]],
  });

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100), nombreNoVacio]],
    saldoInicial: [0, [Validators.required, Validators.min(0)]],
    divisa: ['BOB', [Validators.required, Validators.pattern(/^[A-Z]{3}$/)]],
  });

  /** Controla la ventana emergente del formulario de alta. */
  modalAbierto = false;

  abrirModal(): void {
    this.error = null;
    this.exito = null;
    this.modalAbierto = true;
  }

  cerrarModal(): void {
    this.modalAbierto = false;
    this.error = null;
  }

  ngOnInit(): void {
    this.cargarBilleteras();
  }

  /** Dinero disponible: suma de los saldos de las billeteras activas. */
  get dineroDisponible(): number {
    return this.billeteras
      .filter((b) => b.activa)
      .reduce((suma, b) => suma + b.saldoActual, 0);
  }

  /** Pide la asignación inteligente del dinero disponible y la muestra en una alerta. */
  asignacionInteligente(): void {
    const disponible = this.dineroDisponible;
    this.calculandoAsignacion = true;
    this.error = null;
    this.exito = null;

    this.asignacionService.recomendar(disponible).subscribe({
      next: (resultado) => {
        this.calculandoAsignacion = false;
        window.alert(this.textoAsignacion(disponible, resultado));
      },
      error: (err: HttpErrorResponse) => {
        this.calculandoAsignacion = false;
        this.error = mensajeErrorAsignacion(err);
      },
    });
  }

  private textoAsignacion(disponible: number, resultado: AsignacionInteligente): string {
    const formato = (valor: number) => valor.toFixed(2);

    if (resultado.recomendadas.length === 0) {
      return `Con ${formato(disponible)} no hay metas pendientes que cubrir. ` +
        'Crea una meta de ahorro en la sección Metas para recibir una sugerencia.';
    }

    const lineas = resultado.recomendadas.map(
      (m) => `• Aporta ${formato(m.costo)} a «${m.nombre}» (prioridad ${m.prioridad})`,
    );

    return [
      `Asignación inteligente para ${formato(disponible)}:`,
      '',
      ...lineas,
      '',
      `Total sugerido: ${formato(resultado.totalAsignado)}`,
      `Puntos de prioridad: ${resultado.puntosPrioridad}`,
      `Sobrante: ${formato(resultado.sobrante)}`,
    ].join('\n');
  }

  registrar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    this.guardando = true;
    this.error = null;
    this.exito = null;

    this.billeteraService
      .crear({ ...valores, nombre: valores.nombre.trim() })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.modalAbierto = false;
          this.exito = 'Billetera registrada correctamente.';
          this.form.reset({ nombre: '', saldoInicial: 0, divisa: 'BOB' });
          this.cargarBilleteras();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorBilletera(err);
        },
      });
  }

  /** Abre la confirmación con contraseña para la billetera elegida. */
  iniciarDesactivacion(billetera: Billetera): void {
    this.billeteraADesactivar = billetera;
    this.formDesactivar.reset({ password: '' });
    this.errorDesactivar = null;
  }

  cancelarDesactivacion(): void {
    this.billeteraADesactivar = null;
    this.errorDesactivar = null;
  }

  confirmarDesactivacion(): void {
    const billetera = this.billeteraADesactivar;
    if (!billetera) {
      return;
    }
    if (this.formDesactivar.invalid) {
      this.formDesactivar.markAllAsTouched();
      return;
    }

    this.desactivandoId = billetera.id;
    this.errorDesactivar = null;
    this.exito = null;

    this.billeteraService.desactivar(billetera.id, this.formDesactivar.getRawValue().password).subscribe({
      next: () => {
        this.desactivandoId = null;
        this.billeteraADesactivar = null;
        this.exito = `Billetera "${billetera.nombre}" desactivada.`;
        // Se quita de la lista local para que la vista se actualice al instante
        this.billeteras = this.billeteras.filter((b) => b.id !== billetera.id);
      },
      error: (err: HttpErrorResponse) => {
        this.desactivandoId = null;
        this.errorDesactivar = mensajeErrorBilletera(err);
      },
    });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  private cargarBilleteras(): void {
    this.cargando = true;
    this.billeteraService.listar().subscribe({
      next: (lista) => {
        this.billeteras = lista;
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.error = mensajeErrorBilletera(err);
      },
    });
  }
}
