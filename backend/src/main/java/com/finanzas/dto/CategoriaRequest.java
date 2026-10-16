package com.finanzas.dto;

import com.finanzas.model.TipoCategoria;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotNull TipoCategoria tipo,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser hexadecimal, p. ej. #FF8800") String color,
        @NotBlank @Pattern(regexp = "^[a-z]{2,30}$", message = "El icono debe ser un identificador en minúsculas") String icono
) {
}
