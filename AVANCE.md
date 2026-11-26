# Avance del proyecto de finanzas

## Continuidad acordada con el usuario

El usuario pidió expresamente registrar aquí TODO lo que avancemos para retomar
el proyecto tras reiniciar la conversación. Mantener este archivo como registro
principal: cambios realizados, verificaciones reales, errores y pendientes.
Leer primero las entradas más recientes; no repetir cargas de datos ni asumir
que los procesos siguen activos sin comprobarlo. No incluir claves ni tokens.

## Último avance: presentación exclusiva de Markov y VME — 17/11/2026

Solicitud: modificar únicamente la presentación de estos dos métodos, sin tocar
backend, fórmulas, base de datos, otras funcionalidades ni cargar más datos.

Terminado:
- Markov muestra «Tu situación financiera», las dos frases de estado solicitadas,
  «Tu próximo mes», probabilidades de estabilidad y déficit y consejo breve.
- Sin datos: «Necesitamos más historial para conocer cómo podrían estar tus finanzas».
- VME se titula «¿Qué puedes hacer con tu dinero?», mantiene saldo, un solo campo
  de monto y Evaluar, y muestra tres etiquetas compactas: Guardar, Ahorrar, Invertir.
  La ganadora se destaca únicamente tras recibir el resultado del backend.
- El enlace «Configurar opciones (opcional)» conserva la ruta secundaria existente;
  no se agregaron los seis campos ni tablas al Dashboard.
- `nombreOpcion()` cambia solo «Guardar mi dinero» por «Guardar» en presentación.
  Los nombres enviados a Java y todos los cálculos permanecen iguales.
- Se conserva el tema oscuro y la adaptación móvil.

Archivos frontend modificados, dentro de `s4-proyecto/frontend/src/app/features/dashboard/`:
`analisis-financiero.component.html`, `.scss`, `.ts`, `.spec.ts` y
`explicacion-metodos.component.spec.ts` (ajuste de etiqueta esperada).

Verificación: 26 pruebas Angular aprobadas y compilación de producción correcta.
Chrome real: Dashboard con un campo, dos probabilidades (33.3%/66.7% en demo),
tres etiquetas, cero tablas y cero campos de escenarios. Se introdujeron escenarios
en la pantalla opcional y se volvió al Dashboard: Ahorrar, VME3271.67 BOB, señalado
como demostración. Sin errores JavaScript ni desbordamiento móvil a390px.
La primera comprobación del navegador esperaba texto dentro de un desplegable cerrado;
se corrigió ese criterio del script, no el cálculo de la aplicación.
Artefactos: `inicio/presentacion-tests.log`, `inicio/presentacion-build.log` e
`inicio/presentacion-dashboard.png`. No hubo escrituras de datos financieros.

Estado al cerrar esta tarea: implementación y comprobaciones terminadas, sin
pendientes de esta simplificación. Los cambios siguen locales. El frontend estaba
en localhost:4200 y el backend en localhost:8080 con perfil demo-local; comprobar
su disponibilidad en la próxima sesión antes de afirmar que están corriendo.

## Demostración completa y pruebas del sistema — 17/11/2026

Se revisaron las migraciones, entidades y la base PostgreSQL real antes de cargar.
Conexión comprobada: base `finanzas`, loopback IPv6, puerto 5432. Existían tres
usuarios, uno de ellos la cuenta demo, con 36 movimientos, cuatro billeteras y
ocho meses cerrados. Se reutilizó esa cuenta; no se borraron registros ni se
cambiaron claves, permisos o datos de las otras dos cuentas.

### Carga repetible y comprobación del entorno

`inicio/preparar-demo.py` lee el acceso demo de la configuración local existente
o de FINDEX_DEMO_PASSWORD, sin imprimir credenciales ni tokens. Por defecto solo
comprueba el entorno. Con `--aplicar`, agrega datos únicamente a la cuenta demo.
`PREPARAR_DEMO.cmd` permite ejecutar esa carga con el backend ya iniciado.
El antiguo `inicio/cargar-historial-demo.ps1` ahora llama al mecanismo verificado.

`EntornoDemoController.java` expone una comprobación autenticada de solo lectura
únicamente con el perfil `demo-local`. Rechaza otras cuentas, perfiles con «prod»,
app.demo.enabled=false y conexiones distintas de PostgreSQL loopback:5432/finanzas.
El perfil está en `application-demo-local.properties`; el script de inicio local
lo activa explícitamente. No existe una función de limpieza o borrado.

La carga conserva un mes de referencia en `inicio/demo-completa-periodo.json`,
usa identificadores descriptivos estables «DEMO COMPLETA v1» y evita dos cargas
simultáneas mediante un bloqueo local. Cada operación usa la API y los servicios
normales; si se interrumpe, la siguiente ejecución completa lo que falta.
Las cargas posteriores verificadas crearon cero registros nuevos.

### Datos cargados y resultados

Primera ampliación: 6 categorías nuevas, 1 billetera nueva, 39 ingresos,
73 gastos, 51 presupuestos, 3 metas, 3 aportes manuales y 25 transferencias.
Totales demo: 10 categorías, 5 billeteras, 48 ingresos, 100 gastos,
52 presupuestos en 13 períodos, 4 metas y 26 transferencias.

Saldos: Banco BOB 945; Caja BOB 100; DEMO Ahorros BOB 1200;
DEMO historial BOB 1000; Cuenta USD 300. Disponible BOB=3245; USD separado.
Todas las billeteras concilian con saldo inicial + ingresos − gastos + transferencias
recibidas − enviadas − aportes a metas. Se comprobaron 25 transferencias nuevas:
cada una conservó el total y actualizó ambos saldos exactamente.

Historial cerrado: noviembre 2025 a octubre 2026, doce meses consecutivos,
secuencia E,D,E,E,D,E,E,D,D,E,D,E. Los nuevos movimientos incluyen sueldo,
trabajos independientes, ingresos adicionales, comida, transporte, servicios,
educación, entretenimiento, salud y vivienda. No hay movimientos futuros.
Noviembre actual: ingresos 5100, gastos 3075, balance 2025 BOB, ESTABLE.
Las etiquetas de los movimientos y la insignia DEMO de todas las páginas
identifican los datos ficticios.

Presupuestos actuales: Comida 650/600 (se conservó el límite anterior, exceso50),
Transporte180/300 (disponible120), Entretenimiento275/250 (exceso25),
Servicios400/400 (disponible0). En meses nuevos se usan límites de comida800,
transporte300, entretenimiento250 y servicios400. Los consumos se conciliaron
contra los gastos registrados. La vista ahora muestra disponible o exceso.

