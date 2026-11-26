package com.finanzas.dto;

import java.math.BigDecimal;

/** Total gastado en una categoría durante el mes en curso; alimenta la gráfica del panel. */
public record GastoCategoriaResponse(
        Long categoriaId,
        String categoriaNombre,
        String color,
        BigDecimal total
) {
}
