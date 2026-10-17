package com.finanzas.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TraspasoResponse(
        Long id,
        BigDecimal monto,
        String descripcion,
        LocalDate fecha,
        Long billeteraOrigenId,
        String billeteraOrigenNombre,
        Long billeteraDestinoId,
        String billeteraDestinoNombre
) {
}
