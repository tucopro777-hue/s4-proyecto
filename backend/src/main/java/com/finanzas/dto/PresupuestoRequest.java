package com.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PresupuestoRequest(
        @NotNull Long categoriaId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoLimite,
        @NotNull @Min(2000) @Max(2100) Integer anio,
        @NotNull @Min(1) @Max(12) Integer mes
) {
}
