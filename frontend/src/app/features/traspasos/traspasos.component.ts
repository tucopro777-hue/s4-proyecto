import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DatePipe, DecimalPipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { Billetera, BilleteraService } from '../billeteras/billetera.service';
import { Traspaso, TraspasoService, mensajeErrorTraspaso } from './traspaso.service';

/** Fecha de hoy en formato yyyy-MM-dd según la zona horaria local. */
function hoy(): string {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-traspasos',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent, DecimalPipe, DatePipe],
  templateUrl: './traspasos.component.html',
  styleUrl: './traspasos.component.scss',
})
export class TraspasosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly traspasoService = inject(TraspasoService);
  private readonly billeteraService = inject(BilleteraService);
  private readonly destroyRef = inject(DestroyRef);

  billeteras: Billetera[] = [];
  traspasos: Traspaso[] = [];
  cargando = true;
  guardando = false;
  error: string | null = null;
  exito: string | null = null;

  readonly form = this.fb.nonNullable.group({
    billeteraOrigenId: [null as number | null, [Validators.required]],
    billeteraDestinoId: [null as number | null, [Validators.required]],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    fecha: [hoy(), [Validators.required]],
    descripcion: ['', [Validators.maxLength(255)]],
  });

  constructor() {
    // Si cambia el origen, el destino elegido puede dejar de ser válido: se limpia
    this.form.controls.billeteraOrigenId.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.billeteraDestinoId.setValue(null));
  }

  ngOnInit(): void {
    this.cargarTodo();
  }

  /** Billetera de origen seleccionada. */
  get origen(): Billetera | undefined {
    const id = this.form.controls.billeteraOrigenId.value;
    return this.billeteras.find((b) => b.id === id);
  }

  /** Destinos válidos: distintos del origen y de la misma divisa (el backend no convierte divisas). */
  get destinosPosibles(): Billetera[] {
    const origen = this.origen;
    if (!origen) {
      return [];
    }
    return this.billeteras.filter((b) => b.id !== origen.id && b.divisa === origen.divisa);
  }

  get excedeSaldo(): boolean {
    const monto = this.form.controls.monto.value;
    return !!this.origen && monto !== null && Number(monto) > this.origen.saldoActual;
  }

  get puedeEnviar(): boolean {
    return this.form.valid && !this.excedeSaldo && !this.guardando;
  }

  transferir(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    if (this.excedeSaldo) {
      return;
    }

    const valores = this.form.getRawValue();
    this.guardando = true;
    this.error = null;
    this.exito = null;

    this.traspasoService
      .transferir({
        billeteraOrigenId: Number(valores.billeteraOrigenId),
        billeteraDestinoId: Number(valores.billeteraDestinoId),
        monto: Number(valores.monto),
        descripcion: valores.descripcion.trim() || null,
        fecha: valores.fecha,
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Traspaso realizado. Los saldos se han actualizado.';
          // Se conserva el origen para encadenar traspasos; el destino se vuelve a elegir
          this.form.reset({
            billeteraOrigenId: valores.billeteraOrigenId,
            billeteraDestinoId: null,
            monto: null,
            fecha: hoy(),
            descripcion: '',
          });
          this.cargarTodo();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorTraspaso(err);
        },
      });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  /** Recarga billeteras (saldos) e historial para que ambas vistas reflejen el traspaso. */
  private cargarTodo(): void {
    this.cargando = true;
    forkJoin({
      billeteras: this.billeteraService.listar(),
      traspasos: this.traspasoService.listar(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ billeteras, traspasos }) => {
          this.billeteras = billeteras;
          this.traspasos = traspasos;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          this.cargando = false;
          this.error = mensajeErrorTraspaso(err);
        },
      });
  }
}
