package com.finanzas.dto;

import com.finanzas.model.TipoCategoria;

public record CategoriaResponse(
        Long id,
        String nombre,
        TipoCategoria tipo,
        String color,
        String icono
) {
}
