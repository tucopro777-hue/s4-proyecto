package com.finanzas.config;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.BilleteraRequest;
import com.finanzas.dto.CategoriaRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.PresupuestoRequest;
import com.finanzas.dto.RegistroRequest;
import com.finanzas.dto.TraspasoRequest;
import com.finanzas.dto.TransaccionRequest;
import com.finanzas.model.TipoCategoria;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.repository.UsuarioRepository;
import com.finanzas.service.AuthService;
import com.finanzas.service.BilleteraService;
import com.finanzas.service.CategoriaService;
import com.finanzas.service.MetaAhorroService;
import com.finanzas.service.PresupuestoService;
import com.finanzas.service.TraspasoService;
import com.finanzas.service.TransaccionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Crea un usuario de demostración con billeteras, categorías y movimientos de ejemplo.
 *
 * Se ejecuta solo si app.demo.enabled=true y si el usuario aún no existe, así que es seguro
 * arrancar la aplicación varias veces.  Todo corre en una sola transacción:
 * si algo falla, no queda un usuario a medio crear.
 *
 * Credenciales de demo: demo@finanzas.test / Demo1234.
 */
@Component
@ConditionalOnProperty(name = "app.demo.enabled", havingValue = "true")
public class DatosDemoInicializador implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosDemoInicializador.class);
    private static final String EMAIL = "demo@finanzas.test";
    private static final String PASSWORD = "Demo1234";

    private final AuthService authService;
    private final BilleteraService billeteraService;
    private final CategoriaService categoriaService;
    private final TransaccionService transaccionService;
    private final PresupuestoService presupuestoService;
    private final TraspasoService traspasoService;
    private final MetaAhorroService metaAhorroService;
    private final UsuarioRepository usuarioRepository;
    private final TransactionTemplate transactionTemplate;

    public DatosDemoInicializador(AuthService authService,
                                  BilleteraService billeteraService,
                                  CategoriaService categoriaService,
                                  TransaccionService transaccionService,
                                  PresupuestoService presupuestoService,
                                  TraspasoService traspasoService,
                                  MetaAhorroService metaAhorroService,
                                  UsuarioRepository usuarioRepository,
                                  PlatformTransactionManager transactionManager) {
        this.authService = authService;
        this.billeteraService = billeteraService;
        this.categoriaService = categoriaService;
        this.transaccionService = transaccionService;
        this.presupuestoService = presupuestoService;
        this.traspasoService = traspasoService;
        this.metaAhorroService = metaAhorroService;
        this.usuarioRepository = usuarioRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public void run(ApplicationArguments args) {
        if (usuarioRepository.existsByEmail(EMAIL)) {
            log.info("Datos de demo ya presentes ({}); no se vuelven a crear", EMAIL);
            return;
        }

        transactionTemplate.executeWithoutResult(estado -> sembrar());
        log.info("Datos de demo creados: {} / {}", EMAIL, PASSWORD);
    }

    private void sembrar() {
        authService.registrar(new RegistroRequest("Usuario Demo", EMAIL, PASSWORD));

        // Billeteras en dos divisas (BOB y USD) para probar traspasos y saldos por divisa
        var cajaBob = billeteraService.crear(EMAIL, new BilleteraRequest("Caja BOB", new BigDecimal("500.00"), "BOB"));
        var bancoBob = billeteraService.crear(EMAIL, new BilleteraRequest("Banco BOB", new BigDecimal("2000.00"), "BOB"));
        billeteraService.crear(EMAIL, new BilleteraRequest("Cuenta USD", new BigDecimal("300.00"), "USD"));

        var comida = categoriaService.crear(EMAIL, new CategoriaRequest("Comida", TipoCategoria.GASTO, "#FF8800", "food"));
        var transporte = categoriaService.crear(EMAIL, new CategoriaRequest("Transporte", TipoCategoria.GASTO, "#3B82F6", "transport"));
        var servicios = categoriaService.crear(EMAIL, new CategoriaRequest("Servicios", TipoCategoria.GASTO, "#A855F7", "bills"));
        var salario = categoriaService.crear(EMAIL, new CategoriaRequest("Salario", TipoCategoria.INGRESO, "#0ECB81", "salary"));

        LocalDate hoy = LocalDate.now();

        // Movimientos del mes: el salario entra al banco y los gastos salen de ambas cuentas
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("2000.00"), TipoTransaccion.INGRESO, "Salario mensual",
                hoy.withDayOfMonth(1), bancoBob.id(), salario.id()));
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("120.00"), TipoTransaccion.GASTO, "Almuerzos de la semana",
                hoy.withDayOfMonth(1), cajaBob.id(), comida.id()));
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("40.00"), TipoTransaccion.GASTO, "Pasajes",
                hoy.withDayOfMonth(1), cajaBob.id(), transporte.id()));
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("150.00"), TipoTransaccion.GASTO, "Internet",
                hoy.withDayOfMonth(1), bancoBob.id(), servicios.id()));

        // Presupuesto del mes sobre la categoría de comida (el consumo se calcula con los gastos ya registrados)
        presupuestoService.crear(EMAIL, new PresupuestoRequest(comida.id(), new BigDecimal("600.00"),
                hoy.getYear(), hoy.getMonthValue()));

        // Traspaso de ejemplo entre dos billeteras BOB
        traspasoService.transferir(EMAIL, new TraspasoRequest(
                bancoBob.id(), cajaBob.id(), new BigDecimal("200.00"), "Fondo para gastos", hoy.withDayOfMonth(1)));

        // Meta de ahorro con un primer aporte
        var meta = metaAhorroService.crear(EMAIL, new MetaAhorroRequest(
                "Viaje a Lima", new BigDecimal("3000.00"), hoy.plusMonths(6), new BigDecimal("10.00")));
        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(bancoBob.id(), new BigDecimal("500.00")));
    }
}
