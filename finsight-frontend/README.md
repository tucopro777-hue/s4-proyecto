# FinSight — Frontend (React + TypeScript)

Frontend mobile-first para la aplicación de gestión de finanzas personales FinSight, pensado para conectarse posteriormente a un backend en **Java Spring Boot**.

## Stack

- React 18 + TypeScript
- Vite (bundler y dev server)
- Tailwind CSS (paleta oscura de diseño FinSight ya configurada)
- Lucide React (iconografía)
- React Router DOM (instalado, listo para rutas adicionales)

## Cómo ejecutar

```bash
npm install
npm run dev
```

Abre `http://localhost:5173` — el layout está optimizado primero para el viewport de celular; usa las DevTools en modo responsive (iPhone/Android) para la mejor vista.

## Estructura del proyecto

```
src/
├── components/
│   ├── auth/          # Pantalla de login
│   ├── dashboard/      # Header, resumen, acciones rápidas, movimientos
│   └── common/          # Navegación inferior y piezas compartidas
├── hooks/
│   └── useAuth.ts       # Estado de sesión (login/logout)
├── services/
│   ├── authService.ts    # Login simulado + JWT simulado en localStorage
│   └── financeService.ts # Datos financieros de ejemplo (mock)
├── types/
│   └── index.ts          # Tipos de dominio (User, Movimiento, etc.)
└── utils/
    ├── validation.ts      # Validaciones de formulario y formateo
    └── categoryIcons.ts   # Mapeo de categorías a íconos/colores
```

## Estado actual (v0)

- **Login funcional simulado**: valida correo/contraseña en el cliente, genera un token JWT simulado y lo guarda en `localStorage` (clave `finsight_session`), con expiración de 8 horas.
- **Dashboard**: saludo + avatar, tarjeta de saldo/ingresos/gastos, botones de acción rápida y lista de últimos movimientos — todo con datos de ejemplo (`financeService.ts`).

## Próximos pasos (conexión a Spring Boot)

1. Reemplazar `authService.login()` por `POST /api/auth/login` real, y guardar el JWT que devuelva el backend.
2. Reemplazar `financeService` por llamadas a los endpoints REST correspondientes (`/api/resumen`, `/api/movimientos`, etc.).
3. Agregar un cliente HTTP centralizado (`fetch` o `axios`) con interceptor para adjuntar el token en cada request.
4. Implementar los formularios/modales de "Registrar gasto" y "Registrar ingreso" (actualmente son placeholders con `TODO`).
