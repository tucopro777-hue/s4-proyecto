package com.finanzas.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TraspasoRequest(
        @NotNull Long billeteraOrigenId,
        @NotNull Long billeteraDestinoId,
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @Size(max = 255) String descripcion,
        @NotNull LocalDate fecha
) {
}
