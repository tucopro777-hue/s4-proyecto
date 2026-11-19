package com.finanzas.service;

import com.finanzas.dto.AsignacionResponse;
import com.finanzas.dto.MetaFinanciera;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.Usuario;
import com.finanzas.repository.MetaAhorroRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Asignación inteligente del dinero disponible: problema de la mochila 0/1 resuelto con
 * programación dinámica.
 *
 * Modelo: capacidad = dinero disponible; objetos = metas pendientes; peso = costo pendiente;
 * valor = prioridad (1 a 10). Se maximiza la suma de prioridades sin exceder el dinero.
 *
 * Para usar una matriz de enteros, los montos se trabajan en centavos. La capacidad se trunca
 * hacia abajo y cada costo se redondea hacia arriba, así nunca se recomienda gastar de más.
 */
@Service
public class PresupuestoOptimizerService {

    private static final int PRIORIDAD_MIN = 1;
    private static final int PRIORIDAD_MAX = 10;
    /** Límite de celdas de la matriz (objetos + 1) × (capacidad + 1) para no agotar la memoria. */
    private static final long MAX_CELDAS = 20_000_000L;

    private final MetaAhorroRepository metaAhorroRepository;
    private final UsuarioRepository usuarioRepository;

    public PresupuestoOptimizerService(MetaAhorroRepository metaAhorroRepository,
                                       UsuarioRepository usuarioRepository) {
        this.metaAhorroRepository = metaAhorroRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Toma las metas pendientes del usuario y devuelve la combinación recomendada con su resumen. */
    @Transactional(readOnly = true)
    public AsignacionResponse recomendar(String emailUsuario, double presupuestoDisponible) {
        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        List<MetaFinanciera> pendientes = metaAhorroRepository.findByUsuarioIdOrderByNombreAsc(usuario.getId()).stream()
                .filter(meta -> !meta.estaCompletada())
                .map(this::aMetaFinanciera)
                .toList();

        List<MetaFinanciera> elegidas = optimizar(presupuestoDisponible, pendientes);

        BigDecimal total = elegidas.stream()
                .map(MetaFinanciera::costo)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int puntos = elegidas.stream().mapToInt(MetaFinanciera::prioridad).sum();
        BigDecimal sobrante = BigDecimal.valueOf(presupuestoDisponible).subtract(total);

        return new AsignacionResponse(elegidas, total, puntos, sobrante);
    }

    /**
     * Devuelve el subconjunto de metas que maximiza la suma de prioridades sin superar el
     * presupuesto. Construye la matriz dp[i][w] (mejor puntaje usando las primeras i metas con
     * capacidad w) y reconstruye la elección desde dp[n][C].
     */
    public List<MetaFinanciera> optimizar(double presupuestoDisponible, List<MetaFinanciera> metas) {
        if (!Double.isFinite(presupuestoDisponible) || presupuestoDisponible < 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El presupuesto disponible no es válido");
        }

        int n = metas.size();
        long[] pesos = new long[n];
        int[] valores = new int[n];
        long sumaPesos = 0;

        for (int i = 0; i < n; i++) {
            MetaFinanciera meta = metas.get(i);
            if (meta.prioridad() < PRIORIDAD_MIN || meta.prioridad() > PRIORIDAD_MAX) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La prioridad de '" + meta.nombre() + "' debe estar entre 1 y 10");
            }
            if (meta.costo() == null || meta.costo().signum() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "El costo de '" + meta.nombre() + "' debe ser mayor que cero");
            }
            // Costo en centavos, redondeado hacia arriba: nunca se subestima lo que cuesta
            pesos[i] = meta.costo().movePointRight(2).setScale(0, RoundingMode.UP).longValueExact();
            valores[i] = meta.prioridad();
            sumaPesos += pesos[i];
        }

        // Capacidad en centavos, truncada hacia abajo. No sirve más que la suma de todos los costos.
        long capacidadLarga = BigDecimal.valueOf(presupuestoDisponible).movePointRight(2)
                .setScale(0, RoundingMode.DOWN)
                .min(BigDecimal.valueOf(sumaPesos))
                .longValueExact();

        // Se valida el tamaño antes de convertir a int para que la matriz nunca desborde
        if ((long) (n + 1) * (capacidadLarga + 1) > MAX_CELDAS) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "El presupuesto o el número de metas es demasiado grande para calcular la asignación");
        }
        int capacidad = (int) capacidadLarga;

        // dp[i][w]: mejor suma de prioridades con las primeras i metas y capacidad w
        int[][] dp = new int[n + 1][capacidad + 1];
        for (int i = 1; i <= n; i++) {
            int peso = (int) pesos[i - 1];
            int valor = valores[i - 1];
            for (int w = 0; w <= capacidad; w++) {
                dp[i][w] = dp[i - 1][w];
                if (peso <= w && dp[i - 1][w - peso] + valor > dp[i][w]) {
                    dp[i][w] = dp[i - 1][w - peso] + valor;
                }
            }
        }

        // Reconstrucción: si el valor cambió al considerar la meta i, esa meta fue elegida
        List<MetaFinanciera> elegidas = new ArrayList<>();
        int w = capacidad;
        for (int i = n; i >= 1; i--) {
            if (dp[i][w] != dp[i - 1][w]) {
                elegidas.add(metas.get(i - 1));
                w -= (int) pesos[i - 1];
            }
        }
        Collections.reverse(elegidas);
        return elegidas;
    }

    private MetaFinanciera aMetaFinanciera(MetaAhorro meta) {
        BigDecimal pendiente = meta.getMontoObjetivo().subtract(meta.getMontoActual());
        return new MetaFinanciera(meta.getId(), meta.getNombre(), pendiente, meta.getPrioridad());
    }
}
