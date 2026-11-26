# Avance de la sesión — 17 de noviembre de 2026

## Inicio del sistema

Se iniciaron y verificaron el frontend Angular en `http://localhost:4200` y el backend Spring Boot en `http://localhost:8080`. Se comprobó PostgreSQL local y el acceso a la cuenta demo. Los lanzadores existentes son `INICIAR.cmd`, `inicio/iniciar-backend.cmd` e `inicio/iniciar-frontend.cmd`.

## Restauración de «Tu futuro financiero»

Se comparó el historial de la cuenta demo con `inicio/demo-completa-reporte.json`. Había dos ingresos adicionales de Bs 5.000: movimiento 167 del 15 de marzo de 2026 y movimiento 166 del 17 de octubre de 2026.

Se guardó un respaldo y se anularon mediante la API de transacciones. La anulación lógica conserva auditoría y revierte los efectos sobre los saldos utilizando la lógica del sistema. Se verificó que los doce meses y las probabilidades coincidieran con el reporte original: estabilidad 33,3 % y déficit 66,7 %. Estos porcentajes describen aquella restauración; nuevos movimientos pueden cambiarlos.

Respaldo: `evidencias/reportes/respaldo-restauracion-futuro.json`.

## Movimientos que no aparecían

El frontend mostraba solo los primeros veinte registros sin navegación y los filtros podían ocultar un movimiento recién creado.

Se añadió paginación con Anterior y Siguiente. Al registrar un ingreso o gasto se limpian los filtros y se abre la página que contiene su identificador, manteniendo el orden cronológico recibido del servidor. Al filtrar se vuelve a la primera página; al anular se ajusta la página al número de registros disponibles.

## Error interno al consultar fechas

Se encontró en los registros de PostgreSQL el error de parámetro sin tipo. La consulta JPQL utilizaba condiciones como `:desde IS NULL OR fecha >= :desde`; los filtros opcionales vacíos provocaban un HTTP 500 al consultar el historial, incluso después de un registro correcto.

`TransaccionRepository.buscarConFiltros` ahora construye la consulta mediante JPA Specifications/Criteria. Solo añade los filtros presentes. Conserva el aislamiento por usuario, excluye las transacciones anuladas y ordena por fecha e identificador descendentes.

Se reinició el backend para aplicar esta corrección. Se verificaron consultas reales en PostgreSQL: listado sin filtros, rango mensual, solo ingreso, solo gasto, límite inicial y límite final por separado.

## Consulta de meses anteriores: interfaz final

Se dejó un solo selector Mes y se quitaron los campos Desde y Hasta de la pantalla para evitar duplicidad y fechas incompletas.

Uso:

1. Entrar en Movimientos.
2. Elegir mes y año.
3. Opcionalmente seleccionar categoría y tipo, Ingreso o Gasto.
4. Pulsar Buscar.
5. Pulsar Limpiar para restablecer todos los filtros y consultar todo el historial.

Buscar convierte el mes en un rango inclusivo desde su primer día hasta su último día, incluidos los años bisiestos. Sin mes se consulta todo el historial, respetando los demás filtros. Los rangos se mantienen como parámetros internos de la API; ya no se editan directamente en esta pantalla.

Al registrar varios movimientos se conserva la fecha elegida en el formulario. El éxito del registro indica que se muestra el historial sin filtros.

## Dónde se usan las cadenas de Markov

El cálculo está en `s4-proyecto/backend/src/main/java/com/finanzas/service/MarkovService.java`, método `calcular`.

1. Agrupa los movimientos por moneda y mes, ignorando anulaciones y fechas futuras.
2. Clasifica cada mes como ESTABLE cuando ingresos >= gastos, o DEFICIT cuando gastos > ingresos.
3. Construye una matriz de frecuencias de cambios entre meses cerrados consecutivos. Un mes sin registros no se une artificialmente con el siguiente disponible.
4. Divide las frecuencias de cada fila por su total para obtener probabilidades de transición.
5. Utiliza el estado provisional del mes actual para elegir la fila que estima el próximo mes.

Ejemplo de una fila: si desde ESTABLE hay 2 cambios hacia ESTABLE y 4 hacia DEFICIT, las probabilidades son 2/6 y 4/6: aproximadamente 33,3 % y 66,7 %.

Este es un modelo de primer orden: la predicción usa el estado actual y las transiciones observadas. No introduce probabilidades de respaldo cuando faltan datos.

La conexión con la base de datos está en `AnalisisFinancieroService.predecir`: consulta los movimientos del usuario y llama a `markov.calcular`.

