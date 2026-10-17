package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.DecimalMin; // Valor mínimo permitido
import jakarta.validation.constraints.Digits; // Máximo de dígitos
import jakarta.validation.constraints.NotNull; // No puede ser nulo
import jakarta.validation.constraints.Size; // Longitud máxima

import java.math.BigDecimal; // Números decimales exactos
import java.time.LocalDate; // Fecha sin hora

public record TraspasoRequest( // Datos para transferir entre billeteras
        @NotNull Long billeteraOrigenId, // Billetera de la que sale el dinero
        @NotNull Long billeteraDestinoId, // Billetera que recibe el dinero
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto, // Monto, mínimo 0.01
        @Size(max = 255) String descripcion, // Descripción opcional, máximo 255
        @NotNull LocalDate fecha // Fecha del traspaso
) {
}
