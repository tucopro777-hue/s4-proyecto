import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe, PercentPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { Transaccion, TransaccionService } from '../transacciones/transaccion.service';

/** Paleta categórica (modo oscuro, validada contra la superficie de la app). Orden fijo, sin ciclar. */
const PALETA = ['#3987e5', '#d95926', '#199e70', '#c98500', '#d55181', '#0ca30c', '#9085e9', '#e66767'];
const COLOR_OTROS = '#848e9c';
const CENTRO = 100;
const RADIO = 90;

/** Un sector del gráfico de pastel con su leyenda. */
export interface Porcion {
  clave: string;
  nombre: string;
  color: string;
  total: number;
  porcentaje: number;
  ruta: string;
  esCirculoCompleto: boolean;
}

/** Un gráfico de pastel completo: título, monto total y sectores. */
export interface PanelEstadistico {
  titulo: string;
  total: number;
  porciones: Porcion[];
}

/** Categoría que aparece en los filtros. */
export interface CategoriaFiltro {
  id: number;
  nombre: string;
}

@Component({
  selector: 'app-estadisticas',
  standalone: true,
  imports: [NavegacionComponent, DecimalPipe, PercentPipe, FormsModule],
  templateUrl: './estadisticas.component.html',
  styleUrl: './estadisticas.component.scss',
})
export class EstadisticasComponent implements OnInit {
  private readonly transaccionService = inject(TransaccionService);

  cargando = true;
  error: string | null = null;

  /** Todos los movimientos del usuario, sin filtrar. */
  private movimientos: Transaccion[] = [];
  /** Categorías encontradas en los movimientos, en orden de id (define el color de cada una). */
  categorias: CategoriaFiltro[] = [];
  /** Ranura de color de cada categoría; las que superan la paleta se agrupan en "Otros". */
  private ranuras = new Map<number, number>();
  /** Categorías seleccionadas en el filtro. */
  seleccion = new Map<number, boolean>();

  /** Rango de fechas yyyy-MM-dd; vacío = sin límite. */
  desde = '';
  hasta = '';

  paneles: PanelEstadistico[] = [];

  ngOnInit(): void {
    this.desde = primerDiaDelMes();
    this.hasta = hoyIso();

    this.transaccionService.listar().subscribe({
      next: (movimientos) => {
        this.movimientos = movimientos;
        this.prepararCategorias();
        this.recalcular();
        this.cargando = false;
      },
      error: () => {
        this.error = 'No se pudieron cargar los movimientos. Inténtalo de nuevo más tarde.';
        this.cargando = false;
      },
    });
  }

  /** Vuelve a calcular los gráficos según el rango de fechas y las categorías marcadas. */
  recalcular(): void {
    const filtrados = this.movimientos.filter(
      (m) =>
        (!this.desde || m.fecha >= this.desde) &&
        (!this.hasta || m.fecha <= this.hasta) &&
        this.seleccion.get(m.categoriaId) === true,
    );

    const gastos = filtrados.filter((m) => m.tipo === 'GASTO');
    const ingresos = filtrados.filter((m) => m.tipo === 'INGRESO');

    this.paneles = [
      crearPanel('Gastos', gastos, (id) => this.ranuras.get(id), this.nombres()),
      crearPanel('Ingresos', ingresos, (id) => this.ranuras.get(id), this.nombres()),
      crearPanel('Por categorías', filtrados, (id) => this.ranuras.get(id), this.nombres()),
    ];
  }

  marcar(id: number, valor: boolean): void {
    this.seleccion.set(id, valor);
    this.recalcular();
  }

  marcarTodas(valor: boolean): void {
    for (const c of this.categorias) {
      this.seleccion.set(c.id, valor);
    }
    this.recalcular();
  }

  /** Quita el límite de fechas para ver todo el histórico. */
  todoElTiempo(): void {
    this.desde = '';
    this.hasta = '';
    this.recalcular();
  }

  /** Asigna a cada categoría una ranura fija según su id; el color no depende del filtro ni del ranking. */
  private prepararCategorias(): void {
    const mapa = new Map<number, string>();
    for (const m of this.movimientos) {
      mapa.set(m.categoriaId, m.categoriaNombre);
    }
    this.categorias = [...mapa.entries()]
      .map(([id, nombre]) => ({ id, nombre }))
      .sort((a, b) => a.id - b.id);

    this.categorias.forEach((c, indice) => {
      this.ranuras.set(c.id, indice);
      this.seleccion.set(c.id, true);
    });
  }

  private nombres(): Map<number, string> {
    return new Map(this.categorias.map((c) => [c.id, c.nombre]));
  }
}

/** Construye el pastel de un conjunto de movimientos. Importes en valor absoluto. */
function crearPanel(
  titulo: string,
  movimientos: Transaccion[],
  ranuraDe: (id: number) => number | undefined,
  nombres: Map<number, string>,
): PanelEstadistico {
  const porCategoria = new Map<number, number>();
  for (const m of movimientos) {
    porCategoria.set(m.categoriaId, (porCategoria.get(m.categoriaId) ?? 0) + Math.abs(m.monto));
  }

  const total = [...porCategoria.values()].reduce((suma, v) => suma + v, 0);
  const porciones: Porcion[] = [];
  let otros = 0;
  let cuantasOtros = 0;

  for (const [id, monto] of porCategoria) {
    const ranura = ranuraDe(id);
    if (ranura === undefined || ranura >= PALETA.length) {
      otros += monto;
      cuantasOtros++;
      continue;
    }
    porciones.push({
      clave: String(id),
      nombre: nombres.get(id) ?? 'Sin nombre',
      color: PALETA[ranura],
      total: monto,
      porcentaje: total > 0 ? monto / total : 0,
      ruta: '',
      esCirculoCompleto: false,
    });
  }
  if (otros > 0) {
    porciones.push({
      clave: 'otros',
      nombre: `Otros (${cuantasOtros})`,
      color: COLOR_OTROS,
      total: otros,
      porcentaje: total > 0 ? otros / total : 0,
      ruta: '',
      esCirculoCompleto: false,
    });
  }

  porciones.sort((a, b) => b.total - a.total);
  asignarRutas(porciones);

  return { titulo, total, porciones };
}

/** Calcula el trazado SVG de cada sector, empezando arriba y avanzando en sentido horario. */
function asignarRutas(porciones: Porcion[]): void {
  let angulo = -Math.PI / 2;
  for (const p of porciones) {
    if (p.porcentaje <= 0) {
      continue;
    }
    if (p.porcentaje >= 0.9999) {
      p.esCirculoCompleto = true;
      p.ruta = '';
      angulo += 2 * Math.PI;
      continue;
    }
    const inicio = angulo;
    const fin = angulo + p.porcentaje * 2 * Math.PI;
    const x1 = CENTRO + RADIO * Math.cos(inicio);
    const y1 = CENTRO + RADIO * Math.sin(inicio);
    const x2 = CENTRO + RADIO * Math.cos(fin);
    const y2 = CENTRO + RADIO * Math.sin(fin);
    const granArco = fin - inicio > Math.PI ? 1 : 0;
    p.ruta = `M ${CENTRO} ${CENTRO} L ${x1.toFixed(3)} ${y1.toFixed(3)} A ${RADIO} ${RADIO} 0 ${granArco} 1 ${x2.toFixed(3)} ${y2.toFixed(3)} Z`;
    angulo = fin;
  }
}

function hoyIso(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
}

function primerDiaDelMes(): string {
  const d = new Date();
  return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-01`;
}
