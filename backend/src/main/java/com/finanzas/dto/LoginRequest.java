package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.Email; // Debe tener formato de email
import jakarta.validation.constraints.NotBlank; // No puede estar vacío

public record LoginRequest( // Credenciales de login
        @NotBlank @Email String email, // Email válido obligatorio
        @NotBlank String password // Contraseña obligatoria
) {
}
