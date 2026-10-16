package com.finanzas.dto;

import java.math.BigDecimal;

public record BilleteraResponse(
        Long id,
        String nombre,
        BigDecimal saldoInicial,
        BigDecimal saldoActual,
        String divisa,
        boolean activa
) {
}