La API `GET /api/dashboard/analisis/markov?divisa=BOB` está en `AnalisisFinancieroController`. El frontend obtiene su respuesta en `AnalisisFinancieroComponent.cargar` y muestra los porcentajes en la tarjeta «Tu futuro financiero» de `analisis-financiero.component.html`.

## Dónde se usa el VME

El cálculo está en `s4-proyecto/backend/src/main/java/com/finanzas/service/VmeService.java`, método `calcular`.

Para cada alternativa calcula los saldos finales posibles y aplica:

```text
VME = P(ESTABLE) × saldo final estable
    + P(DEFICIT) × saldo final con déficit
```

La instrucción principal del código es:

```java
BigDecimal vme = p == null ? null
    : p.estable().multiply(estable).add(p.deficit().multiply(deficit));
```

Después filtra las alternativas viables según saldo y reserva, busca el mayor VME y devuelve todas las que empatan. Selecciona antes de redondear; los resultados mostrados tienen dos decimales.

La pantalla actual utiliza `AnalisisFinancieroService.evaluarAutomaticamente`: recibe moneda y monto y prepara las alternativas Guardar, Ahorrar e Invertir. Llama a Markov y entrega sus probabilidades al VME. Los resultados monetarios de estos escenarios son ficticios: Guardar tiene cambio cero; Ahorrar usa +4 % o -2 %; Invertir usa +20 % o -24 %. Son coeficientes didácticos, no tasas reales.

La petición es `POST /api/dashboard/analisis/vme/automatico`. Desde Angular, `AnalisisFinancieroComponent.evaluarMonto` la ejecuta al pulsar Evaluar en «¿Qué hacer con tu dinero?».

Existe también `POST /api/dashboard/analisis/vme` para escenarios configurados, que reutiliza el mismo comparador.

El VME representa el saldo final esperado, no la ganancia garantizada. No mueve dinero. Sin probabilidades suficientes de Markov no se calcula una recomendación.

Flujo completo:

```text
Transacciones → Totales mensuales → Estados → Matriz de Markov
                                              ↓
                                       Probabilidades
                                              ↓
Monto + escenarios simulados → VME → Comparación en el panel
```

## Archivos modificados para movimientos y fechas

- `s4-proyecto/backend/src/main/java/com/finanzas/repository/TransaccionRepository.java`.
- `s4-proyecto/backend/src/test/java/com/finanzas/service/SistemaCompletoIntegrationTest.java`.
- `s4-proyecto/frontend/src/app/features/transacciones/transacciones.component.ts`.
- `s4-proyecto/frontend/src/app/features/transacciones/transacciones.component.html`.
- `s4-proyecto/frontend/src/app/features/revision-sistema.spec.ts`.

## Validación realizada

- Compilación Angular de producción correcta tras la interfaz final.
- Nueve pruebas Angular de regresión aprobadas en la última ejecución.
- Nueve pruebas de backend aprobadas en la ejecución inicial de los grupos TransaccionServiceTest y SistemaCompletoIntegrationTest.
- Una prueba de integración adicional aprobada: registra ingresos y gastos en meses anteriores y consulta diferentes combinaciones de filtros opcionales. Se ejecuta con H2 y revierte sus datos.
- Consultas reales de solo lectura en PostgreSQL verificadas sin HTTP 500; las fechas de los resultados de marzo pertenecían al rango solicitado.
- Frontend disponible con HTTP 200 tras los cambios.

Evidencias en `evidencias/logs/`: `fechas-tests-java.log`, `fechas-regresion-java.log`, `fechas-tests-angular.log`, `fechas-build.log`, `mes-unico-tests.log` y `mes-unico-build.log`.

## Organización de carpetas

Se separó documentación en `docs/` y material histórico en `evidencias/logs/`, `evidencias/capturas/` y `evidencias/reportes/`. Se actualizaron las rutas de salida de capturas y del reporte del navegador en los scripts de `.tools/revision/`.

Se conservaron los scripts, el estado de la demo y los reportes que usa su preparación en `inicio/`. Los logs abiertos por servidores en ejecución permanecen allí. No se movieron las fuentes del backend o frontend, sus dependencias ni las carpetas de compilación.

El registro de los 81 archivos reubicados está en `evidencias/organizacion.json`. Las referencias históricas de AVANCE.md a capturas o logs en inicio/ deben consultarse ahora mediante ese manifiesto o en evidencias/. La guía de presentación pasó a `docs/PRESENTACION_MARKOV_VME.md`.
