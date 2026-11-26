package com.finanzas.dto;

import java.math.BigDecimal;
import java.util.List;

/** Orden de filas y columnas: ESTABLE, DEFICIT. null significa no estimable. */
public record MarkovResponse(String divisa, String mesActual, Mes estadoActual,
        String mesBase, String mesPredicho, String estadoBase, List<Mes> historial,
        List<Fila> matriz, Probabilidades probabilidades, String explicacion) {
    public enum Estado { ESTABLE, DEFICIT }
    public record Mes(String mes, BigDecimal ingresos, BigDecimal gastos, Estado estado) {}
    public record Fila(Estado origen, long aEstable, long aDeficit, long total,
                       BigDecimal pEstable, BigDecimal pDeficit) {}
    public record Probabilidades(BigDecimal estable, BigDecimal deficit) {}
}
