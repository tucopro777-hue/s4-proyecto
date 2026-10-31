# Persona 3 — Transacciones con filtros avanzados

Proyecto de Finanzas · Spring Boot 3.3.5 + Angular 17 (standalone) · Paquete base: `com.finanzas`

---

## 1. Código completo de los archivos afectados

### 1.1 Backend — `TransaccionController.java`

```java
package com.finanzas.controller;

import com.finanzas.dto.TransaccionRequest;
import com.finanzas.dto.TransaccionResponse;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.service.TransaccionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    /**
     * Lista los movimientos del usuario autenticado.
     * Todos los filtros son OPCIONALES y se combinan entre sí (AND):
     * - desde / hasta: rango de fechas inclusive en formato yyyy-MM-dd.
     * - categoriaId: id de la categoría del movimiento.
     * - tipo: INGRESO o GASTO.
     * Si no se envía ningún filtro, la respuesta es el listado predeterminado
     * (movimientos no anulados, ordenados por fecha descendente).
     */
    @GetMapping
    public ResponseEntity<List<TransaccionResponse>> listarDelUsuario(
            Authentication autenticacion,
            @RequestParam(required = false) LocalDate desde,
            @RequestParam(required = false) LocalDate hasta,
            @RequestParam(required = false) Long categoriaId,
            @RequestParam(required = false) TipoTransaccion tipo) {
        return ResponseEntity.ok(
                transaccionService.listarConFiltros(autenticacion.getName(), desde, hasta, categoriaId, tipo));
    }

    @PostMapping
    public ResponseEntity<TransaccionResponse> registrar(Authentication autenticacion,
                                                         @Valid @RequestBody TransaccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.registrar(autenticacion.getName(), request));
    }

    /** Anulación lógica: revierte el saldo de la billetera y el consumo del presupuesto. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(Authentication autenticacion, @PathVariable Long id) {
        transaccionService.anular(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
```

### 1.2 Backend — `TransaccionService.java` (métodos nuevos/modificados)

```java
    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarDeUsuario(String emailUsuario) {
        // Sin filtros: se reutiliza el listado predeterminado original
        return listarConFiltros(emailUsuario, null, null, null, null);
    }

    /**
     * Lista los movimientos del usuario aplicando los filtros opcionales recibidos.
     * Los filtros se combinan entre sí (AND) y SIEMPRE se excluyen las anuladas.
     * Si no llega ningún filtro, se devuelve el listado predeterminado completo.
     */
    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarConFiltros(String emailUsuario,
                                                      LocalDate desde,
                                                      LocalDate hasta,
                                                      Long categoriaId,
                                                      TipoTransaccion tipo) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return transaccionRepository
                .buscarConFiltros(usuario.getId(), desde, hasta, categoriaId, tipo)
                .stream()
                .map(transaccion -> toResponse(transaccion, null))
                .toList();
    }
```

> El resto de la clase (`registrar`, `anular`, `buscarUsuario`, `toResponse`) **no se modificó**.

### 1.3 Backend — `TransaccionRepository.java` (método nuevo)

```java
    /**
     * Consulta NUEVA para el listado con filtros avanzados (Persona 3).
     *
     * IMPORTANTE sobre nulos en PostgreSQL + JPQL:
     * Se usa el patrón "(:param IS NULL OR columna = :param)" y no COALESCE sobre la columna.
     * Con COALESCE(:desde, t.fecha) el optimizador de PostgreSQL puede generar un plan que
     * ignore los índices y, además, si el parámetro llega sin tipo en Hibernate 6 contra
     * PostgreSQL se produce el error "could not determine data type of parameter".
     * Con la forma "IS NULL OR ..." Hibernate tipa cada parámetro explícitamente
     * (LocalDate, Long, enum) y PostgreSQL no falla.
     *
     * Los filtros se combinan entre sí (AND) y SIEMPRE se excluyen las anuladas.
     * Sin filtros, equivale exactamente al listado predeterminado.
     */
    @Query("select t from Transaccion t "
            + "where t.usuario.id = :usuarioId "
            + "and t.anulada = false "
            + "and (:desde IS NULL OR t.fecha >= :desde) "
            + "and (:hasta IS NULL OR t.fecha <= :hasta) "
            + "and (:categoriaId IS NULL OR t.categoria.id = :categoriaId) "
            + "and (:tipo IS NULL OR t.tipo = :tipo) "
            + "order by t.fecha desc, t.id desc")
    List<Transaccion> buscarConFiltros(@Param("usuarioId") Long usuarioId,
                                       @Param("desde") LocalDate desde,
                                       @Param("hasta") LocalDate hasta,
                                       @Param("categoriaId") Long categoriaId,
                                       @Param("tipo") TipoTransaccion tipo);
```

