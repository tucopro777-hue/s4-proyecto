package com.finanzas.dto; // DTOs de entrada y salida

import com.finanzas.model.TipoCategoria; // Enum de tipo de categoría

public record CategoriaResponse( // Datos de categoría devueltos al cliente
        Long id, // Identificador
        String nombre, // Nombre de la categoría
        TipoCategoria tipo, // Ingreso o gasto
        String color, // Color hexadecimal
        String icono // Nombre del icono
) {
}
