package com.finanzas.dto; // DTOs de entrada y salida

import com.finanzas.model.TipoTransaccion; // Enum de ingreso o gasto

import java.math.BigDecimal; // Números decimales exactos
import java.time.LocalDate; // Fecha sin hora

public record TransaccionResponse( // Datos de movimiento devueltos al cliente
        Long id, // Identificador
        BigDecimal monto, // Monto del movimiento
        TipoTransaccion tipo, // Ingreso o gasto
        String descripcion, // Descripción
        LocalDate fecha, // Fecha del movimiento
        Long billeteraId, // Id de la billetera
        String billeteraNombre, // Nombre de la billetera
        Long categoriaId, // Id de la categoría
        String categoriaNombre, // Nombre de la categoría
        // Solo se rellena al registrar un GASTO que afecta a un presupuesto; si no, es null
        AlertaPresupuesto alertaPresupuesto // Alerta de presupuesto, si aplica
) {
}
