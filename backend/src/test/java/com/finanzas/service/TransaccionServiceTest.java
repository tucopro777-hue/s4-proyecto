package com.finanzas.service;

import com.finanzas.dto.PresupuestoRequest;
import com.finanzas.dto.PresupuestoResponse;
import com.finanzas.dto.TransaccionRequest;
import com.finanzas.dto.TransaccionResponse;
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

/** Cada prueba se revierte al final (@Transactional), así no comparten datos. */
@SpringBootTest
@Transactional
class TransaccionServiceTest {

    private static final String EMAIL = "ana.prueba@finanzas.test";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private TransaccionService transaccionService;
    @Autowired
    private PresupuestoService presupuestoService;

    private Billetera billetera;
    private Categoria comida;

    /** Usuario con una billetera de 100 BOB, categoría de gasto y presupuesto de 50 en octubre 2026. */
    private void prepararDatos() {
        Usuario usuario = usuarioRepository.save(new Usuario("Ana", EMAIL, "hash"));
        billetera = billeteraRepository.save(new Billetera("Caja", new BigDecimal("100.00"), "BOB", usuario));
        comida = categoriaRepository.save(new Categoria("Comida", TipoCategoria.GASTO, "#FF8800", "food", usuario));
        presupuestoService.crear(EMAIL, new PresupuestoRequest(comida.getId(), new BigDecimal("50.00"), 2026, 10));
    }

    private TransaccionResponse gasto(BigDecimal monto) {
        return transaccionService.registrar(EMAIL, new TransaccionRequest(
                monto, TipoTransaccion.GASTO, "prueba", FECHA, billetera.getId(), comida.getId()));
    }

    private PresupuestoResponse presupuestoOctubre() {
        return presupuestoService.listarDeUsuario(EMAIL, 2026, 10).get(0);
    }

    private BigDecimal saldoBilletera() {
        return billeteraRepository.findById(billetera.getId()).orElseThrow().getSaldoActual();
    }

    @Test
    void gastoDescuentaSaldoYAvisaAlSuperarPresupuesto() {
        prepararDatos();

        TransaccionResponse primero = gasto(new BigDecimal("30.00"));
        assertThat(primero.alertaPresupuesto()).isNull();

        TransaccionResponse segundo = gasto(new BigDecimal("30.00"));

        assertThat(saldoBilletera()).isEqualByComparingTo("40.00");
        assertThat(segundo.alertaPresupuesto()).isNotNull();
        assertThat(segundo.alertaPresupuesto().nivel()).isEqualTo("EXCEDIDO");
        assertThat(presupuestoOctubre().saldoConsumido()).isEqualByComparingTo("60.00");
    }

    @Test
    void anularRevierteSaldoYConsumoDelPresupuesto() {
        prepararDatos();
        gasto(new BigDecimal("30.00"));
        TransaccionResponse segundo = gasto(new BigDecimal("30.00"));

        transaccionService.anular(EMAIL, segundo.id());

        assertThat(saldoBilletera()).isEqualByComparingTo("70.00");
        assertThat(presupuestoOctubre().saldoConsumido()).isEqualByComparingTo("30.00");
        assertThat(presupuestoOctubre().nivel()).isEqualTo("OK");
    }

    @Test
    void noSePuedeAnularDosVeces() {
        prepararDatos();
        TransaccionResponse movimiento = gasto(new BigDecimal("10.00"));
        transaccionService.anular(EMAIL, movimiento.id());

        assertThatThrownBy(() -> transaccionService.anular(EMAIL, movimiento.id()))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));

        assertThat(saldoBilletera()).isEqualByComparingTo("100.00");
    }

    @Test
    void gastoSinSaldoSeRechazaSinTocarNada() {
        prepararDatos();

        assertThatThrownBy(() -> gasto(new BigDecimal("500.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThat(saldoBilletera()).isEqualByComparingTo("100.00");
        assertThat(presupuestoOctubre().saldoConsumido()).isEqualByComparingTo("0.00");
    }
}
