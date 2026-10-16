package com.finanzas.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record BilleteraRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotNull @DecimalMin("0") @Digits(integer = 10, fraction = 2) BigDecimal saldoInicial,
        @NotNull @Pattern(regexp = "[A-Z]{3}", message = "La divisa debe ser un código de 3 letras mayúsculas") String divisa
) {
}
