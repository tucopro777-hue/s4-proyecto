package com.finanzas.dto; // DTOs de entrada y salida

import java.math.BigDecimal; // Números decimales exactos

// Aviso no bloqueante que acompaña a un gasto cuando el presupuesto de su categoría está en riesgo
public record AlertaPresupuesto( // Alerta de presupuesto
        String nivel, // Nivel de la alerta (ADVERTENCIA o EXCEDIDO)
        String mensaje, // Texto para el usuario
        BigDecimal saldoConsumido, // Monto gastado hasta ahora
        BigDecimal montoLimite // Límite del presupuesto
) {
}
