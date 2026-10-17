package com.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

/** Solo se puede cambiar el límite: categoría y periodo identifican al presupuesto. */
public record ActualizarPresupuestoRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal montoLimite
) {
}
