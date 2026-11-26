package com.finanzas.service;

import com.finanzas.dto.*;
import com.finanzas.dto.VmeResponse.Evaluacion;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

/** Comparador de escenarios configurados por el usuario. No ejecuta inversiones ni transferencias. */
@Service
public class VmeService {
    public VmeResponse calcular(BigDecimal disponible, MarkovResponse markov, VmeRequest request) {
        if (!request.divisa().equals(markov.divisa())) throw invalido("La divisa no coincide");
        // Obtiene las probabilidades de los estados estable y déficit calculadas por Markov.
        var p = markov.probabilidades();
        if (p != null) validarProbabilidades(p);
        Set<String> nombres = new HashSet<>();
        List<Evaluacion> evaluaciones = new ArrayList<>();
        for (var a : request.alternativas()) {
            if (!nombres.add(a.nombre().trim().toLowerCase(Locale.ROOT))) throw invalido("Los nombres de alternativas deben ser únicos");
            BigDecimal liquidez = disponible.subtract(a.asignacion());
            // Calcula el saldo final de cada escenario sumando su ganancia o pérdida al saldo disponible.
            BigDecimal estable = disponible.add(a.resultadoEstable());
            BigDecimal deficit = disponible.add(a.resultadoDeficit());
            String motivo = disponible.signum() <= 0 ? "No hay saldo positivo disponible"
                    : liquidez.compareTo(request.reservaMinima()) < 0 ? "La asignación deja menos liquidez que la reserva mínima"
                    : estable.signum() < 0 || deficit.signum() < 0 ? "La pérdida de un escenario supera el saldo disponible" : "Cumple saldo y reserva mínima";
            // Comprueba que haya saldo positivo, se conserve la reserva y ningún escenario deje saldo negativo.
            boolean viable = disponible.signum() > 0 && liquidez.compareTo(request.reservaMinima()) >= 0
                    && estable.signum() >= 0 && deficit.signum() >= 0;
            // VME = P(estable) * saldo final estable + P(déficit) * saldo final en déficit.
            // Ejemplo: 0.70 * 1100 + 0.30 * 900 = 1040. Sin probabilidades no se calcula.
            BigDecimal vme = p == null ? null : p.estable().multiply(estable).add(p.deficit().multiply(deficit));
            evaluaciones.add(new Evaluacion(a.nombre().trim(), a.asignacion(), liquidez, a.resultadoEstable(), a.resultadoDeficit(),
                    estable, deficit, viable, motivo, vme));
        }
        // Busca el mayor VME entre las alternativas viables que tienen un resultado calculado.
        BigDecimal maximo = evaluaciones.stream().filter(e -> e.viable() && e.vme() != null)
                .map(Evaluacion::vme).max(BigDecimal::compareTo).orElse(null);
        // Selecciona todas las alternativas con ese máximo para conservar los empates.
        List<String> ganadoras = maximo == null ? List.of() : evaluaciones.stream()
                .filter(e -> e.viable() && e.vme().compareTo(maximo) == 0).map(Evaluacion::nombre).toList();
        String explicacion = p == null ? "Sin probabilidades estimables de Markov no se calcula VME ni se recomienda."
                : ganadoras.isEmpty() ? "Ninguna alternativa cumple las restricciones de saldo y reserva."
                : "Mayor patrimonio esperado entre las alternativas viables: " + String.join(", ", ganadoras)
                    + (ganadoras.size() > 1 ? ". Hay empate." : ".")
                    + " La comparación usa precisión interna antes de redondear a dos decimales.";
        // Redondea el VME a dos decimales para mostrarlo; las ganadoras se eligen antes de redondear.
        var salida = evaluaciones.stream().map(e -> new Evaluacion(e.nombre(), e.asignacion(), e.liquidezRestante(),
                e.resultadoEstable(), e.resultadoDeficit(), e.patrimonioEstable(), e.patrimonioDeficit(), e.viable(), e.motivo(),
                e.vme() == null ? null : e.vme().setScale(2, RoundingMode.HALF_UP))).toList();
        return new VmeResponse(request.divisa(), disponible, request.reservaMinima(), markov, salida, ganadoras,
                explicacion + " Simulación de un mes con pagos netos configurados por ti; no son tasas verificadas ni rendimientos garantizados. No mueve dinero.");
    }

    public void validarProbabilidades(MarkovResponse.Probabilidades p) {
        if (p.estable() == null || p.deficit() == null || p.estable().signum() < 0 || p.deficit().signum() < 0
                || p.estable().compareTo(BigDecimal.ONE) > 0 || p.deficit().compareTo(BigDecimal.ONE) > 0
                || p.estable().add(p.deficit()).compareTo(BigDecimal.ONE) != 0) {
            throw invalido("Las probabilidades deben estar entre 0 y 1 y sumar 1");
        }
    }

    private ResponseStatusException invalido(String mensaje) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, mensaje);
    }
}
