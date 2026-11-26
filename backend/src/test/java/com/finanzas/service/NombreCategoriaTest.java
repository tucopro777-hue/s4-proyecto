package com.finanzas.service;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;

class NombreCategoriaTest {
    @Test void ignoraMayusculasTildesYEspaciosInclusoInvisibles() {
        for (String variante : new String[]{" EDUCACIÓN ", "educacion", "Edu cación", "Edu\u00a0cación", "Edu\u200bcación"}) {
            assertThat(NombreCategoria.coinciden("Educación", variante)).as(variante).isTrue();
        }
        assertThat(NombreCategoria.limpiar("  Comida\u00a0  rápida  ")).isEqualTo("Comida rápida");
    }

    @Test void detectaUnaLetraAdicionalAlInicioEnMedioOAlFinal() {
        for (String variante : new String[]{"Ccomida", "Comiida", "Comidaa", "Comidas", "Cmida"}) {
            assertThat(NombreCategoria.coinciden("Comida", variante)).as(variante).isTrue();
            assertThat(NombreCategoria.coinciden(variante, "Comida")).as(variante).isTrue();
        }
    }

    @Test void noConfundeSustitucionesNumerosNiNombresMasEspecificos() {
        for (String[] par : new String[][]{{"Casa", "Caja"}, {"Comida", "Comida trabajo"},
                {"Ahorro 1", "Ahorro 2"}, {"Ahorro", "Ahorro1"}, {"Sol", "Solo"}, {"Comida", "Comiidaa"}}) {
            assertThat(NombreCategoria.coinciden(par[0], par[1])).as(par[0] + "/" + par[1]).isFalse();
        }
    }
}