> Los métodos existentes (`findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc`,
> `bloquearDeUsuario`, `sumarMovimientos`) **no se modificaron ni se eliminaron**.

### 1.4 Frontend — `transaccion.service.ts` (método nuevo)

```typescript
/** Filtros opcionales para el listado de movimientos (Persona 3). */
export interface FiltrosTransaccion {
  /** Fecha inicial del rango, yyyy-MM-dd. */
  desde?: string | null;
  /** Fecha final del rango, yyyy-MM-dd. */
  hasta?: string | null;
  categoriaId?: number | null;
  tipo?: TipoTransaccion | null;
}

  /**
   * Lista movimientos con filtros avanzados. Los parámetros vacíos, nulos o
   * indefinidos se OMITEN del HttpParams para que el backend aplique el listado
   * completo o solo los filtros activos.
   */
  listarConFiltros(filtros: FiltrosTransaccion): Observable<Transaccion[]> {
    let params = new HttpParams();
    if (filtros.desde) {
      params = params.set('desde', filtros.desde);
    }
    if (filtros.hasta) {
      params = params.set('hasta', filtros.hasta);
    }
    if (filtros.categoriaId != null) {
      params = params.set('categoriaId', String(filtros.categoriaId));
    }
    if (filtros.tipo) {
      params = params.set('tipo', filtros.tipo);
    }
    return this.http.get<Transaccion[]>(this.apiUrl, { params });
  }
```

### 1.5 Frontend — `transacciones.component.ts` (nuevos estados y métodos)

```typescript
  /** Mensaje informativo cuando el filtro no devuelve resultados. */
  sinResultados = false;
  /** Indica si hay filtros activos (para distinguir "sin datos" de "sin resultados con filtros"). */
  filtrosActivos = false;

  /** Formulario independiente del panel de filtros avanzados. */
  readonly filtrosForm = this.fb.nonNullable.group({
    desde: [''],
    hasta: [''],
    categoriaId: [null as number | null],
    tipo: ['' as TipoTransaccion | ''],
  });

  /** Carga el historial respetando los filtros activos del panel. */
  private cargarTransacciones(): void {
    this.cargando = true;
    this.error = null;

    const peticion = this.filtrosActivos
      ? this.transaccionService.listarConFiltros(this.filtrosForm.value as FiltrosTransaccion)
      : this.transaccionService.listar();

    peticion
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (transacciones) => {
          this.transacciones = transacciones;
          // Lista vacía CON filtros activos: mensaje informativo en pantalla
          this.sinResultados = this.filtrosActivos && transacciones.length === 0;
          this.cargando = false;
        },
        error: (err: HttpErrorResponse) => {
          this.cargando = false;
          this.error = mensajeErrorTransaccion(err);
        },
      });
  }

  /** Aplica los filtros del panel y recarga el historial. */
  buscar(): void {
    const valores = this.filtrosForm.getRawValue();
    // Solo marca como "filtros activos" si realmente se envió algún criterio
    this.filtrosActivos = Boolean(valores.desde || valores.hasta || valores.categoriaId || valores.tipo);
    this.cargarTransacciones();
  }

  /** Resetea el panel y vuelve al listado general predeterminado. */
  limpiar(): void {
    this.filtrosForm.reset({ desde: '', hasta: '', categoriaId: null, tipo: '' });
    this.filtrosActivos = false;
    this.sinResultados = false;
    this.cargarTransacciones();
  }
```

Además, `cargarTodo()` ahora solo recarga billeteras y categorías; el historial se carga con
`cargarTransacciones()`, que respeta los filtros vigentes (así, tras registrar o anular un
movimiento, el resultado filtrado se mantiene consistente).

### 1.6 Frontend — `transacciones.component.html` (panel de búsqueda)

