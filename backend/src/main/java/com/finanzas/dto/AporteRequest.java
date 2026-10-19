package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.NotNull; // No puede ser nulo

import java.math.BigDecimal; // Números decimales exactos

public record AporteRequest( // Datos de un aporte a una meta
        @NotNull Long billeteraId, // Billetera de la que sale el dinero
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto // Monto del aporte, mínimo 0.01
) {
}
