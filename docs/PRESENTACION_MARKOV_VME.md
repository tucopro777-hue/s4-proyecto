# FINDEX: guía para la exposición

Se reutilizaron MarkovService y VmeService. No se recrearon los métodos.

## Markov

Agrupa los ingresos y gastos del usuario por mes y moneda. Un mes es ESTABLE si ingresos >= gastos; en caso contrario es DEFICIT. Aprende contando cambios entre meses cerrados consecutivos. Omite huecos, movimientos anulados, futuros y otras monedas. No mezcla usuarios. El estado del mes actual es provisional y elige qué probabilidades usar para el siguiente mes; no se incorpora al entrenamiento hasta cerrar el mes.

Fórmula para explicar en diapositivas: P(destino | origen) = número de cambios origen→destino / total de cambios desde origen. Si no hay cambios desde el estado actual, o no hay movimientos actuales, no calcula una predicción. No rellena probabilidades.

## VME y su conexión

La nueva evaluación automática acepta únicamente moneda y monto. Consulta Markov en el servidor y reutiliza VmeService para ponderar los resultados de Guardar mi dinero, Ahorrar e Invertir. No acepta probabilidades del navegador ni mueve dinero.

FINDEX no tiene tasas verificadas ni resultados reales de productos de ahorro o inversión. Por eso esta comparación es siempre SIMULACIÓN, tanto en la cuenta demo como en otras cuentas. No se deducen rentabilidades del historial de ingresos y gastos. En una cuenta real las probabilidades provienen de sus movimientos; en demo provienen de movimientos ficticios.

Escenarios didácticos para monto M (coeficientes ficticios; NO son tasas reales):

| Alternativa | Monto final si ESTABLE | Monto final si DEFICIT |
|---|---:|---:|
| Guardar mi dinero | M | M |
| Ahorrar | 1,04 × M | 0,98 × M |
| Invertir | 1,20 × M | 0,76 × M |

Fórmula para diapositivas: VME = P(ESTABLE) × monto final estable + P(DEFICIT) × monto final déficit. El resultado es monto final esperado, NO ganancia. Se evalúa el monto introducido, no todo el saldo. El servidor valida el saldo y el monto; sin Markov no hay VME ni recomendación. La selección se hace antes de redondear los importes mostrados a dos decimales.

## Datos reutilizados y ejemplo que puedes reproducir

Cuenta local: demo@finanzas.test / Demo1234. Perfil demo-local; base local finanzas. Se reutilizaron los datos existentes: 48 ingresos y 100 gastos en total. El cargador existente no creó registros y verificó por huellas de las tablas que las demás cuentas no cambiaron.

Hay 12 meses cerrados consecutivos, noviembre de 2025 a octubre de 2026, con estados:

ESTABLE, DEFICIT, ESTABLE, ESTABLE, DEFICIT, ESTABLE, ESTABLE, DEFICIT, DEFICIT, ESTABLE, DEFICIT, ESTABLE.

De las 11 transiciones, 6 salen de ESTABLE: 2 a ESTABLE y 4 a DEFICIT. Las otras 5 salen de DEFICIT: 4 a ESTABLE y 1 a DEFICIT. Ambas filas son observables.

Ejemplo Markov: noviembre de 2026 tiene ingresos 5.100 BOB y gastos 3.075 BOB, por lo que es ESTABLE provisional. Para diciembre: P(ESTABLE)=2/6=33,33 % y P(DEFICIT)=4/6=66,67 %.

Ejemplo VME reproducible para 500 BOB y esas probabilidades:

| Alternativa | Estable | Déficit | VME aproximado |
|---|---:|---:|---:|
| Guardar mi dinero | 500 | 500 | 500,00 |
| Ahorrar | 520 | 490 | 500,00 |
| Invertir | 600 | 380 | 453,33 |

Guardar y Ahorrar empatan matemáticamente con probabilidades exactas 1/3 y 2/3. El algoritmo existente elige antes del redondeo; con las probabilidades almacenadas a 12 decimales, Guardar queda ligeramente por encima y aparece como recomendación.

Ejemplo adicional para mostrar el efecto de las probabilidades, con los mismos escenarios y monto de 1.000 BOB:

| P(ESTABLE) / P(DEFICIT) | Guardar | Ahorrar | Invertir | Mejor alternativa |
|---|---:|---:|---:|---|
| 80 % / 20 % | 1.000 | 1.028 | 1.112 | Invertir |
| 50 % / 50 % | 1.000 | 1.010 | 980 | Ahorrar |
| 10 % / 90 % | 1.000 | 986 | 804 | Guardar |

Estos tres casos se verifican en EvaluacionAutomaticaTest sin alterar movimientos ni probabilidades de usuarios. Las cifras son ficticias y no predicen rendimientos reales.

## Cómo verlo

1. Abre http://localhost:4200/ y entra con la cuenta demo.
2. En el Dashboard verás Tu futuro financiero y las probabilidades.
3. En ¿Qué hacer con tu dinero? introduce 500 y pulsa Evaluar.
4. Verás la mejor alternativa, monto final esperado y aviso SIMULACIÓN. Cambiar el monto borra el resultado anterior.

