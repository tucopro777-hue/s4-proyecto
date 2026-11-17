import { Component, OnInit, inject } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { EscenariosFinancierosStore } from './escenarios-financieros.store';
import { AuthService } from '../../core/auth/auth.service';
import { HttpErrorResponse } from '@angular/common/http';
import { AnalisisFinancieroService, Alternativa, Markov, Vme } from './analisis-financiero.service';
import { mensajeErrorDashboard } from './dashboard.service';
import { forkJoin } from 'rxjs';

@Component({
  selector: 'app-analisis-financiero', standalone: true, imports: [DecimalPipe, FormsModule, RouterLink],
  templateUrl: './analisis-financiero.component.html', styleUrl: './analisis-financiero.component.scss',
})
export class AnalisisFinancieroComponent implements OnInit {
  private service = inject(AnalisisFinancieroService);
  private auth = inject(AuthService);
  private escenarios = inject(EscenariosFinancierosStore);
  divisas: string[] = []; divisa = ''; markov: Markov | null = null; vme: Vme | null = null;
  disponible: number | null = null;
  error = ''; cargando = false; evaluando = false;
  montoEvaluar: number | null = null;
  avanzadoAbierto = false;
  escenariosDeEjemplo = false;
  alternativas: Alternativa[] = ['Mantener disponible', 'Destinar al ahorro', 'Destinar a inversión'].map(nombre =>
    ({ nombre, asignacion: nombre === 'Mantener disponible' ? 0 : null, resultadoEstable: null, resultadoDeficit: null }));
  private version = 0;
  private versionCarga = 0;
  private divisaCargada = '';

  // Solo etiqueta de presentación; la autenticación se valida siempre en Java.
  get cuentaDemo(): boolean {
    try {
      const payload = this.auth.obtenerToken()?.split('.')[1];
      return !!payload && JSON.parse(atob(payload.replace(/-/g, '+').replace(/_/g, '/'))).sub === 'demo@finanzas.test';
    } catch { return false; }
  }

  ngOnInit() {
    this.cargando = true;
    this.service.divisas().subscribe({ next: divisas => {
      this.divisas = divisas;
      const preferida = this.escenarios.preferida(this.auth.obtenerToken());
      this.divisa = preferida && divisas.includes(preferida) ? preferida : divisas[0] ?? '';
      if (this.divisa) this.cargar(); else this.cargando = false;
    }, error: (e: HttpErrorResponse) => { this.error = mensajeErrorDashboard(e); this.cargando = false; } });
  }

  cargar() {
    if (this.divisa !== this.divisaCargada) {
      this.escenariosDeEjemplo = false;
      this.montoEvaluar = null;
      this.alternativas.forEach(a => { a.resultadoEstable = null; a.resultadoDeficit = null; a.asignacion = null; });
      this.alternativas[0].asignacion = 0;
      this.divisaCargada = this.divisa;
      const configurados = this.escenarios.leer(this.auth.obtenerToken(), this.divisa);
      if (configurados) {
        this.montoEvaluar = configurados.monto;
        this.alternativas = configurados.alternativas;
        this.escenariosDeEjemplo = configurados.ejemplo;
      }
    }
    const version = ++this.versionCarga;
    ++this.version;
    this.markov = null; this.vme = null; this.disponible = null; this.error = ''; this.cargando = true; this.evaluando = false;
    forkJoin({ markov: this.service.markov(this.divisa), disponible: this.service.disponible(this.divisa) }).subscribe({ next: datos => {
      if (version !== this.versionCarga) return;
      this.markov = datos.markov; this.disponible = datos.disponible; this.cargando = false;
    }, error: (e: HttpErrorResponse) => {
      if (version !== this.versionCarga) return;
      this.error = mensajeErrorDashboard(e); this.cargando = false;
    } });
  }

  invalidar() { this.vme = null; this.error = ''; ++this.version; this.evaluando = false; }

  cargarEjemplo() {
    if (this.cargando || this.disponible === null || this.disponible <= 0) return;
    this.invalidar();
    this.escenariosDeEjemplo = true;
    // Montos didácticos absolutos, sin tasas ni probabilidades de demostración.
    this.montoEvaluar = Math.min(500, this.disponible);
    const cambios = [[0, 0], [20, -10], [100, -120]];
    this.alternativas.forEach((a, i) => {
      a.resultadoEstable = cambios[i][0]; a.resultadoDeficit = cambios[i][1];
    });
    if (this.markov?.probabilidades) this.evaluar();
  }

  cargarCasoDemo(caso: 'ahorro' | 'inversion') {
    if (this.cargando || this.disponible === null || this.disponible <= 0) return;
    this.invalidar(); this.escenariosDeEjemplo = true;
    this.montoEvaluar = Math.min(500, this.disponible);
    const cambios = caso === 'ahorro' ? [[0,0],[60,10],[20,-200]] : [[0,0],[10,0],[200,-20]];
    this.alternativas.forEach((a,i) => { a.resultadoEstable=cambios[i][0];a.resultadoDeficit=cambios[i][1]; });
    if (this.markov?.probabilidades) this.evaluar();
  }

  get maximoHistorial(): number {
    return Math.max(1,...(this.markov?.historial.flatMap(m => [m.ingresos,m.gastos]) ?? []));
  }

  dineroValido(n: number | null): n is number {
    return n !== null && Number.isFinite(n) && Math.abs(n) < 1e12
      && Math.abs(n * 100 - Math.round(n * 100)) < 0.00001;
  }

