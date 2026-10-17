package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.NotBlank; // No puede estar vacío
import jakarta.validation.constraints.NotNull; // No puede ser nulo
import jakarta.validation.constraints.Pattern; // Valida con expresión regular
import jakarta.validation.constraints.Size; // Longitud máxima o mínima

import java.math.BigDecimal; // Números decimales exactos

public record BilleteraRequest( // Datos para crear una billetera
        @NotBlank @Size(max = 100) String nombre, // Nombre, máximo 100 caracteres
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal saldoInicial, // Saldo inicial, no negativo
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "La divisa debe ser un código de 3 letras mayúsculas") String divisa // Código de divisa, p. ej. BOB
) {
}
