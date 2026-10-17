package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.NotNull; // No puede ser nulo

import java.math.BigDecimal; // Números decimales exactos

// Solo se puede cambiar el límite: categoría y periodo identifican al presupuesto
public record ActualizarPresupuestoRequest( // Datos para actualizar un presupuesto
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoLimite // Nuevo límite, mínimo 0.01
) {
}