Metas nuevas: DEMO Comprar computadora, objetivo7000, avance500, fecha julio2027;
DEMO Fondo de emergencia, objetivo5000, avance400, fecha noviembre2027;
DEMO Viaje, objetivo4000, avance250, fecha mayo2027. La meta anterior Viaje a Lima
permanece; sus aportes automáticos la llevaron a3030/3000. Ese pequeño sobrepaso
es la regla existente: el aporte que completa la meta no se recorta y luego cesan
los aportes automáticos. Los avances siempre se descontaron de billeteras.

Markov verificado recalculando frecuencias desde los movimientos:
E→E=2, E→D=4; D→E=4, D→D=1. Matriz [[1/3,2/3],[0.8,0.2]].
Desde el estado actual E: estabilidad33.33%, déficit66.67% para diciembre2026.
Son resultados de datos ficticios registrados; no se escriben probabilidades fijas.

VME: monto500, reserva2745, disponible3245. Dos botones en la pantalla secundaria:
- «Ejemplo: ahorro»: cambios netos guardar(0,0), ahorro(60,10), inversión(20,-200).
  VME guardar3245, ahorro3271.67, inversión3118.33; gana Ahorrar.
- «Ejemplo: inversión»: guardar(0,0), ahorro(10,0), inversión(200,-20).
  VME guardar3245, ahorro3248.33, inversión3298.33; gana Invertir.
Ambos casos usan las mismas probabilidades reales del historial demo. Los pagos
son supuestos didácticos, no rendimientos reales ni ganancias garantizadas.

### Errores corregidos y pruebas

Se terminó la corrección pendiente del botón «Comparar opciones»: muestra por qué
está bloqueado (monto, saldo, importes incompletos o decimales, historial por moneda)
y presenta el resultado directamente, sin exigir volver al Dashboard ni abrir
detalles. Se retiró el apartado «Detalles matemáticos para tu exposición».

Se corrigió además la suma de dólares y bolivianos del Dashboard: los importes
se consultan por divisa, BOB por defecto, con `?divisa=USD` disponible en la API.
El DTO y los indicadores muestran la moneda. No se introduce conversión ficticia.

63 pruebas Java aprobadas, sin fallos ni omisiones. Incluyen el nuevo
`SistemaCompletoIntegrationTest.java`: registro, login, JWT, billeteras, categorías,
ingresos/gastos, presupuestos, alertas, edición, aportes, metas, transferencias,
anulaciones, protección entre usuarios, saldo insuficiente, doce meses de Markov,
dos ganadores VME y separación de monedas. Las pruebas H2 revierten sus registros.
`EntornoDemoControllerTest.java` comprueba restricciones de host, base, perfil y cuenta.

26 pruebas Angular aprobadas y compilación de producción correcta. Incluyen ingreso
real de los seis campos, aceptación de cero, activación del botón, resultado visible,
errores de red y recuperación, escenarios diferentes y navegación entre pantallas.

Verificación adicional con Chrome real y PostgreSQL local: login mediante formulario,
ocho rutas autenticadas, filtros ingreso/gasto, cinco billeteras, diez categorías,
cuatro presupuestos actuales, cuatro metas y veintiséis transferencias visibles;
gráfico de doce meses; comparación manual y ambos ejemplos; falta de historial USD;
retorno al Dashboard y nueva evaluación. Cero errores HTTP/JavaScript durante el
recorrido final y sin desbordamiento horizontal del Dashboard móvil a390px.
El automatismo de login necesitó esperar a que Angular terminara de procesar los
campos antes de enviar; se corrigió el script de comprobación, no las credenciales.

Informes locales sin claves: `inicio/demo-completa-primera-carga.json`,
`inicio/demo-completa-reporte.json`, `inicio/demo-pruebas-navegador.json`.
La auditoría conserva cantidades y huellas de las otras cuentas antes/después:
son iguales. Captura: `inicio/demo-dashboard.png`. Pruebas: `inicio/demo-tests-java.log`,
`inicio/demo-tests-angular.log`, `inicio/demo-build.log`.

### Guía para presentar FINDEX

1. Iniciar con `INICIAR.cmd`. El perfil demo-local sirve solo para desarrollo local.
2. Abrir localhost:4200. Consultar EMAIL y PASSWORD en la configuración local
   existente `DatosDemoInicializador.java`; introducirlos en privado. No se cambió
   la contraseña ni se incluye en esta guía o en los informes.
3. Mostrar Dashboard: BOB3245, ingresos5100, gastos3075, balance2025 y dos excesos.
4. En Billeteras explicar efectivo, banco y ahorro, y que USD no se suma como BOB.
5. En Movimientos filtrar INGRESO, después GASTO, y escoger fechas de meses cerrados.
6. En Categorías mostrar la variedad de ingresos y gastos; los datos pertenecen
   exclusivamente a la cuenta indicada como DEMO.
7. En Presupuestos explicar consumido/límite, disponible y exceso; cambiar de mes.
8. En Metas mostrar objetivos, fechas, avances y la meta completada con aportes reales
   dentro de la demostración; no pulsar eliminar durante la presentación preparada.
9. En Traspasos mostrar fondos de efectivo y ahorro: trasladan dinero, no crean ingreso.
10. En «Explicación de los métodos» abrir «Ver historial mensual» y explicar los estados.
11. En «Prueba tus opciones de dinero», seleccionar BOB y usar ambos botones de ejemplo:
    gana ahorrar en uno e invertir en otro. El resultado se ve inmediatamente.
12. Volver al Dashboard y pulsar Evaluar para comprobar que usa los escenarios de la
    sesión y vuelve a consultar probabilidades. USD muestra falta de historial.

Para repetir la preparación: `PREPARAR_DEMO.cmd`. No borra registros. La comprobación
de entorno ocurre antes de cualquier escritura. No se ejecutó carga alguna en producción.

## Resumen mínimo de los métodos — 17/11/2026

Se acortaron únicamente los textos de `explicacion-metodos.component.html`:
Markov: «Analiza tu historial para estimar cómo estarán tus finanzas el próximo mes».
VME: «Compara guardar, ahorrar o invertir y muestra la opción con el mejor resultado
esperado». La fórmula sencilla se trasladó al detalle matemático y se quitó otra
explicación repetida de la configuración. Se conservan cálculos y validaciones.

## Textos cotidianos para explicar los métodos — 17/11/2026

Solo se modificó presentación Angular: «Prueba tus opciones de dinero»,
«¿Cómo calculamos tu futuro financiero?» y «¿Cómo elegimos la mejor opción para
tu dinero?», con las frases cortas solicitadas. Las etiquetas visibles ahora son
«Guardar mi dinero», «Ahorrar», «Invertir», «Si te va bien económicamente» y
«Si gastas más de lo que ganas». `nombreOpcion()` traduce únicamente la etiqueta;
los nombres enviados a los endpoints y los cálculos permanecen iguales.

