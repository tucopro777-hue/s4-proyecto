package com.finanzas.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CategoriaRequest(
        @NotBlank @Size(max = 50) String nombre,
        @NotBlank @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "El color debe ser hexadecimal, p. ej. #FF8800") String color,
        @NotBlank @Size(max = 20) String tipo
) {
}
