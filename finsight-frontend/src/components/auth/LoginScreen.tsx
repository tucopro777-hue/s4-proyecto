import { FormEvent, useState } from 'react';
import { Eye, EyeOff, Lock, Mail, TrendingUp } from 'lucide-react';
import type { LoginFormErrors, LoginFormValues } from '@/types';
import { validateLoginForm } from '@/utils/validation';

interface LoginScreenProps {
  onLogin: (values: LoginFormValues) => Promise<boolean>;
  isLoading: boolean;
  serverError: string | null;
}

export default function LoginScreen({ onLogin, isLoading, serverError }: LoginScreenProps) {
  const [values, setValues] = useState<LoginFormValues>({ correo: '', contrasena: '' });
  const [errors, setErrors] = useState<LoginFormErrors>({});
  const [mostrarContrasena, setMostrarContrasena] = useState(false);
  const [touched, setTouched] = useState<Record<string, boolean>>({});

  const handleChange = (field: keyof LoginFormValues, value: string) => {
    setValues((prev) => ({ ...prev, [field]: value }));
    if (touched[field]) {
      setErrors(validateLoginForm({ ...values, [field]: value }));
    }
  };

  const handleBlur = (field: keyof LoginFormValues) => {
    setTouched((prev) => ({ ...prev, [field]: true }));
    setErrors(validateLoginForm(values));
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    const validationErrors = validateLoginForm(values);
    setErrors(validationErrors);
    setTouched({ correo: true, contrasena: true });

    if (Object.keys(validationErrors).length > 0) return;

    await onLogin(values);
  };

  return (
    <div className="min-h-dvh w-full flex flex-col justify-center px-6 py-10 bg-bg relative overflow-hidden">
      {/* Acento decorativo sutil */}
      <div
        className="absolute -top-24 -right-24 w-64 h-64 rounded-full opacity-[0.07] blur-3xl pointer-events-none"
        style={{ background: '#c8f064' }}
        aria-hidden="true"
      />

      <div className="w-full max-w-sm mx-auto relative z-10">
        {/* Logo / marca */}
        <div className="flex flex-col items-center mb-10">
          <div className="w-14 h-14 rounded-2xl bg-lime/10 border border-lime/20 flex items-center justify-center mb-4">
            <TrendingUp className="w-7 h-7 text-lime" strokeWidth={2} />
          </div>
          <h1 className="font-display text-3xl text-white tracking-tight">FinSight</h1>
          <p className="font-body text-muted text-sm mt-1">Tus finanzas, claras y bajo control</p>
        </div>

        <form onSubmit={handleSubmit} noValidate className="space-y-4">
          {/* Campo correo */}
          <div>
            <label htmlFor="correo" className="block font-body text-sm text-muted mb-1.5">
              Correo electrónico
            </label>
            <div className="relative">
              <Mail
                className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-muted"
                aria-hidden="true"
              />
              <input
                id="correo"
                type="email"
                inputMode="email"
                autoComplete="email"
                value={values.correo}
                onChange={(e) => handleChange('correo', e.target.value)}
                onBlur={() => handleBlur('correo')}
                placeholder="tucorreo@ejemplo.com"
                className={`w-full bg-card border rounded-xl pl-11 pr-4 py-3.5 text-white placeholder:text-muted/60 font-body text-[15px] transition-colors ${
                  errors.correo && touched.correo
                    ? 'border-red/60'
                    : 'border-border focus:border-lime/50'
                }`}
                aria-invalid={Boolean(errors.correo && touched.correo)}
                aria-describedby={errors.correo ? 'correo-error' : undefined}
              />
            </div>
            {errors.correo && touched.correo && (
              <p id="correo-error" className="mt-1.5 text-xs text-red font-body">
                {errors.correo}
              </p>
            )}
          </div>

          {/* Campo contraseña */}
          <div>
            <label htmlFor="contrasena" className="block font-body text-sm text-muted mb-1.5">
              Contraseña
            </label>
            <div className="relative">
              <Lock
                className="absolute left-3.5 top-1/2 -translate-y-1/2 w-4.5 h-4.5 text-muted"
                aria-hidden="true"
              />
              <input
                id="contrasena"
                type={mostrarContrasena ? 'text' : 'password'}
                autoComplete="current-password"
                value={values.contrasena}
                onChange={(e) => handleChange('contrasena', e.target.value)}
                onBlur={() => handleBlur('contrasena')}
                placeholder="••••••••"
                className={`w-full bg-card border rounded-xl pl-11 pr-11 py-3.5 text-white placeholder:text-muted/60 font-body text-[15px] transition-colors ${
                  errors.contrasena && touched.contrasena
                    ? 'border-red/60'
                    : 'border-border focus:border-lime/50'
                }`}
                aria-invalid={Boolean(errors.contrasena && touched.contrasena)}
                aria-describedby={errors.contrasena ? 'contrasena-error' : undefined}
              />
              <button
                type="button"
                onClick={() => setMostrarContrasena((v) => !v)}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted hover:text-white transition-colors"
                aria-label={mostrarContrasena ? 'Ocultar contraseña' : 'Mostrar contraseña'}
              >
                {mostrarContrasena ? (
                  <EyeOff className="w-4.5 h-4.5" />
                ) : (
                  <Eye className="w-4.5 h-4.5" />
                )}
              </button>
            </div>
            {errors.contrasena && touched.contrasena && (
              <p id="contrasena-error" className="mt-1.5 text-xs text-red font-body">
                {errors.contrasena}
              </p>
            )}
          </div>

          <div className="flex justify-end">
            <button
              type="button"
              className="font-body text-sm text-teal hover:text-teal/80 transition-colors"
            >
              ¿Olvidaste tu contraseña?
            </button>
          </div>

          {serverError && (
            <div
              role="alert"
              className="bg-red/10 border border-red/30 rounded-xl px-4 py-3 text-sm text-red font-body"
            >
              {serverError}
            </div>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-lime text-bg font-body font-semibold text-[15px] rounded-xl py-3.5 mt-2 transition-transform active:scale-[0.98] disabled:opacity-60 disabled:active:scale-100"
          >
            {isLoading ? 'Ingresando…' : 'Iniciar sesión'}
          </button>
        </form>

        <p className="text-center font-body text-sm text-muted mt-8">
          ¿No tienes cuenta?{' '}
          <button type="button" className="text-lime font-medium hover:text-lime/80 transition-colors">
            Regístrate
          </button>
        </p>
      </div>
    </div>
  );
}