Se quitaron párrafos extensos de la vista inicial y se mantuvieron historial,
matriz, fórmulas, pagos y operaciones en «Detalles matemáticos para tu exposición».
La fórmula sencilla describe el cambio esperado; el detalle conserva la aclaración
de que el VME mostrado incluye el saldo inicial. Los avisos de falta de datos y
las indicaciones antes de las tablas usan los textos pedidos por el usuario.

Se actualizaron las dos plantillas, el método de etiquetas en el componente y
sus pruebas de presentación. Validación: 22 pruebas Angular aprobadas y compilación
de producción correcta. No hubo cambios en backend, fórmulas ni validaciones.

## Tarjetas mínimas y pantalla separada de métodos — 17/11/2026

El Dashboard ahora contiene únicamente estado financiero, probabilidad y una frase
en Markov; saldo registrado, monto, botón «Evaluar» y eventual mejor alternativa
con su VME en la segunda tarjeta. Se retiraron del Dashboard ingresos/gastos
repetidos, selector de moneda, botones adicionales, detalles, escenarios y tablas.
Falta de información: «Necesitamos más información para calcular tu recomendación».
No se habilita una recomendación sin probabilidades y resultados monetarios válidos.

Un único enlace pequeño abre `/explicacion-metodos`, protegida por el authGuard
existente y titulada «Explicación de los métodos». Allí quedaron selector de moneda,
configuración de escenarios, historial, matriz, fórmulas y operaciones numéricas.
El ejemplo didáctico también permanece exclusivamente en esa pantalla.

Archivos nuevos en `s4-proyecto/frontend/src/app/features/dashboard/`:

- `explicacion-metodos.component.ts` y `.html`: pantalla secundaria; reutiliza la
  carga y validación del componente de análisis sin duplicar fórmulas del backend.
- `escenarios-financieros.store.ts`: parámetros en memoria por sesión y moneda,
  copiados de forma independiente; se limpian al cambiar de sesión y no almacenan
  probabilidades ni recomendaciones. Permiten volver al Dashboard y evaluar allí.
- `explicacion-metodos.component.spec.ts`: separación de pantallas, restauración
  de parámetros y aislamiento de sesiones y monedas.

Se modificaron las vistas y pruebas de `analisis-financiero.component`, sus estilos
y `frontend/src/app/app.routes.ts` para registrar la ruta nueva. No se cambió Java,
la base de datos ni las demás funcionalidades durante esta etapa.

Verificación: compilación de producción correcta; 21 pruebas Angular y 54 pruebas
Java aprobadas. Chrome real: Dashboard con un campo, cero tablas y cero detalles,
un enlace; secundaria con seis campos y matriz; vuelta al Dashboard y reevaluación
contra PostgreSQL muestra «Destinar a inversión · 4746 BOB» marcado como demostración.
Sin errores JavaScript ni desbordamiento móvil a 390 px. Capturas locales:
`inicio/minimo-desktop.png` y `inicio/minimo-mobile.png`.

## Simplificación de la pantalla financiera — 17/11/2026

Se sustituyó la vista extensa de Markov/VME por dos tarjetas compactas:

- Markov: estado cotidiano, ingresos/gastos actuales, una probabilidad de estabilidad
  y consejo breve. Sin probabilidades se muestra únicamente el aviso de historial:
  «Estamos reuniendo historial para mostrarte una predicción del próximo mes».
- VME: saldo por moneda, un campo de monto, botón «Evaluar mis opciones» y resultado
  breve cuando existe. Sin resultados monetarios configurados el botón permanece
  deshabilitado y se indica que faltan datos de rendimiento; no se inventan tasas.
- Se eliminaron las tres tarjetas de alternativas, barras y bloques repetidos.
- «Simulación avanzada», cerrada por defecto, contiene los campos de escenarios
  y el ejemplo explícito. Ningún supuesto se precarga al entrar a la pantalla.
  Toda comparación se etiqueta como simulación o demostración.
- «Detalles del cálculo», un solo apartado cerrado inicialmente, reúne historial,
  matriz, frecuencias, fórmulas, probabilidades, pagos, operaciones, viabilidad y VME.
- La cuenta demo se identifica por el subject del JWT únicamente para mostrar el
  aviso de demostración; no se utiliza esa lectura para autorización. Java conserva
  toda la autenticación y los cálculos existentes.

Archivos modificados: `analisis-financiero.component.ts`, `.html`, `.scss` y `.spec.ts`
en `s4-proyecto/frontend/src/app/features/dashboard/`. Se redujeron los textos y los
estilos, y se adaptaron las pruebas para comprobar que no haya tablas ni entradas
de escenarios fuera de los apartados secundarios.

Validación final: 18 pruebas Angular y 54 pruebas Java aprobadas; compilación de
producción correcta. Se corrigió durante la implementación una expresión `as`
no admitida por Angular 17 dentro de `@else if` antes de la ejecución final.
Revisión real en Chrome de escritorio y móvil de 390 px sin excepciones JavaScript
ni desbordamiento horizontal, también con los detalles abiertos. Capturas:
`inicio/simple-desktop.png`, `inicio/simple-mobile.png` y `inicio/simple-resultado.png`.
No se cambiaron servicios Java, fórmulas, endpoints, tablas ni datos en esta etapa.

## Datos para practicar Markov y VME — 17/11/2026

A petición del usuario se cargaron 32 movimientos ficticios únicamente en
`demo@finanzas.test`, usando los endpoints existentes. Se creó la billetera
`DEMO historial BOB` con saldo inicial ficticio de 1000 BOB. Las descripciones
de los movimientos comienzan con `DEMO MARKOV`. No se editaron otras cuentas
ni se escribieron probabilidades directamente en la base de datos.

El script reutilizable es `inicio/cargar-historial-demo.ps1`. Identifica los
movimientos por descripción y billetera: una segunda ejecución creó cero
movimientos. Usa las reglas normales de transacciones, incluidos los aportes
automáticos de la meta de ahorro demo, por lo que los saldos permanecen coherentes.
No se modificó la semilla Java ni el esquema.

Datos cargados en esta ejecución:

| Meses | Ingreso mensual | Gasto mensual | Estado |
| --- | ---: | ---: | --- |
| Marzo a julio de 2026 | 2000 BOB | 1600 BOB | ESTABLE |
| Agosto y septiembre de 2026 | 2000 BOB | 2400 BOB | DEFICIT |
| Octubre de 2026 | 2000 BOB | 1600 BOB | ESTABLE |

Cada mes tiene un ingreso de salario y tres gastos: comida, transporte y servicios.
Secuencia E,E,E,E,E,D,D,E: E→E=4, E→D=1, D→E=1, D→D=1.
La fila ESTABLE es (0.8,0.2), la fila DEFICIT es (0.5,0.5). El estado provisional
de noviembre permanece ESTABLE, así que la predicción de diciembre es 80%/20%.

