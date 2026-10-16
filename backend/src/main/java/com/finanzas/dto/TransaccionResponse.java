package com.finanzas.dto;

import com.finanzas.model.TipoTransaccion;

import java.math.BigDecimal;
import java.time.LocalDate;

public record TransaccionResponse(
        Long id,
        BigDecimal monto,
        TipoTransaccion tipo,
        String descripcion,
        LocalDate fecha,
        Long billeteraId,
        String billeteraNombre,
        Long categoriaId,
        String categoriaNombre
) {
}
