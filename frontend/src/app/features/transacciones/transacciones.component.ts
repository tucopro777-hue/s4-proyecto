import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { Billetera, BilleteraService } from '../billeteras/billetera.service';
import { Categoria, CategoriaService } from '../categorias/categoria.service';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import {
  AlertaPresupuesto,
  Transaccion,
  TipoTransaccion,
  TransaccionService,
  mensajeErrorTransaccion,
} from './transaccion.service';

/** Fecha de hoy en formato yyyy-MM-dd según la zona horaria local. */
function hoy(): string {
  const ahora = new Date();
  const local = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
  return local.toISOString().slice(0, 10);
}

@Component({
  selector: 'app-transacciones',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent, DecimalPipe, DatePipe],
  templateUrl: './transacciones.component.html',
  styleUrl: './transacciones.component.scss',
})
export class TransaccionesComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly transaccionService = inject(TransaccionService);
  private readonly billeteraService = inject(BilleteraService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly destroyRef = inject(DestroyRef);

  billeteras: Billetera[] = [];
  categorias: Categoria[] = [];
  transacciones: Transaccion[] = [];
  cargando = true;
  guardando = false;
  error: string | null = null;
  exito: string | null = null;
  /** Aviso de presupuesto del último gasto registrado; no bloquea la operación. */
  alerta: AlertaPresupuesto | null = null;
  anulandoId: number | null = null;

  readonly tipos: { valor: TipoTransaccion; etiqueta: string }[] = [
    { valor: 'GASTO', etiqueta: 'Gasto' },
    { valor: 'INGRESO', etiqueta: 'Ingreso' },
  ];

  readonly form = this.fb.nonNullable.group({
    billeteraId: [null as number | null, [Validators.required]],
    tipo: ['GASTO' as TipoTransaccion, [Validators.required]],
    categoriaId: [null as number | null, [Validators.required]],
    monto: [null as number | null, [Validators.required, Validators.min(0.01)]],
    fecha: [hoy(), [Validators.required]],
    descripcion: ['', [Validators.maxLength(255)]],
  });

  constructor() {
    // Al cambiar el tipo, la categoría elegida deja de ser válida: se limpia para volver a elegir
    this.form.controls.tipo.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.form.controls.categoriaId.setValue(null));
  }

  ngOnInit(): void {
    this.cargarTodo();
  }

  /** Categorías activas que coinciden con el tipo del movimiento seleccionado. */
  get categoriasDelTipo(): Categoria[] {
    const tipo = this.form.controls.tipo.value;
    return this.categorias.filter((c) => c.tipo === tipo);
  }

  /** Saldo de la billetera elegida, para avisar antes de intentar un gasto. */
  get saldoSeleccionado(): number | null {
    const id = this.form.controls.billeteraId.value;
    return this.billeteras.find((b) => b.id === id)?.saldoActual ?? null;
  }

  get recientes(): Transaccion[] {
    return this.transacciones.slice(0, 20);
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
    this.alerta = null;

    this.transaccionService
      .registrar({
        monto: Number(valores.monto),
        tipo: valores.tipo,
        descripcion: valores.descripcion.trim() || null,
        fecha: valores.fecha,
        billeteraId: Number(valores.billeteraId),
        categoriaId: Number(valores.categoriaId),
      })
      .subscribe({
        next: (creada) => {
          this.guardando = false;
          this.exito = 'Movimiento registrado correctamente.';
          this.alerta = creada.alertaPresupuesto;
          // Se conserva la billetera para registrar varios movimientos seguidos
          this.form.reset({
            billeteraId: valores.billeteraId,
            tipo: 'GASTO',
            categoriaId: null,
            monto: null,
            fecha: hoy(),
            descripcion: '',
          });
          this.cargarTodo();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorTransaccion(err);
        },
      });
  }

  anular(transaccion: Transaccion): void {
    const confirmado = window.confirm(
      `¿Anular el movimiento de ${transaccion.monto} en "${transaccion.billeteraNombre}"? ` +
        'El saldo de la billetera y el presupuesto se revertirán.',
    );
    if (!confirmado) {
      return;
    }

    this.anulandoId = transaccion.id;
    this.error = null;
    this.exito = null;
    this.alerta = null;

    this.transaccionService.anular(transaccion.id).subscribe({
      next: () => {
        this.anulandoId = null;
        this.exito = 'Movimiento anulado. Saldo y presupuesto actualizados.';
        this.cargarTodo();
      },
      error: (err: HttpErrorResponse) => {
        this.anulandoId = null;
        this.error = mensajeErrorTransaccion(err);
      },
    });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  /** Recarga saldos, categorías e historial para que todas las vistas reflejen el movimiento. */
  private cargarTodo(): void {
    this.cargando = true;
    forkJoin({
      billeteras: this.billeteraService.listar(),
      categorias: this.categoriaService.listar(),
      transacciones: this.transaccionService.listar(),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ billeteras, categorias, transacciones }) => {
          this.billeteras = billeteras;
          this.categorias = categorias;
          this.transacciones = transacciones;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          this.cargando = false;
          this.error = mensajeErrorTransaccion(err);
        },
      });
  }
}
