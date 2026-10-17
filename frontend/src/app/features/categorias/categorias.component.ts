import { Component, OnInit, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import {
  Categoria,
  CategoriaService,
  ICONOS_CATEGORIA,
  TipoCategoria,
  mensajeErrorCategoria,
} from './categoria.service';

/** Rechaza nombres vacíos o formados solo por espacios. */
function nombreNoVacio(control: AbstractControl): ValidationErrors | null {
  const valor: string = control.value ?? '';
  return valor.trim() ? null : { vacio: true };
}

@Component({
  selector: 'app-categorias',
  standalone: true,
  imports: [ReactiveFormsModule, NavegacionComponent],
  templateUrl: './categorias.component.html',
  styleUrl: './categorias.component.scss',
})
export class CategoriasComponent implements OnInit {
  private readonly fb = inject(FormBuilder);
  private readonly categoriaService = inject(CategoriaService);

  categorias: Categoria[] = [];
  cargando = true;
  guardando = false;
  desactivandoId: number | null = null;
  error: string | null = null;
  exito: string | null = null;

  readonly tipos: { valor: TipoCategoria; etiqueta: string }[] = [
    { valor: 'GASTO', etiqueta: 'Gasto' },
    { valor: 'INGRESO', etiqueta: 'Ingreso' },
  ];
  readonly iconos = ICONOS_CATEGORIA;

  private readonly valoresPorDefecto = {
    nombre: '',
    tipo: 'GASTO' as TipoCategoria,
    color: '#f0b90b',
    icono: 'cart',
  };

  readonly form = this.fb.nonNullable.group({
    nombre: ['', [Validators.required, Validators.maxLength(50), nombreNoVacio]],
    tipo: ['GASTO' as TipoCategoria, [Validators.required]],
    color: ['#f0b90b', [Validators.required, Validators.pattern(/^#[0-9a-fA-F]{6}$/)]],
    icono: ['cart', [Validators.required]],
  });

  get gastos(): Categoria[] {
    return this.categorias.filter((c) => c.tipo === 'GASTO');
  }

  get ingresos(): Categoria[] {
    return this.categorias.filter((c) => c.tipo === 'INGRESO');
  }

  ngOnInit(): void {
    this.cargarCategorias();
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

    this.categoriaService
      .crear({ ...valores, nombre: valores.nombre.trim(), color: valores.color.toUpperCase() })
      .subscribe({
        next: () => {
          this.guardando = false;
          this.exito = 'Categoría creada correctamente.';
          this.form.reset(this.valoresPorDefecto);
          this.cargarCategorias();
        },
        error: (err: HttpErrorResponse) => {
          this.guardando = false;
          this.error = mensajeErrorCategoria(err);
        },
      });
  }

  desactivar(categoria: Categoria): void {
    const confirmado = window.confirm(
      `¿Desactivar la categoría "${categoria.nombre}"? Dejará de aparecer al registrar movimientos.`,
    );
    if (!confirmado) {
      return;
    }

    this.desactivandoId = categoria.id;
    this.error = null;
    this.exito = null;

    this.categoriaService.desactivar(categoria.id).subscribe({
      next: () => {
        this.desactivandoId = null;
        // Se quita de la lista local para que la vista se actualice al instante
        this.categorias = this.categorias.filter((c) => c.id !== categoria.id);
      },
      error: (err: HttpErrorResponse) => {
        this.desactivandoId = null;
        this.error = mensajeErrorCategoria(err);
      },
    });
  }

  simboloIcono(clave: string): string {
    return this.iconos.find((i) => i.clave === clave)?.simbolo ?? '🏷️';
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }

  private cargarCategorias(): void {
    this.cargando = true;
    this.categoriaService.listar().subscribe({
      next: (lista) => {
        this.categorias = lista;
        this.cargando = false;
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.error = mensajeErrorCategoria(err);
      },
    });
  }
}
