package com.finanzas.dto; // DTOs de entrada y salida

import java.math.BigDecimal; // Números decimales exactos
import java.time.LocalDate; // Fecha sin hora

public record TraspasoResponse( // Datos de traspaso devueltos al cliente
        Long id, // Identificador
        BigDecimal monto, // Monto transferido
        String descripcion, // Descripción
        LocalDate fecha, // Fecha del traspaso
        Long billeteraOrigenId, // Id de la billetera origen
        String billeteraOrigenNombre, // Nombre de la billetera origen
        Long billeteraDestinoId, // Id de la billetera destino
        String billeteraDestinoNombre // Nombre de la billetera destino
) {
}
