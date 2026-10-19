package com.finanzas.dto; // DTOs de entrada y salida

import jakarta.validation.constraints.NotBlank; // No puede estar vacío

/** Contraseña del usuario, pedida para confirmar acciones sensibles. */
public record ConfirmarPasswordRequest( // Cuerpo de la petición
        @NotBlank String password // Contraseña actual del usuario
) {
}
