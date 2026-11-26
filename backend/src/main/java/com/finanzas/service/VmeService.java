package com.finanzas.service;

import com.finanzas.dto.ProyeccionFinancieraResponse;
import com.finanzas.dto.VmeResponse;
import com.finanzas.dto.VmeResponse.Alternativa;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Valor Monetario Esperado de tres decisiones: mantener el gasto actual o recortarlo 10 % o 20 %.
 *
 * Las probabilidades del próximo mes salen de la cadena de Markov ({@link ProyeccionFinancieraService}).
 * El pago de cada estado sale del histórico de los últimos 6 meses completos: el balance promedio
 * de los meses en ese estado más el ahorro de recortar el gasto promedio de ese estado.
 *
 * VME(alternativa) = P(ESTABLE) × pago(ESTABLE) + P(DEFICIT) × pago(DEFICIT)
 */
@Service
public class VmeService {

    private static final int MESES_HISTORIAL = 6;
    private static final int DEFICIT = 0;
    private static final int ESTABLE = 1;
    /** Fracciones de recorte de gastos que se comparan como decisiones. */
    private static final double[] REDUCCIONES = {0.0, 0.10, 0.20};

    private final ProyeccionFinancieraService proyeccionFinancieraService;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public VmeService(ProyeccionFinancieraService proyeccionFinancieraService,
                      TransaccionRepository transaccionRepository,
                      UsuarioRepository usuarioRepository) {
        this.proyeccionFinancieraService = proyeccionFinancieraService;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public VmeResponse calcular(String emailUsuario) {
        ProyeccionFinancieraResponse markov = proyeccionFinancieraService.proyectar(emailUsuario);
        if (!markov.datosSuficientes()) {
            return new VmeResponse(false, null, List.of(), null);
        }

        Usuario usuario = usuarioRepository.findByEmail(emailUsuario)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));

        YearMonth ultimo = YearMonth.now().minusMonths(1);
        YearMonth primero = ultimo.minusMonths(MESES_HISTORIAL - 1);
        List<Transaccion> movimientos = transaccionRepository.buscarConFiltros(
                usuario.getId(), primero.atDay(1), ultimo.atEndOfMonth(), null, null);

        // Ingresos [0] y gastos [1] de cada mes
        Map<YearMonth, BigDecimal[]> porMes = new HashMap<>();
        for (Transaccion t : movimientos) {
            BigDecimal[] totales = porMes.computeIfAbsent(YearMonth.from(t.getFecha()),
                    mes -> new BigDecimal[]{BigDecimal.ZERO, BigDecimal.ZERO});
            int indice = t.getTipo() == TipoTransaccion.INGRESO ? 0 : 1;
            totales[indice] = totales[indice].add(t.getMonto());
        }

        // Suma de balances y de gastos, y número de meses, por estado
        double[] sumaBalance = new double[2];
        double[] sumaGasto = new double[2];
        int[] meses = new int[2];
        for (BigDecimal[] totales : porMes.values()) {
            BigDecimal balance = totales[0].subtract(totales[1]);
            int estado = balance.signum() < 0 ? DEFICIT : ESTABLE;
            sumaBalance[estado] += balance.doubleValue();
            sumaGasto[estado] += totales[1].doubleValue();
            meses[estado]++;
        }

        // Sin meses de ambos estados no hay pago histórico que promediar para cada uno
        if (meses[DEFICIT] == 0 || meses[ESTABLE] == 0) {
            return new VmeResponse(false, markov.estadoActual(), List.of(), null);
        }

        double pagoBaseEstable = sumaBalance[ESTABLE] / meses[ESTABLE];
        double pagoBaseDeficit = sumaBalance[DEFICIT] / meses[DEFICIT];
        double gastoEstable = sumaGasto[ESTABLE] / meses[ESTABLE];
        double gastoDeficit = sumaGasto[DEFICIT] / meses[DEFICIT];
        double probEstable = markov.probabilidadEstableSiguiente();
        double probDeficit = markov.probabilidadDeficitSiguiente();

        List<Alternativa> alternativas = new ArrayList<>();
        for (double reduccion : REDUCCIONES) {
            double pagoEstable = pagoBaseEstable + reduccion * gastoEstable;
            double pagoDeficit = pagoBaseDeficit + reduccion * gastoDeficit;
            double vme = probEstable * pagoEstable + probDeficit * pagoDeficit;
            alternativas.add(new Alternativa(
                    nombreAlternativa(reduccion), reduccion,
                    redondear(pagoEstable), redondear(pagoDeficit), redondear(vme)));
        }

        Alternativa mejor = alternativas.stream()
                .max(Comparator.comparingDouble(Alternativa::vme))
                .orElseThrow();

        return new VmeResponse(true, markov.estadoActual(), alternativas, mejor.nombre());
    }

    private static String nombreAlternativa(double reduccion) {
        return reduccion == 0.0
                ? "Mantener gasto actual"
                : "Reducir gastos " + Math.round(reduccion * 100) + " %";
    }

    private static double redondear(double valor) {
        return Math.round(valor * 100.0) / 100.0;
    }
}
