package com.finanzas.service;

import com.finanzas.dto.ActualizarPresupuestoRequest;
import com.finanzas.dto.AlertaPresupuesto;
import com.finanzas.dto.PresupuestoRequest;
import com.finanzas.dto.PresupuestoResponse;
import com.finanzas.model.Categoria;
import com.finanzas.model.Presupuesto;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.TipoCategoria;
import com.finanzas.model.Usuario;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.PresupuestoRepository;
import com.finanzas.repository.TransaccionRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

/**
 * Presupuestos mensuales por categoría de gasto. Las transacciones llaman a
 * {@link #registrarGasto} y {@link #revertirGasto} para mantener el consumo al día.
 */
@Service
public class PresupuestoService {

    /** Porcentaje a partir del cual se emite una advertencia antes de exceder el límite. */
    private static final BigDecimal UMBRAL_ADVERTENCIA = new BigDecimal("80");
    private static final BigDecimal CIEN = new BigDecimal("100");

    private final PresupuestoRepository presupuestoRepository;
    private final CategoriaRepository categoriaRepository;
    private final TransaccionRepository transaccionRepository;
    private final UsuarioRepository usuarioRepository;

    public PresupuestoService(PresupuestoRepository presupuestoRepository,
                              CategoriaRepository categoriaRepository,
                              TransaccionRepository transaccionRepository,
                              UsuarioRepository usuarioRepository) {
        this.presupuestoRepository = presupuestoRepository;
        this.categoriaRepository = categoriaRepository;
        this.transaccionRepository = transaccionRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<PresupuestoResponse> listarDeUsuario(String emailUsuario, int anio, int mes) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return presupuestoRepository
                .findByUsuarioIdAndAnioAndMesOrderByCategoriaNombreAsc(usuario.getId(), anio, mes)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Crea el presupuesto de un periodo. El consumo inicial se calcula con los gastos ya
     * registrados en ese mes, así que crear el presupuesto tarde no pierde movimientos.
     */
    @Transactional
    public PresupuestoResponse crear(String emailUsuario, PresupuestoRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Categoria categoria = categoriaRepository
                .findByIdAndUsuarioIdAndActivaTrue(request.categoriaId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe o está desactivada"));

        if (categoria.getTipo() != TipoCategoria.GASTO) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Los presupuestos solo aplican a categorías de gasto");
        }

        YearMonth periodo = YearMonth.of(request.anio(), request.mes());
        if (presupuestoRepository.existsByUsuarioIdAndCategoriaIdAndAnioAndMes(
                usuario.getId(), categoria.getId(), periodo.getYear(), periodo.getMonthValue())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe un presupuesto para esa categoría en ese mes");
        }

        BigDecimal consumidoInicial = transaccionRepository.sumarMovimientos(
                usuario.getId(), categoria.getId(), TipoTransaccion.GASTO,
                periodo.atDay(1), periodo.atEndOfMonth());

        Presupuesto presupuesto = new Presupuesto(usuario, categoria, request.montoLimite(), periodo, consumidoInicial);
        presupuestoRepository.save(presupuesto);

        return toResponse(presupuesto);
    }

    /** Cambia el límite del presupuesto; el consumo acumulado no se toca. */
    @Transactional
    public PresupuestoResponse actualizar(String emailUsuario, Long presupuestoId, ActualizarPresupuestoRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El presupuesto no existe"));

        presupuesto.cambiarLimite(request.montoLimite());

        return toResponse(presupuesto);
    }

    @Transactional
    public void eliminar(String emailUsuario, Long presupuestoId) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Presupuesto presupuesto = presupuestoRepository.findByIdAndUsuarioId(presupuestoId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El presupuesto no existe"));

        presupuestoRepository.delete(presupuesto);
    }

    /**
     * Suma un gasto al presupuesto de la categoría en el mes de la fecha del movimiento.
     * Devuelve la alerta si el consumo llega al umbral o supera el límite; si no hay presupuesto
     * para ese mes, devuelve null. Nunca bloquea el gasto: solo avisa.
     */
    @Transactional
    public AlertaPresupuesto registrarGasto(Usuario usuario, Categoria categoria, BigDecimal monto, LocalDate fecha) {
        YearMonth periodo = YearMonth.from(fecha);

        return presupuestoRepository
                .bloquearPeriodo(usuario.getId(), categoria.getId(), periodo.getYear(), periodo.getMonthValue())
                .map(presupuesto -> {
                    presupuesto.registrarConsumo(monto);
                    return evaluar(presupuesto);
                })
                .orElse(null);
    }

    /** Devuelve al presupuesto el importe de un gasto anulado. */
    @Transactional
    public void revertirGasto(Usuario usuario, Categoria categoria, BigDecimal monto, LocalDate fecha) {
        YearMonth periodo = YearMonth.from(fecha);

        presupuestoRepository
                .bloquearPeriodo(usuario.getId(), categoria.getId(), periodo.getYear(), periodo.getMonthValue())
                .ifPresent(presupuesto -> presupuesto.revertirConsumo(monto));
    }

    private AlertaPresupuesto evaluar(Presupuesto presupuesto) {
        String nivel = nivelDe(presupuesto);
        if ("OK".equals(nivel)) {
            return null;
        }

        String categoria = presupuesto.getCategoria().getNombre();
        String mensaje = "EXCEDIDO".equals(nivel)
                ? "Has superado el presupuesto de " + categoria + " para " + presupuesto.getPeriodo() + "."
                : "Llevas el 80 % o más del presupuesto de " + categoria + " para " + presupuesto.getPeriodo() + ".";

        return new AlertaPresupuesto(nivel, mensaje, presupuesto.getSaldoConsumido(), presupuesto.getMontoLimite());
    }

    private static String nivelDe(Presupuesto presupuesto) {
        BigDecimal consumido = presupuesto.getSaldoConsumido();
        BigDecimal limite = presupuesto.getMontoLimite();

        if (consumido.compareTo(limite) > 0) {
            return "EXCEDIDO";
        }
        // consumido * 100 >= límite * 80 evita dividir y perder precisión con decimales
        if (consumido.multiply(CIEN).compareTo(limite.multiply(UMBRAL_ADVERTENCIA)) >= 0) {
            return "ADVERTENCIA";
        }
        return "OK";
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private PresupuestoResponse toResponse(Presupuesto presupuesto) {
        return new PresupuestoResponse(
                presupuesto.getId(),
                presupuesto.getCategoria().getId(),
                presupuesto.getCategoria().getNombre(),
                presupuesto.getMontoLimite(),
                presupuesto.getSaldoConsumido(),
                presupuesto.getPeriodo().getYear(),
                presupuesto.getPeriodo().getMonthValue(),
                nivelDe(presupuesto)
        );
    }
}
