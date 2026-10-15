import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import {
  AbstractControl,
  FormBuilder,
  ReactiveFormsModule,
  ValidationErrors,
  Validators,
} from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, mensajeErrorAuth } from '../../../core/auth/auth.service';

/** Comprueba que la contraseña y su confirmación coincidan. */
function passwordsIguales(grupo: AbstractControl): ValidationErrors | null {
  const password = grupo.get('password')?.value;
  const confirmar = grupo.get('confirmar')?.value;
  return password && confirmar && password !== confirmar ? { noCoinciden: true } : null;
}

@Component({
  selector: 'app-registro',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './registro.component.html',
  styleUrl: './registro.component.scss',
})
export class RegistroComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  cargando = false;
  error: string | null = null;
  exito: string | null = null;

  readonly form = this.fb.nonNullable.group(
    {
      nombre: ['', [Validators.required, Validators.maxLength(100)]],
      email: ['', [Validators.required, Validators.email, Validators.maxLength(150)]],
      password: ['', [Validators.required, Validators.minLength(8), Validators.maxLength(72)]],
      confirmar: ['', [Validators.required]],
    },
    { validators: passwordsIguales },
  );

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    const { nombre, email, password } = this.form.getRawValue();

    this.auth.registro({ nombre, email, password }).subscribe({
      next: () => {
        this.exito = '¡Cuenta creada correctamente! Redirigiendo...';
        // Breve pausa para que el usuario vea la confirmación antes de cambiar de pantalla
        setTimeout(() => this.router.navigateByUrl('/dashboard'), 800);
      },
      error: (err: HttpErrorResponse) => {
        this.cargando = false;
        this.error = mensajeErrorAuth(err);
      },
    });
  }

  campoInvalido(control: AbstractControl): boolean {
    return control.invalid && control.touched;
  }
}
