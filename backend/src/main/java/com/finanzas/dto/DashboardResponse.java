package com.finanzas.dto;

import java.math.BigDecimal;

/**
 * Indicadores del panel principal. Los importes de dinero van en BigDecimal para no perder
 * precisión; presupuestosExcedidos es un conteo de presupuestos del mes actual.
 */
public record DashboardResponse(
        BigDecimal saldoTotal,
        BigDecimal ingresosMes,
        BigDecimal gastosMes,
        BigDecimal balanceMes,
        Long presupuestosExcedidos
) {
}
