import { Component, DestroyRef, OnInit, inject } from '@angular/core';
import { DecimalPipe, NgClass } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { forkJoin } from 'rxjs';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { Categoria, CategoriaService } from '../categorias/categoria.service';
import { Presupuesto, PresupuestoService, mensajeErrorPresupuesto } from './presupuesto.service';

@Component({
  selector: 'app-presupuestos',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent, DecimalPipe, NgClass],
  templateUrl: './presupuestos.component.html',
  styleUrl: './presupuestos.component.scss',
})
export class PresupuestosComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly presupuestoService = inject(PresupuestoService);
  private readonly categoriaService = inject(CategoriaService);
  private readonly destroyRef = inject(DestroyRef);

  readonly meses = [
    'Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio',
    'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre',
  ];
  readonly anios = [2025, 2026, 2027];

  presupuestos: Presupuesto[] = [];
  categoriasGasto: Categoria[] = [];
  cargando = true;
  guardando = false;
  editandoId: number | null = null;
  montoEditado: number | null = null;
  eliminandoId: number | null = null;
  error: string | null = null;
  exito: string | null = null;

  readonly periodo = this.fb.nonNullable.group({
    anio: [new Date().getFullYear()],
    mes: [new Date().getMonth() + 1],
  });

  readonly form = this.fb.nonNullable.group({
    categoriaId: [null as number | null, [Validators.required]],
    montoLimite: [null as number | null, [Validators.required, Validators.min(0.01)]],
  });

  constructor() {
    // Cambiar el mes o el año recarga los presupuestos de ese periodo
    this.periodo.valueChanges
      .pipe(takeUntilDestroyed())
      .subscribe(() => this.cargarPresupuestos());
  }

  ngOnInit(): void {
    this.cargarDatos();
  }

  /** Categorías de gasto que aún no tienen presupuesto en el periodo elegido. */
  get categoriasDisponibles(): Categoria[] {
    const usadas = new Set(this.presupuestos.map((p) => p.categoriaId));
    return this.categoriasGasto.filter((c) => !usadas.has(c.id));
  }

  get totalLimite(): number {
    return this.presupuestos.reduce((suma, p) => suma + p.montoLimite, 0);
  }

  get totalConsumido(): number {
    return this.presupuestos.reduce((suma, p) => suma + p.saldoConsumido, 0);
  }

  /** Ancho de la barra de progreso, limitado a 100 %. */
  porcentaje(p: Presupuesto): number {
    if (p.montoLimite <= 0) {
      return 0;
    }
    return Math.min(100, (p.saldoConsumido / p.montoLimite) * 100);
  }

  crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    const { anio, mes } = this.periodo.getRawValue();
    this.guardando = true;
    this.error = null;
    this.exito = null;

    this.presupuestoService
      .crear({
        categoriaId: Number(valores.categoriaId),
        montoLimite: Number(valores.montoLimite),
        anio,
        mes,
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Presupuesto creado.';
          this.form.reset({ categoriaId: null, montoLimite: null });
          this.cargarPresupuestos();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorPresupuesto(err);
        },
      });
  }

  iniciarEdicion(p: Presupuesto): void {
    this.editandoId = p.id;
    this.montoEditado = p.montoLimite;
    this.error = null;
    this.exito = null;
  }

  cancelarEdicion(): void {
    this.editandoId = null;
    this.montoEditado = null;
  }

  guardarEdicion(p: Presupuesto): void {
    if (this.montoEditado === null || Number(this.montoEditado) <= 0) {
      this.error = 'El límite debe ser mayor que 0.';
      return;
    }

    this.presupuestoService.actualizar(p.id, Number(this.montoEditado)).subscribe({
      next: () => {
        this.cancelarEdicion();
        this.exito = 'Límite actualizado.';
        this.cargarPresupuestos();
      },
      error: (err: HttpErrorResponse) => {
        this.error = mensajeErrorPresupuesto(err);
      },
    });
  }

  eliminar(p: Presupuesto): void {
    const confirmado = window.confirm(`¿Eliminar el presupuesto de "${p.categoriaNombre}"?`);
    if (!confirmado) {
      return;
    }

    this.eliminandoId = p.id;
    this.error = null;
    this.exito = null;

    this.presupuestoService.eliminar(p.id).subscribe({
      next: () => {
        this.eliminandoId = null;
        this.presupuestos = this.presupuestos.filter((x) => x.id !== p.id);
      },
      error: (err: HttpErrorResponse) => {
        this.eliminandoId = null;
        this.error = mensajeErrorPresupuesto(err);
      },
    });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  private cargarDatos(): void {
    forkJoin({
      categorias: this.categoriaService.listar('GASTO'),
      presupuestos: this.presupuestoService.listar(
        this.periodo.controls.anio.value,
        this.periodo.controls.mes.value,
      ),
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: ({ categorias, presupuestos }) => {
          this.categoriasGasto = categorias;
          this.presupuestos = presupuestos;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          this.cargando = false;
          this.error = mensajeErrorPresupuesto(err);
        },
      });
  }

  private cargarPresupuestos(): void {
    this.cargando = true;
    this.presupuestoService
      .listar(this.periodo.controls.anio.value, this.periodo.controls.mes.value)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (presupuestos) => {
          this.presupuestos = presupuestos;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          this.cargando = false;
          this.error = mensajeErrorPresupuesto(err);
        },
      });
  }
}
