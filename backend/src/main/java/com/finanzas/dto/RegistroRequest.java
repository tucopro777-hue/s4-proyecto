package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.Email; // Debe tener formato de email
import jakarta.validation.constraints.NotBlank; // No puede estar vacío
import jakarta.validation.constraints.Size; // Longitud máxima o mínima

public record RegistroRequest( // Datos para crear una cuenta
        @NotBlank @Size(max = 100) String nombre, // Nombre, máximo 100 caracteres
        @NotBlank @Email @Size(max = 150) String email, // Email válido, máximo 150 caracteres
        // BCrypt solo procesa los primeros 72 bytes, por eso el máximo
        @NotBlank @Size(min = 8, max = 72) String password // Contraseña entre 8 y 72 caracteres
) {
}
