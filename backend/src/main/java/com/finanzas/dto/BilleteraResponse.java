package com.finanzas.dto; // DTOs de entrada y salida

import java.math.BigDecimal; // Números decimales exactos

public record BilleteraResponse( // Datos de billetera devueltos al cliente
        Long id, // Identificador
        String nombre, // Nombre de la billetera
        BigDecimal saldoInicial, // Saldo con el que se creó
        BigDecimal saldoActual, // Saldo calculado actual
        String divisa, // Código de divisa
        boolean activa // Indica si sigue activa
) {
}
