package com.finanzas.dto;

public record AuthResponse(
        String token,
        String tipo,
        String email,
        String nombre
) {
}