Se añadió `cargarEjemplo()` al componente Angular y el botón «Cargar ejemplo de
VME». El usuario lo activa explícitamente: rellena hasta 500 del saldo como monto
a evaluar y resultados netos mantener=(0,0), ahorro=(20,-10), inversión=(100,-120).
No introduce probabilidades; usa las del historial registrado. Un aviso permanente
durante ese ejemplo identifica los escenarios como hipotéticos, también si se editan.
Los parámetros no se persisten y el botón debe usarse de nuevo tras recargar.

Resultado comprobado contra la API y desde Chrome con la cuenta demo:
saldo BOB=4690, monto=500, reserva=4190. VME mantener=4690,
ahorro=0.8*4710+0.2*4680=4704, inversión=0.8*4790+0.2*4570=4746.
Las tres opciones son viables y gana inversión bajo estos supuestos didácticos.
Los resultados son patrimonio final esperado, no ganancias garantizadas.

Se modificaron `analisis-financiero.component.ts`, su `.html` y su `.spec.ts`
en `s4-proyecto/frontend/src/app/features/dashboard/`. Pasaron las 16 pruebas
Angular y la compilación de producción. En Chrome se verificaron dos barras,
tres resultados VME, tres operaciones en el detalle y el aviso de demostración,
sin errores JavaScript ni desbordamiento móvil, incluso con detalles abiertos.

## Corrección de categorías duplicadas — 16/11/2026

El backend solo comparaba el nombre exacto ignorando mayúsculas. Ahora
`NombreCategoria.java` limpia espacios Unicode y caracteres invisibles, compara
sin mayúsculas, tildes ni espacios, y detecta una letra añadida o faltante en
nombres de al menos cuatro caracteres. No aplica coincidencia difusa arbitraria:
«Casa» y «Caja», nombres con distintos números y nombres más específicos siguen
siendo distintos. La regla de una letra puede bloquear también palabras distintas
que difieran solo por esa inserción: el mensaje pide un nombre más específico.

`CategoriaService.crear()` revisa todas las categorías del mismo usuario,
incluidas las inactivas, y devuelve HTTP 409 con el nombre conflictivo. Bloquea
la fila del usuario mientras valida y guarda para serializar solicitudes
simultáneas. Las categorías de usuarios diferentes no interfieren entre sí.
No modifica tablas ni elimina o fusiona duplicados preexistentes.

Archivos modificados/creados dentro de `s4-proyecto/`:

- `backend/src/main/java/com/finanzas/service/NombreCategoria.java`.
- `backend/src/main/java/com/finanzas/service/CategoriaService.java`.
- `backend/src/main/java/com/finanzas/repository/CategoriaRepository.java`.
- `backend/src/main/java/com/finanzas/repository/UsuarioRepository.java`.
- `backend/src/test/java/com/finanzas/service/NombreCategoriaTest.java`.
- `backend/src/test/java/com/finanzas/service/CategoriaServiceTest.java`.
- `frontend/src/app/features/categorias/categorias.component.ts`: evita un segundo
  envío mientras se guarda.
- `frontend/src/app/features/categorias/categoria.service.ts`: muestra el detalle
  del conflicto devuelto por el servidor.
- `frontend/src/app/core/interceptors/error.interceptor.ts`: conserva el mensaje
  específico de los conflictos HTTP 409 en lugar de sustituirlo por uno genérico.
- `frontend/src/app/features/categorias/categorias.component.spec.ts`.

La suite inicial de 53 pruebas Java pasó. Después pasaron las siete pruebas de
CategoriaServiceTest, incluida la prueba adicional de solicitudes simultáneas:
los informes acumulan 54 pruebas Java sin fallos. Las 14 pruebas Angular pasaron,
incluida la conservación del mensaje a través del interceptor real. La compilación
de producción Angular pasó. Backend reiniciado para aplicar la corrección.
Comprobación real en PostgreSQL: existe «Comida», se intentó «Comidaa», respondió
HTTP 409 identificando «Comida» y el número de categorías permaneció en cuatro.

## Rediseño de experiencia de Markov y VME — 16/11/2026

Se revisaron los servicios Java, el controlador y los contratos HTTP existentes.
Esta mejora cambia la presentación Angular; no cambia cálculos, endpoints,
entidades ni base de datos.

- «Tu futuro financiero» muestra el estado actual con lenguaje cotidiano,
  ingresos, gastos y barras nativas de probabilidad solo si Markov las devuelve.
  Sin historial suficiente muestra qué falta y enlaza a movimientos. La sugerencia
  depende de cuál probabilidad es mayor, incluyendo el caso de empate.
- La matriz, frecuencias, historial, clasificaciones y divisiones numéricas reales
  permanecen dentro de «Ver cómo se calculó», cerrado inicialmente.
- «¿Qué puedes hacer con tu dinero?» separa saldo registrado y monto elegido.
  FINDEX no puede certificar que el saldo esté libre de obligaciones: el usuario
  decide cuánto evaluar tras considerar sus pagos pendientes.
- El frontend traduce esa elección al contrato existente: asignación de mantener=0;
  asignaciones de ahorro e inversión=monto elegido; reserva=saldo−monto elegido,
  calculada en centavos. Se comparan opciones excluyentes, no asignaciones simultáneas.
- Tres tarjetas muestran opciones y, tras evaluar, VME como saldo final esperado,
  viabilidad y ganadora(s). La reserva sigue incluida en el patrimonio esperado.
- «Configurar escenarios» es opcional y está cerrado inicialmente. No se precargan
  intereses, ganancias ni pérdidas; los seis resultados monetarios se introducen
  con etiquetas cotidianas. Sin estos supuestos o sin probabilidades de Markov no
  se habilita la comparación. El ejemplo de ayuda está identificado como ilustrativo
  y no se aplica a los datos. No se agregó una simulación con probabilidades ficticias.
- El detalle del VME muestra las probabilidades devueltas en la misma evaluación,
  los pagos, la tabla completa, operaciones numéricas y motivos del resultado.
- Cambiar moneda limpia monto y supuestos para evitar reutilizarlos accidentalmente.
  Editar entradas elimina resultados anteriores y descarta respuestas obsoletas.

Archivos modificados en `s4-proyecto/frontend/`:

1. `src/app/features/dashboard/analisis-financiero.component.ts`: estado de pantalla,
   validación, textos interpretativos, fechas y traducción al pedido VME existente.
2. `src/app/features/dashboard/analisis-financiero.component.html`: vista sencilla,
   configuración avanzada y detalles matemáticos desplegables.
