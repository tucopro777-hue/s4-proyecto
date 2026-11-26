package com.finanzas.service;

import com.finanzas.model.*;
import org.junit.jupiter.api.Test;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;

class MarkovServiceTest {
    private final MarkovService service = new MarkovService();
    private final Usuario usuario = new Usuario("Ana", "ana@test.test", "clave");
    private final Billetera bob = new Billetera("Caja", BigDecimal.ZERO, "BOB", usuario);
    private final LocalDate hoy = LocalDate.of(2026, 7, 15);

    private Transaccion movimiento(int mes, TipoTransaccion tipo, int monto) {
        return new Transaccion(BigDecimal.valueOf(monto), tipo, "", LocalDate.of(2026, mes, 1), usuario, bob, null);
    }
    private List<Transaccion> secuencia(String estados) {
        List<Transaccion> lista = new ArrayList<>();
        for (int i = 0; i < estados.length(); i++) lista.add(movimiento(i + 1,
                estados.charAt(i) == 'E' ? TipoTransaccion.INGRESO : TipoTransaccion.GASTO, 100));
        return lista;
    }

    @Test void calculaFrecuenciasYUsaFilaDelMesActualSinEntrenarlo() {
        var datos = secuencia("EED EDE".replace(" ", "")); // E,E,D,E,D,E
        datos.add(movimiento(7, TipoTransaccion.INGRESO, 100));
        Collections.reverse(datos); // El repositorio devuelve orden descendente.
        var r = service.calcular(datos, "BOB", hoy);
        assertThat(r.historial()).hasSize(6);
        assertThat(r.matriz().get(0).aEstable()).isEqualTo(1);
        assertThat(r.matriz().get(0).aDeficit()).isEqualTo(2);
        assertThat(r.matriz().get(1).aEstable()).isEqualTo(2);
        assertThat(r.matriz().get(1).aDeficit()).isZero();
        assertThat(r.probabilidades().estable()).isEqualByComparingTo("0.333333333333");
        assertThat(r.probabilidades().estable().add(r.probabilidades().deficit())).isEqualByComparingTo("1");
        assertThat(r.mesPredicho()).isEqualTo("2026-08");
    }

    @Test void sinDatosNoInventaEstadosNiFilas() {
        var r = service.calcular(List.of(), "BOB", hoy);
        assertThat(r.estadoActual()).isNull();
        assertThat(r.probabilidades()).isNull();
        assertThat(r.matriz()).allSatisfy(f -> { assertThat(f.total()).isZero(); assertThat(f.pEstable()).isNull(); });
    }

    @Test void unMesCerradoNoBastaAunqueExistaMesActual() {
        var r = service.calcular(List.of(movimiento(6, TipoTransaccion.INGRESO, 10),
                movimiento(7, TipoTransaccion.INGRESO, 10)), "BOB", hoy);
        assertThat(r.probabilidades()).isNull();
        assertThat(r.explicacion()).contains("transición");
    }

    @Test void huecosNoProducenTransicionesYEmpateEsEstable() {
        var r = service.calcular(List.of(movimiento(1, TipoTransaccion.INGRESO, 50),
                movimiento(1, TipoTransaccion.GASTO, 50), movimiento(3, TipoTransaccion.GASTO, 10)), "BOB", hoy);
        assertThat(r.historial().get(0).estado().name()).isEqualTo("ESTABLE");
        assertThat(r.matriz()).allSatisfy(f -> assertThat(f.total()).isZero());
    }

    @Test void excluyeAnuladasFuturasYOtrasDivisas() {
        var anulada = movimiento(1, TipoTransaccion.GASTO, 999); anulada.anular();
        var usd = new Billetera("Dólares", BigDecimal.ZERO, "USD", usuario);
        var otra = new Transaccion(BigDecimal.TEN, TipoTransaccion.GASTO, "", hoy, usuario, usd, null);
        var futura = new Transaccion(BigDecimal.TEN, TipoTransaccion.GASTO, "", hoy.plusDays(1), usuario, bob, null);
        var datos = secuencia("EE"); datos.addAll(List.of(anulada, otra, futura, movimiento(7, TipoTransaccion.INGRESO, 100)));
        var r = service.calcular(datos, "BOB", hoy);
        assertThat(r.historial().get(0).gastos()).isZero();
        assertThat(r.estadoActual().gastos()).isZero();
        assertThat(r.probabilidades().estable()).isEqualByComparingTo("1");
    }

    @Test void filaAusenteDelEstadoActualImpidePrediccion() {
        var datos = secuencia("EED"); datos.add(movimiento(7, TipoTransaccion.GASTO, 100));
        var r = service.calcular(datos, "BOB", hoy);
        assertThat(r.matriz().get(0).total()).isEqualTo(2);
        assertThat(r.matriz().get(1).pDeficit()).isNull();
        assertThat(r.probabilidades()).isNull();
    }

    @Test void sinMesActualNoReutilizaUnEstadoAntiguo() {
        assertThat(service.calcular(secuencia("EEE"), "BOB", hoy).probabilidades()).isNull();
    }

    @Test void reproduceEjercicioDeDefensaConProbabilidadEstableOchentaPorCiento() {
        var datos = secuencia("EEEEEDDE");
        datos.add(movimiento(9, TipoTransaccion.INGRESO, 100));
        var r = service.calcular(datos, "BOB", LocalDate.of(2026, 9, 15));
        assertThat(r.matriz().get(0).aEstable()).isEqualTo(4);
        assertThat(r.matriz().get(0).aDeficit()).isEqualTo(1);
        assertThat(r.matriz().get(1).aEstable()).isEqualTo(1);
        assertThat(r.matriz().get(1).aDeficit()).isEqualTo(1);
        assertThat(r.probabilidades().estable()).isEqualByComparingTo("0.8");
        assertThat(r.probabilidades().deficit()).isEqualByComparingTo("0.2");
        assertThat(r.mesPredicho()).isEqualTo("2026-10");
    }
}
