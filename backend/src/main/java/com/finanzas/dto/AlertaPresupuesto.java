package com.finanzas.dto;

import java.math.BigDecimal;

/** Aviso no bloqueante que acompaña a un gasto cuando el presupuesto de su categoría está en riesgo. */
public record AlertaPresupuesto(
        String nivel,
        String mensaje,
        BigDecimal saldoConsumido,
        BigDecimal montoLimite
) {
}
