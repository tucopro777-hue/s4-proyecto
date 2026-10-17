package com.finanzas.service;

import com.finanzas.dto.TraspasoRequest;
import com.finanzas.model.Billetera;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
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
class TraspasoServiceTest {

    private static final String EMAIL = "traspaso.prueba@finanzas.test";
    private static final String OTRO_EMAIL = "traspaso.otro@finanzas.test";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 17);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private TraspasoService traspasoService;

    private Billetera caja;
    private Billetera banco;

    private void prepararDatos() {
        Usuario ana = usuarioRepository.save(new Usuario("Ana", EMAIL, "hash"));
        Usuario luis = usuarioRepository.save(new Usuario("Luis", OTRO_EMAIL, "hash"));
        caja = billeteraRepository.save(new Billetera("Caja", new BigDecimal("100.00"), "BOB", ana));
        banco = billeteraRepository.save(new Billetera("Banco", new BigDecimal("20.00"), "BOB", ana));
        // Billetera ajena para probar el aislamiento entre usuarios
        billeteraRepository.save(new Billetera("Ajena", new BigDecimal("500.00"), "BOB", luis));
    }

    private TraspasoRequest traspaso(Billetera origen, Billetera destino, String monto) {
        return new TraspasoRequest(origen.getId(), destino.getId(), new BigDecimal(monto), "prueba", FECHA);
    }

    private BigDecimal saldo(Billetera billetera) {
        return billeteraRepository.findById(billetera.getId()).orElseThrow().getSaldoActual();
    }

    @Test
    void transferenciaMueveElSaldoEntreBilleteras() {
        prepararDatos();

        traspasoService.transferir(EMAIL, traspaso(caja, banco, "30.00"));

        assertThat(saldo(caja)).isEqualByComparingTo("70.00");
        assertThat(saldo(banco)).isEqualByComparingTo("50.00");
        assertThat(traspasoService.listarDeUsuario(EMAIL)).hasSize(1);
    }

    @Test
    void saldoInsuficienteNoModificaNada() {
        prepararDatos();

        assertThatThrownBy(() -> traspasoService.transferir(EMAIL, traspaso(caja, banco, "500.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));

        assertThat(saldo(caja)).isEqualByComparingTo("100.00");
        assertThat(saldo(banco)).isEqualByComparingTo("20.00");
        assertThat(traspasoService.listarDeUsuario(EMAIL)).isEmpty();
    }

    @Test
    void mismaBilleteraEnOrigenYDestinoSeRechaza() {
        prepararDatos();

        assertThatThrownBy(() -> traspasoService.transferir(EMAIL, traspaso(caja, caja, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(saldo(caja)).isEqualByComparingTo("100.00");
    }

    @Test
    void divisasDistintasSeRechazan() {
        prepararDatos();
        Usuario ana = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        Billetera dolares = billeteraRepository.save(new Billetera("Dolares", new BigDecimal("10.00"), "USD", ana));

        assertThatThrownBy(() -> traspasoService.transferir(EMAIL, traspaso(caja, dolares, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(saldo(caja)).isEqualByComparingTo("100.00");
        assertThat(saldo(dolares)).isEqualByComparingTo("10.00");
    }

    @Test
    void noSePuedeTransferirDesdeBilleteraAjena() {
        prepararDatos();
        Billetera ajena = billeteraRepository.findAll().stream()
                .filter(b -> b.getNombre().equals("Ajena"))
                .findFirst().orElseThrow();

        assertThatThrownBy(() -> traspasoService.transferir(EMAIL, traspaso(ajena, caja, "10.00")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));

        assertThat(saldo(ajena)).isEqualByComparingTo("500.00");
        assertThat(saldo(caja)).isEqualByComparingTo("100.00");
    }

    @Test
    void traspasosEnAmbosSentidosSeAplicanEnOrden() {
        prepararDatos();

        traspasoService.transferir(EMAIL, traspaso(caja, banco, "40.00"));
        traspasoService.transferir(EMAIL, traspaso(banco, caja, "15.00"));

        assertThat(saldo(caja)).isEqualByComparingTo("75.00");
        assertThat(saldo(banco)).isEqualByComparingTo("45.00");
    }
}