  get montoValido() {
    return this.dineroValido(this.montoEvaluar) && this.montoEvaluar > 0
      && this.disponible !== null && this.montoEvaluar <= this.disponible;
  }

  /** Reserva derivada de la elección del usuario, sin inferir obligaciones futuras. */
  get reservaMinima(): number {
    return this.montoValido ? Math.round((this.disponible! - this.montoEvaluar!) * 100) / 100 : 0;
  }

  get filaActual() { return this.markov?.matriz.find(f => f.origen === this.markov?.estadoBase); }

  /** Etiquetas de pantalla; no cambia los nombres enviados al cálculo existente. */
  nombreOpcion(nombre: string): string {
    return ({ 'Mantener disponible': 'Guardar', 'Destinar al ahorro': 'Ahorrar',
      'Destinar a inversión': 'Invertir', 'Guardar mi dinero': 'Guardar' } as Record<string, string>)[nombre] ?? nombre;
  }

  mesLegible(mes: string): string {
    const [anio, numero] = mes.split('-').map(Number);
    return new Intl.DateTimeFormat('es', { month: 'long', year: 'numeric' }).format(new Date(anio, numero - 1, 1));
  }

  get consejo(): string {
    const p = this.markov?.probabilidades;
    if (!p) return '';
    if (p.deficit > p.estable) return 'Revisa tus gastos y conserva un margen para imprevistos.';
    if (p.estable > p.deficit) return 'Mantén tus gastos controlados y reserva dinero para tus próximos pagos.';
    return 'Los dos escenarios son igual de probables; conserva un margen antes de decidir.';
  }

  get explicacionRecomendacion(): string {
    if (!this.vme) return '';
    if (!this.vme.markov.probabilidades) return 'Falta historial para comparar.';
    if (!this.vme.recomendadas.length) return 'Ninguna opción conserva la reserva con los escenarios configurados.';
    if (this.vme.recomendadas.length > 1) return 'Hay empate entre opciones viables; el resultado esperado no es una ganancia garantizada.';
    return 'Tiene el mayor saldo final esperado entre las opciones viables; no es una ganancia garantizada.';
  }

  get valido() {
    return this.montoValido && this.alternativas.every(a =>
      this.dineroValido(a.resultadoEstable) && this.dineroValido(a.resultadoDeficit));
  }

  get motivoComparacion(): string {
    if (this.cargando) return 'Cargando tus datos…';
    if (this.disponible === null || this.disponible <= 0) return 'Necesitas saldo disponible en esta moneda.';
    if (!this.dineroValido(this.montoEvaluar) || this.montoEvaluar <= 0) return 'Indica cuánto dinero quieres evaluar, con hasta dos decimales.';
    if (this.montoEvaluar > this.disponible) return 'El monto a evaluar no puede superar tu saldo.';
    if (!this.markov?.probabilidades) return 'Necesitamos más historial en ' + this.divisa + ' para comparar. Puedes revisar otra moneda.';
    const faltantes = this.alternativas.reduce((n, a) => n + Number(a.resultadoEstable === null) + Number(a.resultadoDeficit === null), 0);
    if (faltantes) return 'Completa los ' + faltantes + ' importes pendientes. Escribe 0 si no esperas ganar ni perder.';
    if (!this.valido) return 'Los importes deben ser números con hasta dos decimales.';
    return '';
  }

  get mejores() {
    return this.vme?.alternativas.filter(a => this.vme!.recomendadas.includes(a.nombre)) ?? [];
  }
  get mejoresNombres() { return this.mejores.map(a => this.nombreOpcion(a.nombre)); }

  evaluarMonto() {
    if (!this.montoValido || !this.markov?.probabilidades || this.evaluando || this.cargando) return;
    const version = ++this.version;
    this.evaluando = true; this.error = ''; this.vme = null;
    this.service.evaluarAutomaticamente(this.divisa, this.montoEvaluar!).subscribe({
      next: datos => {
        if (version !== this.version) return;
        this.vme = datos; this.markov = datos.markov; this.evaluando = false;
      },
      error: (e: HttpErrorResponse) => {
        if (version !== this.version) return;
        this.error = e.error?.message || mensajeErrorDashboard(e); this.evaluando = false;
      },
    });
  }

  evaluar() {
    if (!this.valido || !this.markov?.probabilidades || this.evaluando || this.cargando) return;
    this.escenarios.guardar(this.auth.obtenerToken(), this.divisa, {
      monto: this.montoEvaluar!, alternativas: this.alternativas, ejemplo: this.escenariosDeEjemplo,
    });
    const version = ++this.version;
    this.evaluando = true; this.error = ''; this.vme = null;
    const alternativas = this.alternativas.map((a, i) => ({ ...a, asignacion: i === 0 ? 0 : this.montoEvaluar }));
    this.service.evaluar({ divisa: this.divisa, reservaMinima: this.reservaMinima, alternativas }).subscribe({
      next: datos => { if (version !== this.version) return; this.vme = datos; this.markov = datos.markov; this.disponible = datos.disponible; this.evaluando = false; },
      error: (e: HttpErrorResponse) => { if (version !== this.version) return;
        this.error = typeof e.error === 'string' ? e.error : e.error?.message || mensajeErrorDashboard(e); this.evaluando = false; },
    });
  }
}
