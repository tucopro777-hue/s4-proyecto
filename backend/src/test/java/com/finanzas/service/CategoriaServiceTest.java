package com.finanzas.service;

import com.finanzas.dto.CategoriaRequest;
import com.finanzas.model.*;
import com.finanzas.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Propagation;
import java.util.concurrent.*;
import java.util.UUID;
import org.springframework.web.server.ResponseStatusException;
import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Transactional
class CategoriaServiceTest {
    @Autowired CategoriaService service;
    @Autowired UsuarioRepository usuarios;
    @Autowired CategoriaRepository categorias;
    private static final String EMAIL = "categoria.prueba@finanzas.test";
    private void preparar() { usuarios.save(new Usuario("Ana", EMAIL, "hash-prueba")); }
    private CategoriaRequest pedido(String nombre) { return new CategoriaRequest(nombre, TipoCategoria.GASTO, "#FF8800", "food"); }

    @Test void rechazaDuplicadoConLetraExtraEIdentificaLaCategoria() {
        preparar(); service.crear(EMAIL, pedido("Comida"));
        assertThatThrownBy(() -> service.crear(EMAIL, pedido("Comidaa")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> {
                    assertThat(e.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(e.getReason()).contains("\"Comida\"");
                });
        assertThat(categorias.findByUsuarioIdOrderByNombreAsc(usuarios.findByEmail(EMAIL).orElseThrow().getId())).hasSize(1);
    }

    @Test void rechazaDiferenciasDeTildesYEspacios() {
        preparar(); service.crear(EMAIL, pedido("Educación"));
        assertThatThrownBy(() -> service.crear(EMAIL, pedido(" EDU CACION "))).isInstanceOf(ResponseStatusException.class);
    }

    @Test void incluyeCategoriasInactivasSinBorrarElHistorial() {
        preparar(); var c = service.crear(EMAIL, pedido("Transporte")); service.desactivar(EMAIL, c.id());
        assertThatThrownBy(() -> service.crear(EMAIL, pedido("Transportee")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getReason()).contains("desactivada"));
        assertThat(categorias.findById(c.id()).orElseThrow().isActiva()).isFalse();
    }

    @Test void permiteElMismoNombreEnOtroUsuarioYNombresEspecificosEnElMismo() {
        preparar(); usuarios.save(new Usuario("Luis", "categoria.otro@finanzas.test", "hash-prueba"));
        service.crear(EMAIL, pedido("Comida"));
        assertThat(service.crear("categoria.otro@finanzas.test", pedido("Comida")).nombre()).isEqualTo("Comida");
        assertThat(service.crear(EMAIL, pedido("Comida trabajo")).nombre()).isEqualTo("Comida trabajo");
    }

    @Test void conservaNombreLegibleYNoReutilizaElMismoNombreParaOtroTipo() {
        preparar();
        assertThat(service.crear(EMAIL, pedido("  Comida\u00a0  rápida ")).nombre()).isEqualTo("Comida rápida");
        assertThatThrownBy(() -> service.crear(EMAIL, new CategoriaRequest("comida rapida", TipoCategoria.INGRESO, "#FF8800", "food")))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test void rechazaNombreQueSoloContieneEspaciosOCaracteresInvisibles() {
        preparar();
        assertThatThrownBy(() -> service.crear(EMAIL, pedido("\u00a0\u200b")))
                .isInstanceOfSatisfying(ResponseStatusException.class, e -> assertThat(e.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST));
    }

    @Test
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    void dosSolicitudesSimultaneasNoCreanVariantesDuplicadas() throws Exception {
        String email = "categoria.concurrente." + UUID.randomUUID() + "@finanzas.test";
        var usuario = usuarios.save(new Usuario("Concurrencia", email, "hash-prueba"));
        var pool = Executors.newFixedThreadPool(2);
        var inicio = new CountDownLatch(1);
        try {
            Callable<Integer> primera = () -> crearConcurrente(email, "Comida", inicio);
            Callable<Integer> segunda = () -> crearConcurrente(email, "Comidaa", inicio);
            var a = pool.submit(primera); var b = pool.submit(segunda); inicio.countDown();
            assertThat(java.util.List.of(a.get(30, TimeUnit.SECONDS), b.get(30, TimeUnit.SECONDS)))
                    .containsExactlyInAnyOrder(201, 409);
            assertThat(categorias.findByUsuarioIdOrderByNombreAsc(usuario.getId())).hasSize(1);
        } finally {
            pool.shutdownNow();
            categorias.deleteAll(categorias.findByUsuarioIdOrderByNombreAsc(usuario.getId()));
            usuarios.deleteById(usuario.getId());
        }
    }

    private int crearConcurrente(String email, String nombre, CountDownLatch inicio) throws InterruptedException {
        inicio.await();
        try { service.crear(email, pedido(nombre)); return 201; }
        catch (ResponseStatusException e) { return e.getStatusCode().value(); }
    }
}