```html
<!-- Panel de búsqueda con filtros avanzados (Persona 3) -->
<form
  class="panel-filtros"
  role="search"
  aria-label="Filtros de búsqueda de movimientos"
  [formGroup]="filtrosForm"
  (ngSubmit)="buscar()"
  novalidate
>
  <div class="campo">
    <label for="filtro-desde">Desde</label>
    <input id="filtro-desde" type="date" formControlName="desde" aria-describedby="ayuda-filtros" />
  </div>

  <div class="campo">
    <label for="filtro-hasta">Hasta</label>
    <input id="filtro-hasta" type="date" formControlName="hasta" />
  </div>

  <div class="campo">
    <label for="filtro-categoria">Categoría</label>
    <select id="filtro-categoria" formControlName="categoriaId">
      <option [ngValue]="null">Todas</option>
      @for (categoria of categorias; track categoria.id) {
        <option [ngValue]="categoria.id">{{ categoria.nombre }}</option>
      }
    </select>
  </div>

  <div class="campo">
    <label for="filtro-tipo">Tipo</label>
    <select id="filtro-tipo" formControlName="tipo">
      <option value="">Todos</option>
      <option value="INGRESO">Ingreso</option>
      <option value="GASTO">Gasto</option>
    </select>
  </div>

  <div class="acciones-filtros">
    <button type="submit" class="btn-primary" [disabled]="cargando">Buscar</button>
    <button type="button" class="btn-secundario" (click)="limpiar()" [disabled]="cargando">Limpiar</button>
  </div>
  <small id="ayuda-filtros" class="ayuda">Los filtros se combinan entre sí. Puedes dejar campos vacíos.</small>
</form>

@if (cargando) {
  <p class="estado">Cargando movimientos...</p>
} @else if (sinResultados) {
  <p class="estado" role="status">No hay movimientos con esos filtros.</p>
} @else {
  <!-- listado existente, sin cambios -->
}
```

También se añadió el estilo `.panel-filtros` y `.btn-secundario` en
`transacciones.component.scss`.

### 1.7 Frontend — Manejo global de errores HTTP (`error.interceptor.ts`, NUEVO)

Se añadió un interceptor funcional en `frontend/src/app/core/interceptors/error.interceptor.ts`
y se registró en `app.config.ts` después del interceptor de autenticación:

```typescript
provideHttpClient(withInterceptors([authInterceptor, errorInterceptor])),
```

El interceptor traduce cualquier `HttpErrorResponse` a un mensaje amigable según su estado
(`0` = sin conexión, `400`, `401`, `403`, `404`, `409`, `422`, `500`...) y lo adjunta en
`err.error.mensaje`, para que todos los componentes muestren el mismo formato sin repetir
lógica de `switch`.

### 1.8 Frontend — Estado de conexión en `transacciones.component.ts`

```typescript
  /** true si la última petición falló por conexión (backend caído o red). */
  errorConexion = false;

  /**
   * Punto único para registrar errores HTTP en el componente:
   * distingue fallo de conexión (status 0) de errores de negocio y evita
   * que una alerta antigua persista cuando una llamada posterior tiene éxito.
   */
  private registrarError(err: HttpErrorResponse): void {
    const mensaje = typeof err.error === 'string' ? err.error : mensajeErrorTransaccion(err);
    this.errorConexion = err.status === 0;
    this.error = mensaje;
  }
```

Además, tanto `cargarTodo()` como `cargarTransacciones()` ponen
`this.errorConexion = false` en el bloque `next`, de modo que la alerta
"No se pudo conectar..." **se oculta automáticamente** en cuanto el backend
vuelve a responder (al pulsar Buscar, Limpiar o al registrar/anular). Cuando hay error de
conexión, la alerta muestra además un botón **Reintentar** que relanza la carga.

---

## 2. Explicación paso a paso (qué se cambió y por qué)

1. **Controller** — Se declararon los cuatro parámetros con `@RequestParam(required = false)`:
   `desde` y `hasta` se enlazan automáticamente a `LocalDate` gracias a la conversión ISO
   `yyyy-MM-dd` de Spring, y `tipo` al enum `TipoTransaccion`. Son opcionales, así que
   llamar a `GET /api/transacciones` sin parámetros sigue funcionando exactamente como antes.
2. **Service** — Se creó `listarConFiltros(...)`, que valida el usuario por el token y delega al
   repositorio. `listarDeUsuario(...)` (el método original que usaban registrar/anular) ahora
   delega en `listarConFiltros` con nulos, garantizando el **mismo comportamiento
   predeterminado** con una sola fuente de verdad.
3. **Repository** — Consulta JPQL nueva `buscarConFiltros`. Cada filtro usa
   `(:param IS NULL OR columna = :param)`: si el parámetro llega nulo, la condición se anula;
   si llega, filtra. Los filtros se combinan con AND y `t.anulada = false` está **fuera** de
   cualquier condición, por lo que las anuladas nunca aparecen.
   **Riesgo resuelto con nulos en PostgreSQL:** con `COALESCE(:desde, t.fecha)` PostgreSQL
   puede fallar con *"could not determine data type of parameter"* cuando el parámetro nulo
   llega sin tipo en Hibernate 6, y además impide usar índices. El patrón `IS NULL OR` tipa
   cada parámetro y evita ambos problemas (comentario dejado en el código).
