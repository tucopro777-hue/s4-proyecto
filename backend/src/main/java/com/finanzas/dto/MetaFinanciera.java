package com.finanzas.dto;

import java.math.BigDecimal;

/**
 * Ítem de la mochila: una meta con su costo pendiente y su prioridad (1 = baja, 10 = alta).
 * El costo es lo que falta para completar la meta.
 */
public record MetaFinanciera(
        Long id,
        String nombre,
        BigDecimal costo,
        int prioridad
) {
}
