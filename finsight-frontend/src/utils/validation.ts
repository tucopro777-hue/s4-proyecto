import type { LoginFormErrors, LoginFormValues } from '@/types';

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
const MIN_PASSWORD_LENGTH = 6;

export function validateLoginForm(values: LoginFormValues): LoginFormErrors {
  const errors: LoginFormErrors = {};

  if (!values.correo.trim()) {
    errors.correo = 'Ingresa tu correo electrónico.';
  } else if (!EMAIL_REGEX.test(values.correo.trim())) {
    errors.correo = 'Ingresa un correo electrónico válido.';
  }

  if (!values.contrasena) {
    errors.contrasena = 'Ingresa tu contraseña.';
  } else if (values.contrasena.length < MIN_PASSWORD_LENGTH) {
    errors.contrasena = `Debe tener al menos ${MIN_PASSWORD_LENGTH} caracteres.`;
  }

  return errors;
}

export function formatearMoneda(monto: number): string {
  return new Intl.NumberFormat('es-BO', {
    style: 'currency',
    currency: 'BOB',
    minimumFractionDigits: 2,
  }).format(monto);
}

export function formatearFecha(fechaIso: string): string {
  const fecha = new Date(fechaIso);
  return new Intl.DateTimeFormat('es-BO', {
    day: '2-digit',
    month: 'short',
  }).format(fecha);
}
