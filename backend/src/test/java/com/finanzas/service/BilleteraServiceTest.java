package com.finanzas.service;

import com.finanzas.dto.BilleteraRequest;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada prueba se revierte al final (@Transactional). */
@SpringBootTest
@Transactional
class BilleteraServiceTest {

    private static final String EMAIL = "billetera.prueba@finanzas.test";
    private static final String PASSWORD = "Clave12345";

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private BilleteraService billeteraService;
    @Autowired
    private PasswordEncoder passwordEncoder;

    private void prepararUsuario() {
        usuarioRepository.save(new Usuario("Ana", EMAIL, passwordEncoder.encode(PASSWORD)));
    }

    private BilleteraRequest pedido(String nombre) {
        return new BilleteraRequest(nombre, new BigDecimal("100.00"), "BOB");
    }

    @Test
    void noPermiteDosBilleterasActivasConElMismoNombre() {
        prepararUsuario();
        billeteraService.crear(EMAIL, pedido("Caja"));

        assertThatThrownBy(() -> billeteraService.crear(EMAIL, pedido("  caja  ")))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT));
    }

    @Test
    void permiteReutilizarNombreDeUnaBilleteraDesactivada() {
        prepararUsuario();
        var caja = billeteraService.crear(EMAIL, pedido("Caja"));
        billeteraService.desactivar(EMAIL, caja.id(), PASSWORD);

        var nueva = billeteraService.crear(EMAIL, pedido("Caja"));

        assertThat(nueva.activa()).isTrue();
    }

    @Test
    void desactivarRequiereLaContrasenaDelUsuario() {
        prepararUsuario();
        var caja = billeteraService.crear(EMAIL, pedido("Caja"));

        assertThatThrownBy(() -> billeteraService.desactivar(EMAIL, caja.id(), "otra-clave"))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(billeteraRepository.findById(caja.id()).orElseThrow().isActiva()).isTrue();

        billeteraService.desactivar(EMAIL, caja.id(), PASSWORD);
        assertThat(billeteraRepository.findById(caja.id()).orElseThrow().isActiva()).isFalse();
    }
}
