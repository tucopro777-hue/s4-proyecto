package com.finanzas.dto; // DTOs de entrada y salida

import com.finanzas.model.TipoCategoria; // Enum de tipo de categoría
import jakarta.validation.constraints.NotBlank; // No puede estar vacío
import jakarta.validation.constraints.NotNull; // No puede ser nulo
import jakarta.validation.constraints.Pattern; // Valida con expresión regular
import jakarta.validation.constraints.Size; // Longitud máxima

public record CategoriaRequest( // Datos para crear una categoría
        @NotBlank @Size(max = 50) String nombre, // Nombre, máximo 50 caracteres
        @NotNull TipoCategoria tipo, // Ingreso o gasto
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser hexadecimal, p. ej. #FF8800") String color, // Color hexadecimal
        @NotBlank @Pattern(regexp = "^[a-z]{2,30}$", message = "El icono debe ser un identificador en minúsculas") String icono // Nombre del icono
) {
}
