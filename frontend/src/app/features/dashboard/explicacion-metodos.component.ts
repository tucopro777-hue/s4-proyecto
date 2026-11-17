import { Component } from '@angular/core';
import { DecimalPipe } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { NavegacionComponent } from '../../shared/components/navegacion/navegacion.component';
import { AnalisisFinancieroComponent } from './analisis-financiero.component';

/** Comparte la consulta y validación existentes; toda la configuración queda en esta ruta. */
@Component({
  standalone: true,
  imports: [DecimalPipe, FormsModule, RouterLink, NavegacionComponent],
  templateUrl: './explicacion-metodos.component.html',
  styleUrl: './analisis-financiero.component.scss',
})
export class ExplicacionMetodosComponent extends AnalisisFinancieroComponent {}