3. `src/app/features/dashboard/analisis-financiero.component.scss`: jerarquía visual,
   colores del tema existente, barras, tarjetas, foco y diseño adaptable.
4. `src/app/features/dashboard/analisis-financiero.component.spec.ts`: diez pruebas
   de la experiencia y del contrato HTTP, incluida falta de historial, reserva,
   cambio de moneda, resultados, empate y respuestas obsoletas.
5. `src/app/shared/components/navegacion/navegacion.component.scss`: corrección pequeña
   de desbordamiento móvil: enlaces desplazables dentro de su contenedor.

Validación: diez pruebas Angular aprobadas y 44 pruebas Java aprobadas, sin fallos.
La compilación final de producción Angular terminó sin advertencias de tamaño.
Se revisó el Dashboard real en Chrome de escritorio y en una pantalla de 390 px:
sin excepciones JavaScript, sin tablas fuera de detalles y sin desbordamiento
horizontal después del ajuste de navegación. La cuenta demo conserva su historial
insuficiente y no muestra barras ni recomendaciones ficticias. Las pruebas Angular
con datos controlados verifican también el caso de probabilidades y recomendaciones.
Capturas locales: `inicio/ux-desktop.png` e `inicio/ux-mobile.png`.

Para la defensa: Markov conserva P(i,j)=N(i,j)/sum(N(i,k)). VME conserva
sum(P(estado)*pago(estado)), con pago=saldo+cambio neto configurado. La interfaz
traduce estados y recoge decisiones; Java sigue validando probabilidades, saldo,
reserva y recomendación. La presentación simplificada no reemplaza el método.

## Implementación de Markov y VME — 16/11/2026

Se implementaron desde cero, con dos tarjetas nuevas en el Dashboard. La base
real del proyecto es PostgreSQL 16, no MySQL. No se modificaron entidades,
tablas ni migraciones. Los parámetros del VME son temporales y no se guardan.

### Datos y reglas

- `transacciones`: monto, tipo INGRESO/GASTO, fecha, anulada, usuario, billetera,
  categoría. Se usan solo las del usuario autenticado y no anuladas.
- `billeteras`: saldo actual, divisa, activa y usuario. La disponibilidad suma
  solo billeteras activas de una divisa; el historial incluye las inactivas.
- `presupuestos`: límite y consumo por usuario, categoría, año y mes. Los límites
  no son obligaciones futuras: no se descuentan automáticamente del disponible.
- `traspasos` y `aportes_meta` no se interpretan como ingresos/gastos nuevos.
  Los aportes ya descuentan saldo de las billeteras; no se suman otra vez.

Markov agrupa meses con registros: ingresos >= gastos produce ESTABLE; en otro
caso, DEFICIT. Usa pares de meses cerrados consecutivos. Los huecos son datos
desconocidos y no se rellenan con ceros. La matriz no incluye el mes actual;
su estado provisional selecciona la fila para predecir el mes siguiente.
Sin movimientos actuales o sin salidas históricas desde ese estado, no hay
predicción ni recomendación VME. No se agregan datos de otros usuarios,
conversiones de moneda, suavizado ni probabilidades por defecto.

Para cada alternativa, el usuario introduce asignación y resultado neto en
ambos estados, en la misma moneda y con horizonte de un mes. No hay tasas
prefijadas ni rendimientos reales implícitos. D es el disponible; A, la asignación.
Pago(estado) = D + resultadoNeto(estado). La asignación permanece en el patrimonio,
pero la liquidez inmediata es D - A. La reserva mínima la configura el usuario.
Viabilidad: D > 0; D - A >= reserva; ambos pagos >= 0.
VME = P(ESTABLE)*pagoEstable + P(DEFICIT)*pagoDeficit.
Se elige el máximo viable antes de redondear; se informan los empates.
No se ejecutan inversiones, aportes ni transferencias.

### Archivos reales

Rutas relativas a `s4-proyecto/`:

| Archivo | Responsabilidad |
| --- | --- |
| `backend/src/main/java/com/finanzas/service/MarkovService.java` | Agrupación mensual, estados, frecuencias, matriz, probabilidades y avisos. |
| `backend/src/main/java/com/finanzas/service/VmeService.java` | Validación de probabilidades, pagos, viabilidad, VME y selección. |
| `backend/src/main/java/com/finanzas/service/AnalisisFinancieroService.java` | Resuelve al usuario autenticado, consulta JPA, filtra divisas y combina cálculos. |
| `backend/src/main/java/com/finanzas/controller/AnalisisFinancieroController.java` | Endpoints autenticados y validación HTTP. |
| `backend/src/main/java/com/finanzas/dto/MarkovResponse.java` | Records Mes, Fila, Probabilidades y enum Estado para responder al cliente. |
| `backend/src/main/java/com/finanzas/dto/VmeRequest.java` | Validaciones de reserva, divisa y alternativas; no recibe probabilidades del cliente. |
| `backend/src/main/java/com/finanzas/dto/VmeResponse.java` | Resultado por alternativa, disponibilidad, probabilidades usadas y recomendadas. |
| `backend/src/main/java/com/finanzas/repository/BilleteraRepository.java` | Nueva consulta por usuario que incluye billeteras inactivas. |
| `frontend/src/app/features/dashboard/analisis-financiero.service.ts` | Tipos y peticiones HTTP. |
| `frontend/src/app/features/dashboard/analisis-financiero.component.ts` | Carga, parámetros, validación de formulario y descarte de respuestas obsoletas. |
| `frontend/src/app/features/dashboard/analisis-financiero.component.html` | Dos tarjetas, matrices, formularios, pagos, avisos y detalles del cálculo. |
| `frontend/src/app/features/dashboard/analisis-financiero.component.scss` | Estilos con variables existentes y tablas con desplazamiento en móvil. |
| `frontend/src/app/features/dashboard/dashboard.component.ts` | Importa el componente nuevo. |
| `frontend/src/app/features/dashboard/dashboard.component.html` | Inserta el componente debajo de los indicadores existentes. |

### Recorrido de los datos

Angular envía el JWT con el interceptor existente. Spring Security valida la
sesión y el controlador usa `Authentication.getName()`, no un ID enviado por
el navegador. AnalisisFinancieroService obtiene el usuario y consulta los
repositorios de transacciones y billeteras dentro de una transacción de lectura.
Hibernate ejecuta consultas en PostgreSQL. MarkovService calcula las
probabilidades, VmeService calcula los valores esperados, los DTO se serializan
a JSON y AnalisisFinancieroComponent presenta el resultado.

Endpoints bajo `/api/dashboard/analisis`:

- GET `/divisas`: divisas de las billeteras del usuario.
- GET `/disponible?divisa=BOB`: saldo activo en esa divisa.
- GET `/markov?divisa=BOB`: historial, estado provisional y matriz.
- POST `/vme`: recalcula saldo y Markov en servidor; evalúa los escenarios recibidos.

