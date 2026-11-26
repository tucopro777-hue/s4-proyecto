package com.finanzas.dto;

import jakarta.validation.constraints.PositiveOrZero;

/** Dinero disponible que se quiere repartir entre las metas. */
public record AsignacionRequest(
        @PositiveOrZero double presupuestoDisponible
) {
}
