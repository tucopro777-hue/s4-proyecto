package com.finanzas.dto;

/**
 * Proyección del próximo mes según la cadena de Markov. Estados: ESTABLE (el mes termina con
 * balance mayor o igual a 0) y DEFICIT (balance menor que 0). Las probabilidades van de 0 a 1.
 * Si datosSuficientes es false, los campos de probabilidad y la matriz vienen en null.
 */
public record ProyeccionFinancieraResponse(
        boolean datosSuficientes,
        int mesesAnalizados,
        String estadoActual,
        Double probabilidadMantener,
        Double probabilidadEstableSiguiente,
        Double probabilidadDeficitSiguiente,
        MatrizTransicion matriz
) {

    /** Matriz de transición: el origen es la fila y el destino la columna. */
    public record MatrizTransicion(
            double deficitADeficit,
            double deficitAEstable,
            double estableADeficit,
            double estableAEstable
    ) {
    }
}
