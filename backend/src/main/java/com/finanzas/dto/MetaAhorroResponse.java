package com.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MetaAhorroResponse(
        Long id,
        String nombre,
        BigDecimal montoObjetivo,
        BigDecimal montoActual,
        LocalDate fechaLimite,
        /** Progreso en porcentaje (0 a 100 o más si se supera el objetivo), con un decimal. */
        BigDecimal porcentaje,
        boolean completada
) {
}
