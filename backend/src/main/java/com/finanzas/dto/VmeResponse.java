package com.finanzas.dto;

import java.math.BigDecimal;
import java.util.List;

public record VmeResponse(String divisa, BigDecimal disponible, BigDecimal reservaMinima,
        MarkovResponse markov, List<Evaluacion> alternativas, List<String> recomendadas,
        String explicacion) {
    public record Evaluacion(String nombre, BigDecimal asignacion, BigDecimal liquidezRestante,
            BigDecimal resultadoEstable, BigDecimal resultadoDeficit,
            BigDecimal patrimonioEstable, BigDecimal patrimonioDeficit,
            boolean viable, String motivo, BigDecimal vme) {}
}
