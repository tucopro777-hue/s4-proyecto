package com.finanzas.service;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.MetaAhorroResponse;
import com.finanzas.model.Usuario;
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

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private MetaAhorroService metaAhorroService;

    private void prepararUsuarios() {
        usuarioRepository.save(new Usuario("Ana", EMAIL, "hash"));
        usuarioRepository.save(new Usuario("Luis", OTRO_EMAIL, "hash"));
    }

    private MetaAhorroResponse crearMeta(BigDecimal objetivo) {
        return metaAhorroService.crear(EMAIL, new MetaAhorroRequest(
                "Viaje", objetivo, LocalDate.of(2027, 6, 30)));
    }

    @Test
    void nuevaMetaEmpiezaEnCeroYSinCompletar() {
        prepararUsuarios();

        MetaAhorroResponse meta = crearMeta(new BigDecimal("500.00"));

        assertThat(meta.montoActual()).isEqualByComparingTo("0");
        assertThat(meta.porcentaje()).isEqualByComparingTo("0.0");
        assertThat(meta.completada()).isFalse();
    }

    @Test
    void aportesAcumulanYCompletanLaMeta() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));

        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(new BigDecimal("40.00")));
        MetaAhorroResponse tras = metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(new BigDecimal("60.00")));

        assertThat(tras.montoActual()).isEqualByComparingTo("100.00");
        assertThat(tras.porcentaje()).isEqualByComparingTo("100.0");
        assertThat(tras.completada()).isTrue();
    }

    @Test
    void actualizarNoModificaLoAcumulado() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));
        metaAhorroService.aportar(EMAIL, meta.id(), new AporteRequest(new BigDecimal("25.00")));

        MetaAhorroResponse editada = metaAhorroService.actualizar(EMAIL, meta.id(),
                new MetaAhorroRequest("Viaje a Lima", new BigDecimal("200.00"), null));

        assertThat(editada.montoActual()).isEqualByComparingTo("25.00");
        assertThat(editada.montoObjetivo()).isEqualByComparingTo("200.00");
        assertThat(editada.porcentaje()).isEqualByComparingTo("12.5");
    }

    @Test
    void noSePuedeAportarAMetaAjena() {
        prepararUsuarios();
        MetaAhorroResponse meta = crearMeta(new BigDecimal("100.00"));

        assertThatThrownBy(() -> metaAhorroService.aportar(OTRO_EMAIL, meta.id(),
                new AporteRequest(new BigDecimal("10.00"))))
                .isInstanceOfSatisfying(ResponseStatusException.class,
                        e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND));
    }
}
