package com.finanzas.service;

import java.text.Normalizer;
import java.util.Locale;

/** Comparación conservadora: equivalencia de escritura o una sola letra añadida/faltante. */
final class NombreCategoria {
    private NombreCategoria() {}

    static String limpiar(String nombre) {
        return nombre.replaceAll("\\p{Cf}", "").replaceAll("(?U)\\s+", " ").strip();
    }

    static String clave(String nombre) {
        return Normalizer.normalize(limpiar(nombre), Normalizer.Form.NFD)
                .replaceAll("\\p{M}+", "").replace(" ", "").toLowerCase(Locale.ROOT);
    }

    static boolean coinciden(String primero, String segundo) {
        String a = clave(primero), b = clave(segundo);
        if (a.equals(b)) return true;
        int[] corta = a.codePoints().toArray(), larga = b.codePoints().toArray();
        if (corta.length > larga.length) { int[] tmp = corta; corta = larga; larga = tmp; }
        // Evita bloquear indiscriminadamente nombres cortos o distintos por una sustitución.
        if (corta.length < 4 || larga.length != corta.length + 1) return false;
        int i = 0, j = 0;
        boolean omitida = false;
        while (i < corta.length && j < larga.length) {
            if (corta[i] == larga[j]) { i++; j++; }
            else {
                if (omitida || !Character.isLetter(larga[j])) return false;
                omitida = true; j++;
            }
        }
        return j == larga.length || Character.isLetter(larga[j]);
    }
}
