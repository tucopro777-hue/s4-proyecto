package com.finanzas.service;

import com.finanzas.dto.MarkovResponse;
import com.finanzas.dto.MarkovResponse.*;
import com.finanzas.model.Transaccion;
import com.finanzas.model.TipoTransaccion;
import org.springframework.stereotype.Service;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.util.*;

/** Estimación empírica de primer orden, sin suavizado ni probabilidades de respaldo. */
@Service
public class MarkovService {
    public MarkovResponse calcular(List<Transaccion> movimientos, String divisa, java.time.LocalDate hoy) {
        YearMonth actual = YearMonth.from(hoy);
        SortedMap<YearMonth, BigDecimal[]> totales = new TreeMap<>();
        for (var t : movimientos) {
            var mes = YearMonth.from(t.getFecha());
            if (t.isAnulada() || !divisa.equals(t.getBilletera().getDivisa())
                    || t.getFecha().isAfter(hoy)) continue;
            var sumas = totales.computeIfAbsent(mes, k -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int indice = t.getTipo() == TipoTransaccion.INGRESO ? 0 : 1;
            sumas[indice] = sumas[indice].add(t.getMonto());
        }
        List<Mes> historial = new ArrayList<>();
        long[][] frecuencias = new long[2][2];
        Mes anterior = null;
        for (var entrada : totales.entrySet()) {
            if (!entrada.getKey().isBefore(actual)) continue;
            Mes mes = mes(entrada.getKey(), entrada.getValue());
            if (anterior != null && YearMonth.parse(anterior.mes()).plusMonths(1).equals(entrada.getKey())) {
                frecuencias[anterior.estado().ordinal()][mes.estado().ordinal()]++;
            }
            historial.add(mes);
            anterior = mes;
        }
        List<Fila> filas = new ArrayList<>();
        for (var estado : Estado.values()) {
            long e = frecuencias[estado.ordinal()][0], d = frecuencias[estado.ordinal()][1], total = e + d;
            BigDecimal pe = total == 0 ? null : BigDecimal.valueOf(e).divide(BigDecimal.valueOf(total), 12, RoundingMode.HALF_UP);
            filas.add(new Fila(estado, e, d, total, pe, pe == null ? null : BigDecimal.ONE.subtract(pe)));
        }
        // La matriz usa solo meses cerrados; la fila para el próximo mes se elige
        // con el estado provisional del mes en curso, declarado explícitamente en la UI.
        YearMonth base = actual;
        Mes ultimo = totales.containsKey(actual) ? mes(actual, totales.get(actual)) : null;
        Probabilidades probabilidades = null;
        String mensaje;
        if (ultimo == null || !ultimo.mes().equals(base.toString())) {
            mensaje = "Faltan movimientos del mes actual (" + base + "). No se puede elegir su estado para predecir el próximo mes.";
        } else {
            Fila fila = filas.get(ultimo.estado().ordinal());
            if (fila.total() == 0) {
                mensaje = "Falta al menos una transición entre meses cerrados consecutivos que salga del estado "
                        + ultimo.estado() + ". Registra más historial; no se inventan probabilidades.";
            } else {
                probabilidades = new Probabilidades(fila.pEstable(), fila.pDeficit());
                mensaje = "Estimación para " + actual.plusMonths(1) + " desde " + base + " (" + ultimo.estado()
                        + "), basada en " + fila.total() + " transiciones de ese estado. Una muestra pequeña es poco concluyente.";
            }
        }
        mensaje += " Solo meses cerrados con registros; los huecos no forman transiciones. El mes en curso es provisional."
                + " Se supone que los registros mensuales están completos y que el patrón permanece; no es una garantía.";
        return new MarkovResponse(divisa, actual.toString(), totales.containsKey(actual) ? mes(actual, totales.get(actual)) : null,
                base.toString(), actual.plusMonths(1).toString(), ultimo != null ? ultimo.estado().name() : null,
                historial, filas, probabilidades, mensaje);
    }

    private Mes mes(YearMonth periodo, BigDecimal[] sumas) {
        return new Mes(periodo.toString(), sumas[0], sumas[1],
                sumas[0].compareTo(sumas[1]) >= 0 ? Estado.ESTABLE : Estado.DEFICIT);
    }
}
