package com.finanzas.dto;

import java.util.List;

/**
 * Valor Monetario Esperado (VME) de cada decisión para el próximo mes. El VME de una alternativa
 * es la suma, sobre los estados del próximo mes, de probabilidad (Markov) × pago en ese estado.
 * Si datosSuficientes es false, la lista de alternativas viene vacía.
 */
public record VmeResponse(
        boolean datosSuficientes,
        String estadoActual,
        List<Alternativa> alternativas,
        String mejorAlternativa
) {

    /**
     * Pago = balance mensual esperado del próximo mes en cada estado, más el ahorro de recortar
     * una fracción de los gastos promedio de ese estado.
     */
    public record Alternativa(
            String nombre,
            double reduccionGastos,
            double pagoEstable,
            double pagoDeficit,
            double vme
    ) {
    }
}
