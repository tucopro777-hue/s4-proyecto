package com.finanzas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Resultado de la asignación inteligente: metas recomendadas y cómo queda el dinero. */
public record AsignacionResponse(
        List<MetaFinanciera> recomendadas,
        BigDecimal totalAsignado,
        int puntosPrioridad,
        BigDecimal sobrante
) {
}
