package com.finanzas.service;

import com.finanzas.dto.AuthResponse;
import com.finanzas.dto.LoginRequest;
import com.finanzas.dto.RegistroRequest;
import com.finanzas.model.Usuario;
import com.finanzas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Cada prueba se revierte al final (@Transactional). */
@SpringBootTest
@Transactional
class AuthServiceTest {

    private static final String EMAIL = "auth.prueba@finanzas.test";
    private static final String PASSWORD = "clave-correcta-123";

    @Autowired
    private AuthService authService;
    @Autowired
    private UsuarioRepository usuarioRepository;


    private void registrarUsuario() {
        authService.registrar(new RegistroRequest("Prueba", EMAIL, PASSWORD));
    }

    private void fallarLogin(int veces) {
        for (int i = 0; i < veces; i++) {
            fallarLoginUnaVez();
        }
    }

    private void fallarLoginUnaVez() {
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "incorrecta-999")))
                .isInstanceOf(RuntimeException.class);
    }

    @Test
    void tresFallosSeguidosBloqueanLaCuentaYLaContraseñaCorrectaTambienSeRechaza() {
        registrarUsuario();

        // Los dos primeros fallos devuelven 401 normal
        for (int i = 0; i < 2; i++) {
            assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "mal")))
                    .isInstanceOf(ResponseStatusException.class)
                    .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                            .isEqualTo(HttpStatus.UNAUTHORIZED));
        }

        // El tercero bloquea la cuenta
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, "mal")))
                .isInstanceOf(CuentaBloqueadaException.class);

        // Aunque la contraseña sea la buena, sigue bloqueada
        assertThatThrownBy(() -> authService.login(new LoginRequest(EMAIL, PASSWORD)))
                .isInstanceOf(CuentaBloqueadaException.class);

        Usuario usuario = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        assertThat(usuario.getBloqueadoHasta()).isNotNull();
    }

    @Test
    void loginCorrectoAntesDelBloqueoReseteaLosFallos() {
        registrarUsuario();
        fallarLogin(2);

        AuthResponse respuesta = authService.login(new LoginRequest(EMAIL, PASSWORD));

        assertThat(respuesta.email()).isEqualTo(EMAIL);
        assertThat(usuarioRepository.findByEmail(EMAIL).orElseThrow().getIntentosFallidos()).isZero();
    }

    @Test
    void cuentaDesbloqueadaTrasExpirarElTiempoDeBloqueo() {
        registrarUsuario();
        fallarLogin(3);

        // Simulamos que el bloqueo ya caducó
        Usuario usuario = usuarioRepository.findByEmail(EMAIL).orElseThrow();
        usuario.setBloqueadoHasta(java.time.LocalDateTime.now().minusMinutes(1));

        AuthResponse respuesta = authService.login(new LoginRequest(EMAIL, PASSWORD));
        assertThat(respuesta.email()).isEqualTo(EMAIL);
    }



}