4. **Servicio Angular** — Nuevo `listarConFiltros()` que construye `HttpParams` **omitiendo**
   los campos vacíos, nulos o indefinidos (`if (filtros.desde)`, `!= null`, etc.), para no
   enviar cadenas vacías que el backend interpretaría mal. `listar()` se conserva intacto.
5. **Componente** — Se añadió un formulario reactivo independiente (`filtrosForm`) para no
   tocar el formulario de registro, más los estados `sinResultados` y `filtrosActivos`.
   - `buscar()`: marca filtros activos solo si hay algún criterio y recarga el historial.
   - `limpiar()`: resetea el panel y vuelve a `transaccionService.listar()` (listado general
     predeterminado).
   - `cargarTransacciones()`: elige el método del servicio según si hay filtros; si el
     resultado viene vacío **con** filtros, activa `sinResultados` para mostrar
     *"No hay movimientos con esos filtros"*.
6. **Plantilla** — Panel accesible (`role="search"`, `aria-label`, labels con `for`) arriba de
   la lista, con fechas Desde/Hasta, select de categoría cargado de `this.categorias`
   (dinámico), select de tipo (Todos/Ingreso/Gasto) y botones Buscar/Limpiar. El mensaje
   vacío del `@empty` ahora solo se muestra cuando no hay filtros activos, para no duplicar
   el aviso.
7. **No se alteró** ninguna lógica de crear, editar ni anular transacciones: `registrar` y
   `anular` siguen usando los mismos servicios y `cargarTodo()` recarga billeteras/categorías
   y luego el historial filtrado vigente.
8. **Robustez / calidad del frontend (manejo de errores):** se añadió un interceptor global
   de errores HTTP (`error.interceptor.ts`) que traduce cada `HttpErrorResponse` a un mensaje
   amigable (incluyendo `status 0` / `ERR_CONNECTION_REFUSED` y `500`). El componente de
   transacciones centraliza la gestión en `registrarError()`, que marca `errorConexion = true`
   solo cuando el fallo es de conexión. La alerta **nunca persiste**: se borra al iniciar
   cualquier nueva llamada y se oculta automáticamente cuando la petición tiene éxito
   (`errorConexion = false` en el bloque `next`). Además, la alerta de conexión incluye un
   botón "Reintentar" para volver a cargar sin salir de la pantalla.

---

## 3. Resumen para la defensa (guion orientado al docente)

> *"Buenos días. Esta es mi implementación de los filtros avanzados en transacciones.*
>
> *En el **backend**, amplié el endpoint `GET /api/transacciones` con cuatro parámetros
> opcionales: `desde`, `hasta`, `categoriaId` y `tipo`. Usé `@RequestParam(required = false)`
> para que el mismo endpoint sirva tanto el listado general como la búsqueda filtrada. La
> consulta nueva vive en el repositorio con JPQL y el patrón `(:param IS NULL OR ...)`:
> cada filtro solo se aplica si llega un valor, se combinan entre sí con AND, y la condición
> `anulada = false` está fija, de modo que las transacciones anuladas jamás aparecen, con o
> sin filtros. Elegí ese patrón en lugar de `COALESCE` porque en PostgreSQL un parámetro
> nulo sin tipo en Hibernate 6 lanza el error 'could not determine data type of parameter' y
> `COALESCE` además degrada el uso de índices.*
>
> *En el **frontend**, construí un panel de búsqueda accesible con Reactive Forms:
> dos fechas, un select de categorías cargado dinámicamente y un select de tipo. El servicio
> envía solo los filtros activos vía `HttpParams`, omitiendo vacíos y nulos. Al pulsar
> 'Buscar' se recarga el historial con los filtros combinados; si no hay resultados, se
> muestra 'No hay movimientos con esos filtros'; y al pulsar 'Limpiar' se resetea el panel y
> se vuelve al listado predeterminado. La creación y anulación de movimientos no sufrieron
> ningún cambio: tras operar, la lista se refresca respetando los filtros vigentes.*
>
> *En síntesis: un solo endpoint reutilizable, consultas seguras contra nulos en PostgreSQL,
> UI accesible y comportamiento predeterminado 100 % preservado.*
>
> *Además, el frontend es tolerante a fallos: un interceptor global traduce los errores HTTP
> (incluida la caída de conexión con status 0) a mensajes amigables, la alerta se oculta
> sola cuando el backend vuelve a responder, y ofrezco un botón Reintentar para recuperarse
> sin recargar la página.*"
