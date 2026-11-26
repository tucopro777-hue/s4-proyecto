package com.finanzas.service;

/** Se lanza cuando el login se intenta con una cuenta bloqueada por demasiados fallos. */
public class CuentaBloqueadaException extends RuntimeException {

    private final long segundosRestantes;

    public CuentaBloqueadaException(long segundosRestantes) {
        super("Demasiados intentos fallidos. Inténtalo de nuevo en " + minutosLegibles(segundosRestantes) + ".");
        this.segundosRestantes = segundosRestantes;
    }

    public long getSegundosRestantes() {
        return segundosRestantes;
    }

    private static String minutosLegibles(long segundos) {
        long minutos = Math.max(1, (segundos + 59) / 60); // redondea hacia arriba
        return minutos == 1 ? "1 minuto" : minutos + " minutos";
    }
}
