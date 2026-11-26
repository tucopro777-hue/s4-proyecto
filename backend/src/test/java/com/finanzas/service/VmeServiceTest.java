package com.finanzas.service;

import com.finanzas.dto.*;
import com.finanzas.dto.MarkovResponse.Probabilidades;
import com.finanzas.dto.VmeRequest.Alternativa;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class VmeServiceTest {
    private final VmeService service = new VmeService();
    private BigDecimal n(String n) { return new BigDecimal(n); }
    private MarkovResponse markov(String e, String d) {
        return new MarkovResponse("BOB", "2026-07", null, "2026-07", "2026-08", "ESTABLE", List.of(), List.of(),
                e == null ? null : new Probabilidades(n(e), n(d)), "prueba");
    }
    private VmeRequest pedido() {
        return new VmeRequest("BOB", n("100"), List.of(
                new Alternativa("Mantener", n("0"), n("0"), n("0")),
                new Alternativa("Ahorro", n("400"), n("20"), n("-10")),
                new Alternativa("Inversión", n("600"), n("100"), n("-120"))));
    }

    @Test void resuelveEjercicioYElGanadorCambiaConProbabilidades() {
        var r = service.calcular(n("1000"), markov("0.8", "0.2"), pedido());
        assertThat(r.alternativas().get(0).vme()).isEqualByComparingTo("1000");
        assertThat(r.alternativas().get(1).vme()).isEqualByComparingTo("1014");
        assertThat(r.alternativas().get(2).vme()).isEqualByComparingTo("1056");
        assertThat(r.recomendadas()).containsExactly("Inversión");
        assertThat(service.calcular(n("1000"), markov("0.5", "0.5"), pedido()).recomendadas()).containsExactly("Ahorro");
        assertThat(service.calcular(n("1000"), markov("0.1", "0.9"), pedido()).recomendadas()).containsExactly("Mantener");
    }

    @Test void saldoYReservaExcluyenAlternativasConVmeMayor() {
        var r = service.calcular(n("500"), markov("1", "0"), pedido());
        assertThat(r.recomendadas()).containsExactly("Ahorro");
        assertThat(r.alternativas().get(2).viable()).isFalse();
        assertThat(service.calcular(BigDecimal.ZERO, markov("1", "0"), pedido()).recomendadas()).isEmpty();
        assertThat(service.calcular(n("-1"), markov("1", "0"), pedido()).recomendadas()).isEmpty();
    }

    @Test void simulacionCambiaGanadoraAlCambiarPagosSinCambiarProbabilidadesNiDependerDelOrden() {
        // Escenarios ficticios con las mismas probabilidades de la demo.
        var p = markov("0.333333333333", "0.666666666667");
        for (String ganadora : List.of("Guardar", "Ahorrar", "Invertir")) {
            for (var orden : List.of(List.of("Guardar", "Ahorrar", "Invertir"), List.of("Invertir", "Guardar", "Ahorrar"))) {
                var alternativas = orden.stream().map(nombre -> new Alternativa(nombre, n("0"),
                        nombre.equals(ganadora) ? n("60") : n("-30"),
                        nombre.equals(ganadora) ? n("30") : n("-60"))).toList();
                var r = service.calcular(n("500"), p, new VmeRequest("BOB", n("0"), alternativas));
                assertThat(r.alternativas()).hasSize(3).allSatisfy(a -> assertThat(a.viable()).isTrue());
                assertThat(r.recomendadas()).containsExactly(ganadora);
                assertThat(r.alternativas().stream().filter(a -> a.nombre().equals(ganadora)).findFirst().orElseThrow().vme())
                        .isEqualByComparingTo("540");
            }
        }
    }

    @Test void probabilidadesTruncadasPuedenRomperEmpateVisiblePeroNoSeFavoreceGuardar() {
        var pedido = new VmeRequest("BOB", n("0"), List.of(
                new Alternativa("Guardar", n("0"), n("0"), n("0")),
                new Alternativa("Ahorrar", n("500"), n("20"), n("-10")),
                new Alternativa("Invertir", n("500"), n("100"), n("-120"))));
        assertThat(service.calcular(n("500"), markov("0.333333333333", "0.666666666667"), pedido).recomendadas())
                .containsExactly("Guardar");
        // Cambiar la aproximación al otro lado de 1/3 favorece Ahorrar.
        assertThat(service.calcular(n("500"), markov("0.333333333334", "0.666666666666"), pedido).recomendadas())
                .containsExactly("Ahorrar");
        // Empate exacto: devuelve ambas, aunque Guardar esté primero.
        var empate = new VmeRequest("BOB", n("0"), List.of(pedido.alternativas().get(0),
                new Alternativa("Ahorrar", n("500"), n("20"), n("-20"))));
        assertThat(service.calcular(n("500"), markov("0.5", "0.5"), empate).recomendadas())
                .containsExactly("Guardar", "Ahorrar");
    }

    @Test void sinProbabilidadesNoHayVmeNiRecomendacion() {
        var r = service.calcular(n("1000"), markov(null, null), pedido());
        assertThat(r.recomendadas()).isEmpty();
        assertThat(r.alternativas()).allSatisfy(a -> assertThat(a.vme()).isNull());
    }

    @Test void rechazaProbabilidadesInvalidas() {
        for (var p : List.of(new Probabilidades(n("-0.1"), n("1.1")), new Probabilidades(n("0.3"), n("0.3")),
                new Probabilidades(null, n("1")), new Probabilidades(n("1.1"), n("0")))) {
            assertThatThrownBy(() -> service.validarProbabilidades(p)).isInstanceOf(ResponseStatusException.class);
        }
    }

    @Test void informaEmpatesYExcluyePerdidasMayoresAlPatrimonio() {
        var request = new VmeRequest("BOB", n("0"), List.of(new Alternativa("A", n("0"), n("0"), n("0")),
                new Alternativa("B", n("0"), n("0"), n("0")), new Alternativa("C", n("0"), n("5000"), n("-1001"))));
        var r = service.calcular(n("1000"), markov("0.5", "0.5"), request);
        assertThat(r.recomendadas()).containsExactly("A", "B");
        assertThat(r.alternativas().get(2).viable()).isFalse();
    }

    @Test void noSeleccionaConValoresRedondeadosYNombresDebenSerUnicos() {
        var request = new VmeRequest("BOB", n("0"), List.of(new Alternativa("A", n("0"), n("0.01"), n("0")),
                new Alternativa("B", n("0"), n("0"), n("0"))));
        assertThat(service.calcular(n("1000"), markov("0.1", "0.9"), request).recomendadas()).containsExactly("A");
        var duplicado = new VmeRequest("BOB", n("0"), List.of(request.alternativas().get(0), request.alternativas().get(0)));
        assertThatThrownBy(() -> service.calcular(n("1000"), markov("1", "0"), duplicado)).isInstanceOf(ResponseStatusException.class);
    }
}
