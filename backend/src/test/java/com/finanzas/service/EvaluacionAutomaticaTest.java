package com.finanzas.service;

import com.finanzas.dto.*;
import com.finanzas.model.*;
import com.finanzas.repository.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.mockito.Mockito.*;
import static org.assertj.core.api.Assertions.assertThat;

class EvaluacionAutomaticaTest {
    private AnalisisFinancieroService simulador(String estable, String deficit) {
        var usuarios = mock(UsuarioRepository.class);
        var billeteras = mock(BilleteraRepository.class);
        var transacciones = mock(TransaccionRepository.class);
        var markov = mock(MarkovService.class);
        var usuario = new Usuario("SIMULACIÓN", "simulacion@test.test", "hash");
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(billeteras.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuario.getId())).thenReturn(
                List.of(new Billetera("Caja ficticia", new BigDecimal("2000"), "BOB", usuario)));
        when(transacciones.findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(usuario.getId())).thenReturn(List.of());
        var prediccion = new MarkovResponse("BOB", "2026-11", null, "2026-11", "2026-12", "ESTABLE",
                List.of(), List.of(), new MarkovResponse.Probabilidades(new BigDecimal(estable), new BigDecimal(deficit)), "SIMULACIÓN de prueba");
        when(markov.calcular(anyList(), eq("BOB"), any(LocalDate.class))).thenReturn(prediccion);
        return new AnalisisFinancieroService(usuarios, transacciones, billeteras, markov, new VmeService());
    }

    @Test void probabilidadesActualesEvaluanLasTresYElMontoEscalaCadaResultado() {
        var servicio = simulador("0.333333333333", "0.666666666667");
        var pequeno = servicio.evaluarAutomaticamente("simulacion@test.test", new EvaluacionAutomaticaRequest("BOB", new BigDecimal("500")));
        var grande = servicio.evaluarAutomaticamente("simulacion@test.test", new EvaluacionAutomaticaRequest("BOB", new BigDecimal("1000")));
        assertThat(pequeno.alternativas()).hasSize(3).allSatisfy(a -> assertThat(a.viable()).isTrue());
        assertThat(pequeno.explicacion()).contains("SIMULACIÓN", "ficticios");
        assertThat(pequeno.recomendadas()).containsExactly("Guardar mi dinero");
        assertThat(grande.recomendadas()).isEqualTo(pequeno.recomendadas());
        String[] cambiosE = {"0", "20", "100"}, cambiosD = {"0", "-10", "-120"};
        for (int i = 0; i < 3; i++) {
            var a = pequeno.alternativas().get(i); var b = grande.alternativas().get(i);
            assertThat(a.resultadoEstable()).isEqualByComparingTo(cambiosE[i]);
            assertThat(a.resultadoDeficit()).isEqualByComparingTo(cambiosD[i]);
            var exacto = a.patrimonioEstable().multiply(pequeno.markov().probabilidades().estable())
                    .add(a.patrimonioDeficit().multiply(pequeno.markov().probabilidades().deficit()));
            assertThat(a.vme()).isEqualByComparingTo(exacto.setScale(2, java.math.RoundingMode.HALF_UP));
            assertThat(b.resultadoEstable()).isEqualByComparingTo(a.resultadoEstable().multiply(BigDecimal.valueOf(2)));
            assertThat(b.resultadoDeficit()).isEqualByComparingTo(a.resultadoDeficit().multiply(BigDecimal.valueOf(2)));
        }
        assertThat(pequeno.alternativas().get(0).vme()).isEqualByComparingTo("500");
        assertThat(pequeno.alternativas().get(1).vme()).isEqualByComparingTo("500");
        assertThat(pequeno.alternativas().get(2).vme()).isEqualByComparingTo("453.33");
        assertThat(grande.alternativas().get(2).vme()).isEqualByComparingTo("906.67");
    }

    @Test void montoEscalaSinCambiarGanadoraEnLosTresRegimenes() {
        String[] probabilidades = {"0.1", "0.5", "0.8"};
        String[] ganadoras = {"Guardar mi dinero", "Ahorrar", "Invertir"};
        String[][] esperados = {{"500", "493", "402"}, {"500", "505", "490"}, {"500", "514", "556"}};
        for (int i = 0; i < 3; i++) {
            var p = new BigDecimal(probabilidades[i]);
            var servicio = simulador(p.toPlainString(), BigDecimal.ONE.subtract(p).toPlainString());
            for (String monto : List.of("500", "1000")) {
                var r = servicio.evaluarAutomaticamente("simulacion@test.test", new EvaluacionAutomaticaRequest("BOB", new BigDecimal(monto)));
                assertThat(r.recomendadas()).containsExactly(ganadoras[i]);
                assertThat(r.alternativas()).hasSize(3);
                for (int j = 0; j < 3; j++) {
                    var esperado = new BigDecimal(esperados[i][j]).multiply(new BigDecimal(monto).divide(new BigDecimal("500")));
                    assertThat(r.alternativas().get(j).vme()).isEqualByComparingTo(esperado);
                }
            }
        }
    }

    @Test void mismosEscenariosCambianGanadoraSoloAlCambiarProbabilidades() {
        var usuarios = mock(UsuarioRepository.class);
        var billeteras = mock(BilleteraRepository.class);
        var transacciones = mock(TransaccionRepository.class);
        var markov = mock(MarkovService.class);
        var usuario = new Usuario("Prueba", "prueba@test.test", "hash");
        when(usuarios.findByEmail(usuario.getEmail())).thenReturn(Optional.of(usuario));
        when(billeteras.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuario.getId())).thenReturn(
                List.of(new Billetera("Caja", new BigDecimal("2000"), "BOB", usuario)));
        when(transacciones.findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(usuario.getId())).thenReturn(List.of());
        var servicio = new AnalisisFinancieroService(usuarios, transacciones, billeteras, markov, new VmeService());
        String[] probabilidades = {"0.8", "0.5", "0.1"};
        String[] ganadoras = {"Invertir", "Ahorrar", "Guardar mi dinero"};
        String[] esperados = {"1112", "1010", "1000"};
        for (int i = 0; i < probabilidades.length; i++) {
            var p = new BigDecimal(probabilidades[i]);
            var prediccion = new MarkovResponse("BOB", "2026-11", null, "2026-11", "2026-12", "ESTABLE",
                    List.of(), List.of(), new MarkovResponse.Probabilidades(p, BigDecimal.ONE.subtract(p)), "Prueba");
            when(markov.calcular(List.of(), "BOB", LocalDate.now())).thenReturn(prediccion);
            var r = servicio.evaluarAutomaticamente(usuario.getEmail(), new EvaluacionAutomaticaRequest("BOB", new BigDecimal("1000")));
            assertThat(r.recomendadas()).containsExactly(ganadoras[i]);
            assertThat(r.markov()).isSameAs(prediccion);
            final String ganadora = ganadoras[i];
            assertThat(r.alternativas().stream().filter(a -> a.nombre().equals(ganadora)).findFirst().orElseThrow().vme())
                    .isEqualByComparingTo(esperados[i]);
        }
        verify(transacciones, never()).save(any());
        verify(billeteras, never()).save(any());
    }
}
