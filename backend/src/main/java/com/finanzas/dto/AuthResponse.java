package com.finanzas.dto; // DTOs de entrada y salida

public record AuthResponse( // Respuesta de login o registro
        String token, // Token JWT para las siguientes peticiones
        String tipo, // Tipo de token (Bearer)
        String email, // Email del usuario
        String nombre // Nombre del usuario
) {
}