### Ejercicio para la defensa

Ejemplo hipotético; no corresponde al historial real de la cuenta demo.
Ocho meses cerrados: E,E,E,E,E,D,D,E. El mes actual provisional es E.
Entre los meses cerrados hay E→E=4, E→D=1, D→E=1, D→D=1.
P = [[4/5,1/5],[1/2,1/2]]. Como el estado actual es E, el próximo mes tiene
P(E)=0.8 y P(D)=0.2. La fila D tiene menos observaciones y mayor incertidumbre;
ninguna de estas frecuencias garantiza el futuro.

D=1000, reserva=100, asignaciones mantener=0, ahorro=400, inversión=600.
Resultados netos configurados: mantener=(0,0), ahorro=(20,-10), inversión=(100,-120).
Pagos: mantener=(1000,1000), ahorro=(1020,990), inversión=(1100,880).
VME mantener=1000; ahorro=0.8*1020+0.2*990=1014;
inversión=0.8*1100+0.2*880=1056. Todas cumplen la reserva y gana inversión.
Si P(E)=0.5, los VME son 1000,1005,990 y gana ahorro.
Si P(E)=0.1, son 1000,993,902 y gana mantener. Estos cambios muestran que
el modelo no impone una alternativa ganadora por construcción.

Frase para exponer: «Markov estima probabilidades a partir de mis transiciones
mensuales observadas. VME usa esas probabilidades para ponderar los pagos que
configuré en cada escenario y elige el mayor valor esperado entre las opciones
que conservan la reserva. Es apoyo a la decisión: depende de la calidad del
historial, de los supuestos de pago y de que el comportamiento siga siendo parecido».

### Pruebas y comprobación real

- `backend/src/test/java/com/finanzas/service/MarkovServiceTest.java`: frecuencias,
  orden temporal, empate de ingresos/gastos, exclusiones, huecos, filas sin datos,
  ausencia de estado actual y ejercicio de defensa.
- `backend/src/test/java/com/finanzas/service/VmeServiceTest.java`: fórmula, cambios
  de ganador, probabilidades inválidas, saldo insuficiente, reserva, pérdidas,
  empates, nombres repetidos y selección previa al redondeo.
- `backend/src/test/java/com/finanzas/service/AnalisisFinancieroIntegrationTest.java`:
  HTTP, autenticación, validaciones anidadas, aislamiento entre usuarios y divisas,
  historial inactivo y ausencia de cambios de saldo. Base H2 con rollback.
- `frontend/src/app/features/dashboard/analisis-financiero.component.spec.ts`:
  seis pruebas de presentación, validación, peticiones, cambios de divisa,
  respuestas obsoletas, usuario sin billeteras y error de conexión.
- `frontend/karma.conf.cjs`: configuración de pruebas de Chrome sin GPU.
- Compilación de producción Angular completada. Seis pruebas Angular aprobadas.
  ChromeHeadless convencional falló al iniciar la GPU; la configuración incluida
  permitió ejecutar todas las pruebas.
- La primera ejecución ampliada tuvo un fallo de aserción textual 1000/1000.00;
  se corrigió a comparación numérica. Resultado final: 44 pruebas Java aprobadas,
  sin fallos, errores ni omisiones; incluye el ejercicio de Markov de este registro.
- Backend reiniciado con caché Maven local; PostgreSQL respondió y Flyway validó
  el esquema existente. Los cuatro endpoints se comprobaron con la sesión demo.
- Estado demo observado: disponible 3690 BOB, mes actual ESTABLE, cero meses
  cerrados. Probabilidades y VME nulos, recomendadas vacías, aviso de falta de
  transiciones. No se crearon movimientos históricos ficticios.
- No se probó MySQL porque el proyecto no lo tiene configurado. Las pruebas de
  interfaz usan HTTP simulado; no equivalen a una prueba E2E completa con PostgreSQL.
- Los cambios siguen locales, sin commit ni envío a GitHub.

Registro de la sesión del 16 de noviembre de 2026.

## Ubicación y objetivo

Carpeta de trabajo: `C:\Users\SEBASTIAN\OneDrive\Desktop\proyecto\17-11`.

El repositorio Git está dentro de `s4-proyecto/`. El objetivo es poder ejecutar
el sistema de finanzas y mantener solo los archivos necesarios, con un inicio sencillo.

## Estructura actual

```text
17-11/
├── INICIAR.cmd
├── AVANCE.md
├── inicio/
│   ├── iniciar-backend.cmd
│   └── iniciar-frontend.cmd
├── .tools/
│   ├── apache-maven-3.10.1/
│   ├── node-v20.20.2-win-x64/
│   ├── maven-repository/
│   └── revision/
├── .github/
└── s4-proyecto/
    ├── backend/
    ├── frontend/
    ├── .git/
    ├── .github/
    └── .vscode/
```

`.tools/maven-repository` es una copia de caché usada durante las comprobaciones.
`.tools/revision` contiene archivos generados para revisar la compilación Java.
Los scripts de inicio usan las carpetas de Node y Maven; no requieren esas dos
carpetas de diagnóstico. El backend usa la caché predeterminada de Maven del usuario.

## Herramientas y dependencias

- Java JDK 17.0.21 de Eclipse Adoptium: ya estaba instalado.
- PostgreSQL 16: ya estaba instalado y aceptaba conexiones en el puerto 5432.
- Node 20.20.2 y npm 10.8.2: extraídos dentro de `.tools/` y comprobados.
- Maven 3.10.1: extraído dentro de `.tools/` y comprobado con Java 17.
- Se instalaron 887 paquetes del frontend desde la caché local con `npm ci --offline --ignore-scripts`.
- Node 24 sigue instalado en el sistema. Los scripts priorizan el Node 20 local
  porque el proyecto utiliza Angular 17.3.
- Docker está instalado, pero no es necesario para el arranque actual, que usa PostgreSQL local.

Los ZIP se descargaron en `C:\Users\SEBASTIAN\OneDrive\Documentos\ope`.
Pueden permanecer fuera del proyecto: los ejecutables usados ya están en `.tools/`.
`node_pdb.zip` era un archivo de depuración; el ZIP correcto fue `node-v20.20.2-win-x64.zip`.

## Puertos y base de datos

| Componente | Dirección |
| --- | --- |
| Frontend Angular | `http://localhost:4200/` |
| API Spring Boot | `http://localhost:8080/api/` |
| PostgreSQL | `localhost:5432`, base `finanzas` |

Se creó la base `finanzas` y se verificó el acceso con la configuración local del backend.
Flyway administra el esquema mediante las migraciones V1 y V2 en
`backend/src/main/resources/db/migration/`. No borrar esas migraciones.

