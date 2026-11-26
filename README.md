# FINDEX — Control de finanzas personales

Aplicación Angular 17 y Spring Boot con PostgreSQL, cadenas de Markov y comparación mediante Valor Monetario Esperado (VME).

## Ejecución

Requiere Java 17, Maven, Node 20 y PostgreSQL 16 con una base llamada `finanzas` en el puerto 5432. Configura `DB_USER` y `DB_PASSWORD` si tu instalación usa credenciales diferentes.

- Backend: desde `backend/`, ejecuta `mvn spring-boot:run`. Escucha en http://localhost:8080.
- Frontend: desde `frontend/`, ejecuta `npm ci` y `npm start`. Abre http://localhost:4200.
- Windows: `INICIAR.cmd` utiliza las herramientas instaladas en PATH o las copias opcionales en `.tools/`.

Flyway crea el esquema al arrancar. `PREPARAR_DEMO.cmd` prepara datos locales con Python y PostgreSQL 16; requiere ambos servidores activos. No es necesario ejecutarlo en cada inicio.

## Código y validación

- Markov: `backend/src/main/java/com/finanzas/service/MarkovService.java`.
- VME y comentarios del cálculo: `backend/src/main/java/com/finanzas/service/VmeService.java`.
- Pantallas: `frontend/src/app/features/`.
- Backend: `mvn test` desde `backend/`.
- Frontend: `npm run build` y `npm test -- --watch=false --karma-config=karma.conf.cjs` desde `frontend/`; las pruebas requieren Chrome.

La documentación de la exposición está en `docs/` y el registro histórico en `AVANCE.md`. Las rutas históricas con `s4-proyecto/` se refieren a la raíz de este repositorio.

## Nombres de movimientos y metas

La eliminación de la palabra DEMO de los registros existentes se realizó en PostgreSQL local. El script opcional `inicio/quitar-demo-nombres.sql` reproduce ese ajuste únicamente para la cuenta de ejemplo, sin modificar montos, fechas ni saldos. Se ejecuta con `psql -d finanzas -v ON_ERROR_STOP=1 -f inicio/quitar-demo-nombres.sql` usando tus parámetros de conexión.

La base de datos local, las dependencias y los registros de ejecución no forman parte del repositorio.
