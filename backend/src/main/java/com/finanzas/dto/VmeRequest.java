package com.finanzas.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.util.List;

/** Pagos netos hipotéticos al final de un mes, expresados en la divisa seleccionada. */
public record VmeRequest(@NotBlank @Pattern(regexp = "[A-Z]{3}") String divisa,
        @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal reservaMinima,
        @NotEmpty @Size(max = 10) List<@NotNull @Valid Alternativa> alternativas) {
    public record Alternativa(@NotBlank @Size(max = 80) String nombre,
            @NotNull @DecimalMin("0") @Digits(integer = 12, fraction = 2) BigDecimal asignacion,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal resultadoEstable,
            @NotNull @Digits(integer = 12, fraction = 2) BigDecimal resultadoDeficit) {}
}
