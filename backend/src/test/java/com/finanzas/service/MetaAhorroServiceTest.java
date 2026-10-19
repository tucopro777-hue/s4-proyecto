package com.finanzas.service;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.MetaAhorroResponse;
import com.finanzas.dto.TransaccionRequest;
import com.finanzas.model.Billetera;
import com.finanzas.model.Categoria;
import com.finanzas.model.TipoCategoria;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada prueba se revierte al final (@Transactional). */
@SpringBootTest
@Transactional
class MetaAhorroServiceTest {

    private static final String EMAIL = "meta.prueba@finanzas.test";
    private static final String OTRO_EMAIL = "otro.prueba@finanzas.test";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private MetaAhorroService metaAhorroService;
    @Autowired
    private TransaccionService transaccionService;

    private Billetera billetera;
    private Categoria salario;

    /** Usuario con una billetera de 500 BOB y una categoría de ingreso. */
    private void prepararUsuarios() {
        Usuario ana = usuarioRepository.save(new Usuario("Ana", EMAIL, "hash"));
        usuarioRepository.save(new Usuario("Luis", OTRO_EMAIL, "hash"));
        billetera = billeteraRepository.save(new Billetera("Caja", new BigDecimal("500.00"), "BOB", ana));
        salario = categoriaRepository.save(new Categoria("Salario", TipoCategoria.INGRESO, "#0ECB81", "salary", ana));
    }

    private MetaAhorroResponse crearMeta(BigDecimal objetivo) {
        return crearMeta(objetivo, null);
    }

    private MetaAhorroResponse crearMeta(BigDecimal objetivo, BigDecimal porcentajeAutomatico) {
        return metaAhorroService.crear(EMAIL, new MetaAhorroRequest(
                "Viaje", objetivo, LocalDate.of(2027, 6, 30), porcentajeAutomatico));
    }

    private void ingreso(BigDecimal monto) {
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                monto, TipoTransaccion.INGRESO, "Sueldo", FECHA, billetera.getId(), salario.getId()));
    }

    private BigDecimal saldoBilletera() {
        return billeteraRepository.findById(billetera.getId()).orElseThrow().getSaldoActual();
    }

    @Test
    void nuevaMetaEmpiezaEnCeroYSinCompletar() {
        prepararUsuarios();

        MetaAhorroResponse meta = crearMeta(new BigDecimal("500.00"));

        assertThat(meta.montoActual()).isEqualByComparingTo("0");
        assertThat(meta.porcentaje()).isEqualByComparingTo("0.0");
        assertThat(meta.completada()).isFalse();
        assertThat(meta.porcentajeAutomatico()).isNull();
    }

    @Test
    void aportesSaleDeLaBilleteraYCompletanLaMeta() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));

        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(billetera.getId(), new BigDecimal("40.00")));
        MetaAhorroResponse tras = metaAhorroService.aportar(EMAIL, meta.id(),
                new AporteRequest(billetera.getId(), new BigDecimal("60.00")));

        assertThat(tras.montoActual()).isEqualByComparingTo("100.00");
        assertThat(tras.porcentaje()).isEqualByComparingTo("100.0");
        assertThat(tras.completada()).isTrue();
        assertThat(saldoBilletera()).isEqualByComparingTo("400.00");
    }

    @Test
    void aporteMayorAlSaldoNoCambiaNada() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("1000.00"));

        assertThatThrownBy(() -> metaAhorroService.aportar(EMAIL, meta.id(),
                new AporteRequest(billetera.getId(), new BigDecimal("600.00"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThat(saldoBilletera()).isEqualByComparingTo("500.00");
        assertThat(metaAhorroService.listarDeUsuario(EMAIL).get(0).montoActual()).isEqualByComparingTo("0");
    }

    @Test
    void actualizarNoModificaLoAcumulado() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));
        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(billetera.getId(), new BigDecimal("25.00")));

        MetaAhorroResponse editada = metaAhorroService.actualizar(EMAIL, meta.id(),
                new MetaAhorroRequest("Viaje a Lima", new BigDecimal("200.00"), null, null));

        assertThat(editada.montoActual()).isEqualByComparingTo("25.00");
        assertThat(editada.montoObjetivo()).isEqualByComparingTo("200.00");
        assertThat(editada.porcentaje()).isEqualByComparingTo("12.5");
    }

    @Test
    void ingresoConPorcentajeAutomaticoAportaLaParteAMeta() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("1000.00"), new BigDecimal("10"));

        ingreso(new BigDecimal("200.00"));

        MetaAhorroResponse tras = metaAhorroService.listarDeUsuario(EMAIL).get(0);
        assertThat(tras.id()).isEqualTo(meta.id());
        assertThat(tras.montoActual()).isEqualByComparingTo("20.00");
        // 500 + 200 del ingreso - 20 que pasaron a la meta
        assertThat(saldoBilletera()).isEqualByComparingTo("680.00");
    }

    @Test
    void sumaDePorcentajesNoPuedeSuperarCien() {
        prepararUsuarios();
        crearMeta(new BigDecimal("1000.00"), new BigDecimal("60"));

        assertThatThrownBy(() -> crearMeta(new BigDecimal("500.00"), new BigDecimal("50")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    void anularIngresoDevuelveSusAportesAutomaticos() {
        prepararUsuarios();
        crearMeta(new BigDecimal("1000.00"), new BigDecimal("10"));
        ingreso(new BigDecimal("200.00"));
        Long idIngreso = transaccionService.listarDeUsuario(EMAIL).get(0).id();

        transaccionService.anular(EMAIL, idIngreso);

        assertThat(saldoBilletera()).isEqualByComparingTo("500.00");
        assertThat(metaAhorroService.listarDeUsuario(EMAIL).get(0).montoActual()).isEqualByComparingTo("0");
    }

    @Test
    void eliminarMetaDevuelveLoAportadoALaBilletera() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("1000.00"));
        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(billetera.getId(), new BigDecimal("150.00")));

        metaAhorroService.eliminar(EMAIL, meta.id());

        assertThat(saldoBilletera()).isEqualByComparingTo("500.00");
        assertThat(metaAhorroService.listarDeUsuario(EMAIL)).isEmpty();
    }

    @Test
    void noSePuedeAportarAMetaAjena() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));

        assertThatThrownBy(() -> metaAhorroService.aportar(OTRO_EMAIL, meta.id(),
                new AporteRequest(billetera.getId(), new BigDecimal("10.00"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }
}
