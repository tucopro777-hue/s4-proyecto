package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.Max; // Valor máximo permitido
import jakarta.validation.constraints.Min; // Valor mínimo permitido
import jakarta.validation.constraints.NotNull; // No puede ser nulo

import java.math.BigDecimal; // Números decimales exactos

public record PresupuestoRequest( // Datos para crear un presupuesto
        @NotNull Long categoriaId, // Categoría a la que aplica
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoLimite, // Límite, mínimo 0.01
        @NotNull @Min(2000) @Max(2100) Integer anio, // Año entre 2000 y 2100
        @NotNull @Min(1) @Max(12) Integer mes // Mes entre 1 y 12
) {
}
