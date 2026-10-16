package com.finanzas.dto;

import com.finanzas.model.TipoTransaccion;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionRequest(
        @NotNull @DecimalMin("0.01") @Digits(integer = 10, fraction = 2) BigDecimal monto,
        @NotNull TipoTransaccion tipo,
        @Size(max = 255) String descripcion,
        @NotNull LocalDate fecha,
        @NotNull Long billeteraId,
        @NotNull Long categoriaId
) {
}
