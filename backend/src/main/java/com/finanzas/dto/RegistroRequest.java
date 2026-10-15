package com.finanzas.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegistroRequest(
        @NotBlank @Size(max = 100) String nombre,
        @NotBlank @Email @Size(max = 150) String email,
        // BCrypt solo procesa los primeros 72 bytes, por eso el máximo
        @NotBlank @Size(min = 8, max = 72) String password
) {
}
