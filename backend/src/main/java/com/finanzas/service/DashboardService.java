package com.finanzas.service;

import com.finanzas.dto.DashboardResponse;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.DashboardRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

/**
 * Calcula los indicadores del panel principal del usuario autenticado.
 * Solo lee datos: no modifica billeteras, transacciones ni presupuestos.
 */
@Service
public class DashboardService {

    private final DashboardRepository dashboardRepository;
    private final UsuarioRepository usuarioRepository;

    public DashboardService(DashboardRepository dashboardRepository,
                            UsuarioRepository usuarioRepository) {
        this.dashboardRepository = dashboardRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public DashboardResponse obtenerResumen(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        // Todos los importes del mes se calculan sobre el mes en curso
        YearMonth mes = YearMonth.now();
        LocalDate inicio = mes.atDay(1);
        LocalDate fin = mes.atEndOfMonth();

        BigDecimal saldoTotal = dashboardRepository.sumarSaldoActivo(usuario.getId());
        BigDecimal ingresosMes = dashboardRepository.sumarMovimientosPeriodo(
                usuario.getId(), TipoTransaccion.INGRESO, inicio, fin);
        BigDecimal gastosMes = dashboardRepository.sumarMovimientosPeriodo(
                usuario.getId(), TipoTransaccion.GASTO, inicio, fin);
        BigDecimal balanceMes = ingresosMes.subtract(gastosMes);
        Long presupuestosExcedidos = dashboardRepository.contarExcedidos(
                usuario.getId(), mes.getYear(), mes.getMonthValue());

        return new DashboardResponse(saldoTotal, ingresosMes, gastosMes, balanceMes, presupuestosExcedidos);
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
