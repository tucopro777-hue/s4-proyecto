package com.finanzas.dto;

public record CategoriaResponse(
        Long id,
        String nombre,
        String color,
        String tipo
) {
}
