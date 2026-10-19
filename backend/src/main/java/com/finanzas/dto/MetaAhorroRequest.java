package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.DecimalMax; // Valor máximo permitido
import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.NotBlank; // No puede estar vacío
import jakarta.validation.constraints.NotNull; // No puede ser nulo
import jakarta.validation.constraints.Size; // Longitud máxima

import java.math.BigDecimal; // Números decimales exactos
import java.time.LocalDate; // Fecha sin hora

public record MetaAhorroRequest( // Datos para crear o editar una meta
        @NotBlank @Size(max = 100) String nombre, // Nombre, máximo 100 caracteres
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoObjetivo, // Objetivo, mínimo 0.01
        LocalDate fechaLimite, // Fecha límite opcional
        // Porcentaje de cada ingreso que se aporta a la meta; null = desactivado
        @DecimalMin("0.01") @DecimalMax("100") @Digits(integer = 3, fraction = 2) BigDecimal porcentajeAutomatico
) {
}