El backend define el puerto 8080 en `application.properties`. Todos los servicios
Angular apuntan a esa API y CORS permite el origen `http://localhost:4200`.
La conexión a la base se puede sobrescribir mediante `DB_URL`, `DB_USER` y `DB_PASSWORD`.

## Correcciones realizadas

1. `backend/src/main/java/com/finanzas/service/TransaccionService.java`:
   se resolvió un conflicto de Git que impedía compilar. Se conservaron los imports
   de `BigDecimal` y `LocalDate`, ambos necesarios.
2. `frontend/src/app/features/transacciones/transacciones.component.ts`:
   se añadió la carga del historial al completar `cargarTodo()`. Ahora se solicita
   al entrar en la pantalla y después de registrar o anular movimientos, respetando los filtros activos.
3. Se crearon scripts para iniciar con las herramientas locales y se actualizaron
   sus rutas al agruparlos en `inicio/`.
4. Se creó `INICIAR.cmd` para abrir ambos servidores. Comprueba los puertos antes
   de lanzar procesos y evita duplicarlos si detecta un puerto ocupado.

## Comprobaciones y problemas encontrados

- TypeScript y el compilador de Angular validaron el código y las plantillas sin errores.
- La compilación completa dentro de la sesión restringida falló por permisos sobre
  carpetas del usuario. Esto no demostró un error en los archivos de estilos.
- La comprobación Java directa generó clases, pero terminó con una excepción de permisos;
  no se considera una verificación completa del backend ni una ejecución de sus pruebas.
- Desde la terminal normal del usuario, Angular compiló y anunció `http://localhost:4200/`.
- El usuario compartió un arranque de Spring Boot que conectó a PostgreSQL y validó
  ambas migraciones. Ese intento terminó porque 8080 ya estaba ocupado.
- Se comprobó que el proceso Java existente en 8080 ya atendía la API del proyecto:
  el inicio de sesión de demostración devolvió un token válido.
- En esa comprobación, el frontend devolvió HTTP 200 en 4200.
- Las advertencias de Maven sobre repositorios de Flyway no fueron la causa del
  fallo mostrado; la causa fatal de ese intento fue el puerto 8080 ocupado.
- No se ejecutó con éxito la suite completa de pruebas Java durante esta sesión.
- En la última inspección para escribir este registro no se observaron listeners
  en 4200 ni 8080. Las verificaciones anteriores no significan que los servidores
  sigan encendidos: comprobar de nuevo al iniciar.

## Cómo iniciar

Ejecutar `INICIAR.cmd` desde la carpeta principal. Conserva abiertas las ventanas
del frontend y backend. Esperar a que Angular termine de compilar y abrir
`http://localhost:4200/`.

También se pueden ejecutar por separado:

```text
inicio\iniciar-backend.cmd
inicio\iniciar-frontend.cmd
```

Un puerto ocupado puede pertenecer a otro programa. `INICIAR.cmd` solo comprueba
la ocupación; no verifica la identidad ni la salud del proceso existente.
No detener procesos sin identificar primero qué aplicación está usando el puerto.

Si aparece `ERR_CONNECTION_REFUSED`, comprobar que el frontend siga ejecutándose.
Si aparece `Port 8080 was already in use`, comprobar si el backend ya está activo.
Revisar el resultado de la terminal antes de cambiar puertos o reinstalar programas.

Para cerrar cada servidor, usar Ctrl+C en su ventana. Si faltan dependencias,
Maven o npm pueden necesitar acceso a Internet.

## Preferencias acordadas para continuar

- Cuando el usuario pida «corre el proyecto» o «iniciemos el proyecto», intentar
  iniciarlo directamente y comprobar antes si ya está funcionando.
- Verificar la página y la API; no asumir que ocupar un puerto implica que todo funciona.
- Si los permisos de la sesión bloquean el arranque, explicar el bloqueo y dar el
  paso mínimo necesario. No garantizar que todas las sesiones permitan ejecutar programas.
- El usuario prefiere una estructura sencilla, sin documentación repetida ni ejemplos innecesarios.
- Este archivo es el único registro de avance solicitado expresamente por el usuario.

## Limpieza autorizada

Se eliminaron los scripts `.ps1` de instalación y activación, los README y LEEME,
la documentación anterior y el ejercicio de PostgreSQL con Docker. Este ejercicio
usaba una base distinta (`clase_db`, puerto 5433), no la base de la aplicación.
Se conservaron código, configuración, dependencias, migraciones y scripts de inicio.

## Estado de Git

Los cambios no se han confirmado mediante commit ni enviado a un repositorio remoto.
Git muestra las correcciones en los dos archivos de código y las eliminaciones
de la documentación, README del frontend y antiguo ejercicio Docker.
También existe una carpeta `.vscode/` sin seguimiento que se conservó.

Este `AVANCE.md`, `INICIAR.cmd`, `inicio/` y `.tools/` están fuera del repositorio
anidado `s4-proyecto/.git`; un commit dentro de ese repositorio no los incluirá.

## 2026-11-17 — Markov y evaluación automática del VME

Preferencia reafirmada por el usuario: guardar todos los avances en este archivo, incluidos cambios, pruebas, hallazgos y pendientes. AVANCE.md continúa siendo el registro principal.

Se reutilizaron MarkovService y VmeService. Markov calcula ingresos y gastos por usuario, moneda y mes; clasifica ESTABLE si ingresos >= gastos y DEFICIT en caso contrario. Entrena con meses cerrados consecutivos, sin inventar probabilidades ni unir huecos. El mes actual es provisional y determina la fila para predecir el siguiente mes.

Se añadió /api/dashboard/analisis/vme/automatico, autenticado, que recibe moneda y monto, consulta Markov en el servidor y reutiliza VmeService. Valida que el monto sea positivo, con dos decimales y no supere el saldo. No mueve dinero ni acepta probabilidades del cliente. La comparación es siempre SIMULACIÓN porque FINDEX no dispone de rendimientos reales.

El Dashboard conserva el diseño oscuro y las dos tarjetas de análisis: Tu futuro financiero y ¿Qué hacer con tu dinero? Solo requiere un monto y Evaluar. No muestra matrices, fórmulas ni seis campos de resultados. Se mantiene la ruta secundaria preexistente para compatibilidad.

Cuenta utilizada: demo@finanzas.test, en desarrollo local con perfil demo-local. Ya existían 12 meses cerrados consecutivos, noviembre de 2025 a octubre de 2026. Se reutilizaron sin crear registros: 48 ingresos y 100 gastos totales. El cargador existente comprobó que las demás cuentas no cambiaron y las billeteras cuadraban.