Las dos tarjetas conservan el diseño oscuro y no muestran matrices, fórmulas ni seis campos de resultados. Se conserva la implementación anterior y su ruta secundaria para compatibilidad, pero el Dashboard ya no depende de configurarla.

## Archivos de implementación y pruebas

- backend/src/main/java/com/finanzas/dto/EvaluacionAutomaticaRequest.java: validación de monto y moneda.
- backend/src/main/java/com/finanzas/service/AnalisisFinancieroService.java: escenarios ficticios y conexión al cálculo existente.
- backend/src/main/java/com/finanzas/controller/AnalisisFinancieroController.java: endpoint autenticado /vme/automatico.
- frontend/src/app/features/dashboard/analisis-financiero.service.ts: llamada automática.
- frontend/src/app/features/dashboard/analisis-financiero.component.ts y .html: botón, dos tarjetas y avisos.
- backend/src/test/java/com/finanzas/service/AnalisisFinancieroIntegrationTest.java y EvaluacionAutomaticaTest.java: validación, aislamiento, autenticación y cambio de ganadora.
- frontend/src/app/features/dashboard/analisis-financiero.component.spec.ts: interfaz y evaluación con un monto.
- frontend/karma.local.cjs: lanzador Chrome para el entorno Windows restringido.

Las rutas backend/ y frontend/ anteriores están bajo s4-proyecto/.

Se añadieron también .tools/revision/verificar-metodos.cjs para la comprobación del navegador y este documento. El cargador existente actualizó inicio/demo-completa-reporte.json como evidencia, sin crear datos.

## Verificación

- Maven package: 65 pruebas, 0 fallos, 0 errores; compilación y JAR correctos. Incluye Markov, VME, seguridad, transacciones, billeteras, presupuestos, metas y traspasos.
- Angular: 34 pruebas correctas en Chrome con karma.local.cjs. Incluyen clic en Evaluar, payload con solo monto/moneda, resultado, invalidación al editar y bloqueo sin historial.
- Angular build: compilación de producción correcta.
- Cargador demo existente: 12 meses, ninguna creación, otras cuentas sin cambios y billeteras cuadradas.
- Navegador real: acceso con demo, un único campo, clic en Evaluar con 500 BOB, resultado Guardar · 500,00 BOB y aviso SIMULACIÓN. Sin errores JavaScript ni respuestas HTTP fallidas. Captura: evidencias/capturas/metodos-dashboard.png.

Logs en evidencias/logs/verificacion-metodos-java.log, evidencias/logs/verificacion-metodos-angular.log, evidencias/logs/verificacion-metodos-build.log y evidencias/logs/verificacion-metodos-navegador.log.

## Revisión: por qué aparece Guardar al cambiar el monto

No se cambiaron los pagos ficticios ni el selector. Con M > 0 y p = probabilidad de estabilidad, los montos finales esperados son:

- Guardar: M.
- Ahorrar: M × (0,98 + 0,06 × p).
- Invertir: M × (0,76 + 0,44 × p).

Cambiar M multiplica todos los valores por el mismo factor positivo y conserva su orden. La ganadora cambia con las probabilidades o con los pagos, no necesariamente con el monto.

Con las probabilidades actuales 0,333333333333 y 0,666666666667 y M = 500:

- Guardar: 0,333333333333 × 500 + 0,666666666667 × 500 = 500.
- Ahorrar: 0,333333333333 × 520 + 0,666666666667 × 490 = 499,999999999990.
- Invertir: 0,333333333333 × 600 + 0,666666666667 × 380 = 453,333333333260.

Guardar queda ligeramente por encima de Ahorrar por la aproximación de las probabilidades. Con fracciones exactas 1/3 y 2/3 empatan. No es una ventaja real de Guardar sobre Ahorrar: es una diferencia numérica por la representación de probabilidades. El selector existente compara antes de redondear. No se modificó esa regla para imponer otra recomendación.

La interfaz ahora muestra los tres montos finales esperados debajo de la recomendación, con aviso SIMULACIÓN y una nota sobre el redondeo. Para M = 1.000 muestra 1.000,00; 1.000,00 y 906,67 BOB, respectivamente. Cada importe se calcula con precisión interna y luego se redondea por separado: el doble de un importe mostrado puede diferir en un centavo del resultado de evaluar el doble del monto.

Se ampliaron EvaluacionAutomaticaTest y VmeServiceTest para verificar las tres ganadoras, todos los VME a distintos montos, cambios de pagos con probabilidades fijas, independencia del orden, empates exactos y diferencias debidas a la aproximación. La prueba de interfaz comprueba las tres líneas de comparación. Esta revisión no modifica datos de usuarios.

Archivos de esta revisión: plantilla y estilos de analisis-financiero.component, su .spec.ts, EvaluacionAutomaticaTest.java, VmeServiceTest.java, el comprobador .tools/revision/verificar-metodos.cjs y esta guía. Evidencia en evidencias/logs/revision-vme-*.log.
