package com.finanzas.dto; // DTOs de entrada y salida

import java.math.BigDecimal; // Números decimales exactos
import java.time.LocalDate; // Fecha sin hora

public record MetaAhorroResponse( // Datos de meta devueltos al cliente
        Long id, // Identificador
        String nombre, // Nombre de la meta
        BigDecimal montoObjetivo, // Monto que se quiere alcanzar
        BigDecimal montoActual, // Monto ahorrado hasta ahora
        LocalDate fechaLimite, // Fecha límite
        // Progreso en porcentaje (0 a 100 o más si se supera el objetivo), con un decimal
        BigDecimal porcentaje, // Porcentaje de avance
        boolean completada, // Indica si ya se alcanzó el objetivo
        BigDecimal porcentajeAutomatico // Porcentaje de cada ingreso que se aporta; null = desactivado
) {
}
