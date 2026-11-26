package com.finanzas.dto;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;

public record EvaluacionAutomaticaRequest(
        @NotBlank @Pattern(regexp = "[A-Z]{3}") String divisa,
        @NotNull @DecimalMin("0.01") @Digits(integer = 12, fraction = 2) BigDecimal monto) {}
