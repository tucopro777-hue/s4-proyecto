package com.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record GastoResponse(
        Long id,
        BigDecimal monto,
        String descripcion,
        LocalDate fecha,
        Long categoriaId,
        String categoriaNombre
) {
}