Transiciones: ESTABLE→ESTABLE: 2; ESTABLE→DEFICIT: 4; DEFICIT→ESTABLE: 4; DEFICIT→DEFICIT: 1. Noviembre actual: ingresos 5.100 y gastos 3.075 BOB, ESTABLE provisional. Predicción: 33,33 % de estabilidad y 66,67 % de déficit.

Archivos de la implementación automática (rutas bajo s4-proyecto/):

- backend/src/main/java/com/finanzas/dto/EvaluacionAutomaticaRequest.java.
- backend/src/main/java/com/finanzas/service/AnalisisFinancieroService.java.
- backend/src/main/java/com/finanzas/controller/AnalisisFinancieroController.java.
- frontend/src/app/features/dashboard/analisis-financiero.service.ts.
- frontend/src/app/features/dashboard/analisis-financiero.component.ts y .html.
- backend/src/test/java/com/finanzas/service/AnalisisFinancieroIntegrationTest.java y EvaluacionAutomaticaTest.java.
- frontend/src/app/features/dashboard/analisis-financiero.component.spec.ts y frontend/karma.local.cjs.

Primera verificación: 65 pruebas de backend y 34 de Angular correctas; compilaciones Java/JAR y Angular de producción correctas. Prueba en navegador: acceso demo, Evaluar 500 BOB y aviso SIMULACIÓN; sin errores JavaScript ni HTTP fallidos.

## 2026-11-17 — Revisión de la recomendación Guardar y comparación sencilla

El usuario observó que cambiar el monto mantenía Guardar. Se revisó y se explicó el hallazgo antes de modificar la interfaz. No se cambiaron los escenarios ni la lógica de selección para favorecer otra alternativa.

Pagos ficticios netos para monto M:

- Guardar: 0 en ambos estados.
- Ahorrar: +0,04 × M si ESTABLE; −0,02 × M si DEFICIT.
- Invertir: +0,20 × M si ESTABLE; −0,24 × M si DEFICIT.

VME = P(ESTABLE) × monto final estable + P(DEFICIT) × monto final déficit. El resultado es monto final esperado, no ganancia. Para probabilidad de estabilidad p: Guardar = M; Ahorrar = M × (0,98 + 0,06p); Invertir = M × (0,76 + 0,44p). Como todos son proporcionales a M, cambiar un monto positivo escala los resultados y no cambia necesariamente la ganadora.

Con p = 0,333333333333 y P(DEFICIT) = 0,666666666667, para 500 BOB:

- Guardar: 500,00.
- Ahorrar: 499,999999999990 internamente; se muestra 500,00.
- Invertir: 453,333333333260 internamente; se muestra 453,33.

Con fracciones exactas 1/3 y 2/3, Guardar y Ahorrar empatan. La aproximación de Markov a 12 decimales deja Ahorrar ligeramente por debajo. El selector compara antes de redondear. No hay preferencia programada por el nombre Guardar, pero esa diferencia numérica explica la selección actual. No representa una ventaja económica real sobre Ahorrar.

Se añadió debajo de la recomendación una comparación breve de los tres montos finales esperados, sin tabla ni campos nuevos, con etiqueta SIMULACIÓN y nota de redondeo. Para 1.000 BOB muestra Guardar 1.000,00; Ahorrar 1.000,00; Invertir 906,67. Cada cálculo se redondea independientemente: no se duplica el importe ya redondeado.

Pruebas añadidas: todas las alternativas y sus pagos; escalado de 500 a 1.000; ganadoras con probabilidades de estabilidad 10 % (Guardar), 50 % (Ahorrar) y 80 % (Invertir); cambios de pagos manteniendo probabilidades; independencia del orden; empates exactos y aproximaciones a ambos lados de 1/3. Son escenarios ficticios de pruebas, sin alterar el historial de usuarios.

Archivos de esta revisión:

- s4-proyecto/frontend/src/app/features/dashboard/analisis-financiero.component.html y .scss.
- s4-proyecto/frontend/src/app/features/dashboard/analisis-financiero.component.spec.ts.
- s4-proyecto/backend/src/test/java/com/finanzas/service/EvaluacionAutomaticaTest.java.
- s4-proyecto/backend/src/test/java/com/finanzas/service/VmeServiceTest.java.
- .tools/revision/verificar-metodos.cjs: prueba real con 500 y 1.000 BOB.
- PRESENTACION_MARKOV_VME.md: guía complementaria de ejemplos; AVANCE.md es el registro principal.

Resultados: 69 pruebas de backend sin fallos ni errores; 34 pruebas Angular correctas; compilación Angular de producción correcta. Navegador real: las tres líneas coinciden con los cálculos en ambos montos, editar limpia el resultado anterior, sin errores JavaScript ni HTTP fallidos. Evidencias en inicio/revision-vme-java.log, inicio/revision-vme-angular.log, inicio/revision-vme-build.log e inicio/revision-vme-navegador.log; captura en inicio/metodos-dashboard.png. No se modificaron datos de usuarios.


## 2026-11-17 ? Movimientos hist?ricos y organizaci?n del proyecto

Se document? esta sesi?n en [docs/AVANCE_SESION_2026-11-17.md](docs/AVANCE_SESION_2026-11-17.md): restauraci?n del historial demo, paginaci?n, correcci?n de filtros opcionales en PostgreSQL, selector mensual ?nico, pruebas y explicaci?n del c?digo de Markov y VME.

La gu?a de presentaci?n se encuentra ahora en docs/PRESENTACION_MARKOV_VME.md. Los logs, capturas y reportes hist?ricos se agruparon en evidencias/. El manifiesto evidencias/organizacion.json conserva la correspondencia de las 81 rutas anteriores y nuevas. Las referencias antiguas de este registro a inicio/*.log o inicio/*.png corresponden ahora a evidencias/logs/ o evidencias/capturas/. Los logs de servidores abiertos se conservaron en inicio/.

Se a?adi? README.md en la ra?z e inicio/README.md para orientar el arranque y la ubicaci?n de archivos. Se actualizaron las rutas de salida de los scripts de revisi?n de capturas. No se cambiaron las ubicaciones de fuentes, dependencias o compilaciones.

## 2026-11-17 — Preparación para GitHub
Se incorporan Markov, VME, correcciones de pantallas y filtros, comentarios explicativos del VME y eliminación de etiquetas de demostración. Se quitaron únicamente las palabras DEMO de 144 movimientos y 3 metas en PostgreSQL local; inicio/quitar-demo-nombres.sql reproduce el ajuste. Se copian documentación y scripts al repositorio, adaptando sus rutas a backend/ y frontend/. Las herramientas, los logs y la base local no se versionan. El preparador reconoce los nombres de movimientos y metas con o sin la palabra DEMO para evitar duplicados.

Validación previa a publicación: 70 pruebas de backend y 36 pruebas de frontend aprobadas; compilación de producción de Angular aprobada durante esta sesión.
