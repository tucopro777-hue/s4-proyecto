import { Component, inject } from '@angular/core';
import { HttpErrorResponse } from '@angular/common/http';
import { AbstractControl, FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { AuthService, mensajeErrorAuth } from '../../../core/auth/auth.service';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './login.component.html',
  styleUrl: './login.component.scss',
})
export class LoginComponent {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  cargando = false;
  error: string | null = null;
  exito: string | null = null;

  readonly form = this.fb.nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required]],
  });

  enviar(): void {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }

    this.cargando = true;
    this.error = null;

    this.auth.login(this.form.getRawValue()).subscribe({
      next: () => {
        this.exito = 'Sesión iniciada correctamente. Redirigiendo...';
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
