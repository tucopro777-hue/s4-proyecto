import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe, DatePipe } from '@angular/common';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { MetaAhorro, MetaAhorroService, mensajeErrorMeta } from './meta.service';

/** Rechaza nombres vacíos o formados solo por espacios. */
function nombreNoVacio(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value ?? '';
  return valor.trim() ? null : { vacio: true };
}

@Component({
  selector: 'app-metas',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent, DecimalPipe, DatePipe],
  templateUrl: './metas.component.html',
  styleUrl: './metas.component.scss',
})
export class MetasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly metaService = inject(MetaAhorroService);

  metas: MetaAhorro[] = [];
  cargando = true;
  guardando = false;
  aportandoId: number | null = null;
  eliminandoId: number | null = null;
  editandoId: number | null = null;
  guardandoEdicion = false;
  error: string | null = null;
  exito: string | null = null;

  /** Importe por meta antes de confirmar un aporte, indexado por id. */
  montosAporte: Record<number, number | null> = {};

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100), nombreNoVacio]],
    montoObjetivo: [null as number | null, [Validators.required, Validators.min(0.01)]],
    fechaLimite: [''],
  });

  /** Formulario de edición, reutilizado por la meta que se esté editando. */
  readonly formEdicion = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(100), nombreNoVacio]],
    montoObjetivo: [null as number | null, [Validators.required, Validators.min(0.01)]],
    fechaLimite: [''],
  });

  ngOnInit(): void {
    this.cargarMetas();
  }

  iniciarEdicion(meta: MetaAhorro): void {
    this.editandoId = meta.id;
    this.formEdicion.reset({
      nombre: meta.nombre,
      montoObjetivo: meta.montoObjetivo,
      fechaLimite: meta.fechaLimite ?? '',
    });
    this.error = null;
    this.exito = null;
  }

  cancelarEdicion(): void {
    this.editandoId = null;
  }

  guardarEdicion(meta: MetaAhorro): void {
    if (this.formEdicion.invalid) {
      this.formEdicion.markAllAsTouched();
      return;
    }

    const valores = this.formEdicion.getRawValue();
    this.guardandoEdicion = true;
    this.error = null;
    this.exito = null;

    this.metaService
      .actualizar(meta.id, {
        nombre: valores.nombre.trim(),
        montoObjetivo: Number(valores.montoObjetivo),
        fechaLimite: valores.fechaLimite || null,
      })
      .subscribe({
        next: (actualizada) => {
          this.guardandoEdicion = false;
          this.editandoId = null;
          this.exito = `Meta "${actualizada.nombre}" actualizada.`;
          this.metas = this.metas.map((m) => (m.id === actualizada.id ? actualizada : m));
        },
        error: (err: HttpErrorResponse) => {
          this.guardandoEdicion = false;
          this.error = mensajeErrorMeta(err);
        },
      });
  }

  campoEdicionInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  /** Ancho de la barra de progreso, limitado a 100 %. */
  porcentajeVisible(meta: MetaAhorro): number {
    return Math.min(100, meta.porcentaje);
  }

  crear(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    const valores = this.form.getRawValue();
    this.guardando = true;
    this.error = null;
    this.exito = null;

    this.metaService
      .crear({
        nombre: valores.nombre.trim(),
        montoObjetivo: Number(valores.montoObjetivo),
        fechaLimite: valores.fechaLimite || null,
      })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Meta creada correctamente.';
          this.form.reset({ nombre: '', montoObjetivo: null, fechaLimite: '' });
          this.cargarMetas();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorMeta(err);
        },
      });
  }

  aportar(meta: MetaAhorro): void {
    const monto = this.montosAporte[meta.id];
    if (monto === null || monto === undefined || Number(monto) <= 0) {
      this.error = 'El aporte debe ser mayor que 0.';
      return;
    }

    this.aportandoId = meta.id;
    this.error = null;
    this.exito = null;

    this.metaService.aportar(meta.id, Number(monto)).subscribe({
      next: (actualizada) => {
        this.aportandoId = null;
        this.montosAporte[meta.id] = null;
        this.exito = `Aporte registrado en "${actualizada.nombre}".`;
        // Se reemplaza la meta en la lista para que la barra se actualice al instante
        this.metas = this.metas.map((m) => (m.id === actualizada.id ? actualizada : m));
      },
      error: (err: HttpErrorResponse) => {
        this.aportandoId = null;
        this.error = mensajeErrorMeta(err);
      },
    });
  }

  eliminar(meta: MetaAhorro): void {
    const confirmado = window.confirm(`¿Eliminar la meta "${meta.nombre}"?`);
    if (!confirmado) {
      return;
    }

    this.eliminandoId = meta.id;
    this.error = null;
    this.exito = null;

    this.metaService.eliminar(meta.id).subscribe({
      next: () => {
        this.eliminandoId = null;
        this.metas = this.metas.filter((m) => m.id !== meta.id);
      },
      error: (err: HttpErrorResponse) => {
        this.eliminandoId = null;
        this.error = mensajeErrorMeta(err);
      },
    });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  private cargarMetas(): void {
    this.cargando = true;
    this.metaService.listar().subscribe({
      next: (metas) => {
        this.metas = metas;
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.error = mensajeErrorMeta(err);
      },
    });
  }
}
