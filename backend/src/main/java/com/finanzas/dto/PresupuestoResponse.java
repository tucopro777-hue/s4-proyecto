package com.finanzas.dto;

import java.math.BigDecimal;

public record PresupuestoResponse(
        Long id,
        Long categoriaId,
        String categoriaNombre,
        BigDecimal montoLimite,
        BigDecimal saldoConsumido,
        int anio,
        int mes,
        /** OK, ADVERTENCIA (80 % o más) o EXCEDIDO (por encima del límite). */
        String nivel
) {
}
