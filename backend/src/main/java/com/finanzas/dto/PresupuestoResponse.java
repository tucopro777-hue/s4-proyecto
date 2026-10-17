package com.finanzas.dto; // DTOs de entrada y salida

import java.math.BigDecimal; // Números decimales exactos

public record PresupuestoResponse( // Datos de presupuesto devueltos al cliente
        Long id, // Identificador
        Long categoriaId, // Categoría asociada
        String categoriaNombre, // Nombre de la categoría
        BigDecimal montoLimite, // Límite del periodo
        BigDecimal saldoConsumido, // Gasto acumulado del periodo
        int anio, // Año del presupuesto
        int mes, // Mes del presupuesto
        // OK, ADVERTENCIA (80 % o más) o EXCEDIDO (por encima del límite)
        String nivel // Estado del presupuesto
) {
}
