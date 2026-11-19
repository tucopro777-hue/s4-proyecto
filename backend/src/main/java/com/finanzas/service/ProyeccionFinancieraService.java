package com.finanzas.service;

import com.finanzas.dto.ProyeccionFinancieraResponse;
import com.finanzas.dto.ProyeccionFinancieraResponse.MatrizTransicion;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.TransaccionRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Cadena de Markov de dos estados sobre el resultado mensual del usuario:
 * DEFICIT (ingresos menores que gastos) y ESTABLE (ingresos mayores o iguales que gastos).
 *
 * Usa los últimos 6 meses completos. Cuenta las transiciones entre meses consecutivos con
 * datos para armar la matriz P. Aplica suavizado de Laplace (+1 a cada conteo), así ningún
 * estado queda con probabilidad 0 o 1 por falta de datos. La distribución del próximo mes es
 * la fila de P correspondiente al estado actual.
 */
@Service
public class ProyeccionFinancieraService {

    private static final int MESES_HISTORIAL = 6;
    private static final int DEFICIT = 0;
    private static final int ESTABLE = 1;

    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public ProyeccionFinancieraService(TransaccionRepository transaccionRepository,
                                       UsuarioRepository usuarioRepository) {
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public ProyeccionFinancieraResponse proyectar(String emailUsuario) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        YearMonth ultimo = YearMonth.now().minusMonths(1);
        YearMonth primero = ultimo.minusMonths(MESES_HISTORIAL - 1);

        // Balance (ingresos menos gastos) de cada mes; las transacciones anuladas ya vienen excluidas
        List<Transaccion> movimientos = transaccionRepository.buscarConFiltros(
                usuario.getId(), primero.atDay(1), ultimo.atEndOfMonth(), null, null);
        Map<YearMonth, BigDecimal> balances = new HashMap<>();
        for (Transaccion t : movimientos) {
            BigDecimal montoConSigno = t.getTipo() == TipoTransaccion.INGRESO
                    ? t.getMonto() : t.getMonto().negate();
            balances.merge(YearMonth.from(t.getFecha()), montoConSigno, BigDecimal::add);
        }

        // Estado de cada mes que tiene datos, en orden cronológico
        List<Integer> estados = new ArrayList<>();
        for (YearMonth mes = primero; !mes.isAfter(ultimo); mes = mes.plusMonths(1)) {
            BigDecimal balance = balances.get(mes);
            if (balance != null) {
                estados.add(balance.signum() < 0 ? DEFICIT : ESTABLE);
            }
        }

        if (estados.size() < 2) {
            // Con menos de dos meses no hay transiciones que contar
            return new ProyeccionFinancieraResponse(false, estados.size(), null, null, null, null, null);
        }

        // Conteo de transiciones entre meses consecutivos: conteos[origen][destino]
        double[][] conteos = new double[2][2];
        for (int i = 1; i < estados.size(); i++) {
            conteos[estados.get(i - 1)][estados.get(i)]++;
        }

        // Matriz de transición con suavizado de Laplace: P[i][j] = (c[i][j] + 1) / (sum_j c[i][j] + 2)
        double[][] p = new double[2][2];
        for (int origen = 0; origen < 2; origen++) {
            double total = conteos[origen][DEFICIT] + conteos[origen][ESTABLE] + 2;
            for (int destino = 0; destino < 2; destino++) {
                p[origen][destino] = (conteos[origen][destino] + 1) / total;
            }
        }

        int actual = estados.get(estados.size() - 1);

        return new ProyeccionFinancieraResponse(
                true,
                estados.size(),
                actual == ESTABLE ? "ESTABLE" : "DEFICIT",
                redondear(p[actual][actual]),
                redondear(p[actual][ESTABLE]),
                redondear(p[actual][DEFICIT]),
                new MatrizTransicion(
                        redondear(p[DEFICIT][DEFICIT]), redondear(p[DEFICIT][ESTABLE]),
                        redondear(p[ESTABLE][DEFICIT]), redondear(p[ESTABLE][ESTABLE])));
    }

    private static double redondear(double valor) {
        return Math.round(valor * 10000.0) / 10000.0;
    }
}
