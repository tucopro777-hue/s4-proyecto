package com.finanzas.service;

import com.finanzas.dto.AlertaPresupuesto;
import com.finanzas.dto.TransaccionRequest;
import com.finanzas.dto.TransaccionResponse;
import com.finanzas.model.Billetera;
import com.finanzas.model.Categoria;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.TransaccionRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

<<<<<<< HEAD
import java.math.BigDecimal;
=======
import java.time.LocalDate;
>>>>>>> 7e3fa49b53a7fccdc404807142721c14f634a5d5
import java.util.List;

/**
 * Movimientos de ingreso y gasto. Cada alta, baja lógica (anulación) o cambio de saldo ocurre
 * dentro de una única transacción de BD junto con la billetera y el presupuesto afectados.
 */
@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final BilleteraRepository billeteraRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;
    private final PresupuestoService presupuestoService;
    private final MetaAhorroService metaAhorroService;

    public TransaccionService(TransaccionRepository transaccionRepository,
                              BilleteraRepository billeteraRepository,
                              CategoriaRepository categoriaRepository,
                              UsuarioRepository usuarioRepository,
                              PresupuestoService presupuestoService,
                              MetaAhorroService metaAhorroService) {
        this.transaccionRepository = transaccionRepository;
        this.billeteraRepository = billeteraRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
        this.presupuestoService = presupuestoService;
        this.metaAhorroService = metaAhorroService;
    }

    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarDeUsuario(String emailUsuario) {
        // Sin filtros: se reutiliza el listado predeterminado original
        return listarConFiltros(emailUsuario, null, null, null, null);
    }

    /**
     * Lista los movimientos del usuario aplicando los filtros opcionales recibidos.
     * Los filtros se combinan entre sí (AND) y SIEMPRE se excluyen las anuladas.
     * Si no llega ningún filtro, se devuelve el listado predeterminado completo.
     */
    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarConFiltros(String emailUsuario,
                                                      LocalDate desde,
                                                      LocalDate hasta,
                                                      Long categoriaId,
                                                      TipoTransaccion tipo) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return transaccionRepository
                .buscarConFiltros(usuario.getId(), desde, hasta, categoriaId, tipo)
                .stream()
                .map(transaccion -> toResponse(transaccion, null))
                .toList();
    }

    /**
     * Registra el movimiento, actualiza el saldo de la billetera y, si es un gasto, su presupuesto.
     * Un gasto que supera el presupuesto se registra igualmente y se devuelve una alerta.
     */
    @Transactional
    public TransaccionResponse registrar(String emailUsuario, TransaccionRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Billetera billetera = billeteraRepository
                .bloquearActivaDeUsuario(request.billeteraId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La billetera no existe o está desactivada"));

        Categoria categoria = categoriaRepository
                .findByIdAndUsuarioIdAndActivaTrue(request.categoriaId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe o está desactivada"));

        // TipoCategoria y TipoTransaccion tienen los mismos valores; se compara por nombre
        if (!categoria.getTipo().name().equals(request.tipo().name())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no corresponde al tipo del movimiento");
        }

        if (request.tipo() == TipoTransaccion.GASTO
                && billetera.getSaldoActual().compareTo(request.monto()) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Saldo insuficiente en la billetera");
        }

        billetera.aplicarMovimiento(request.tipo(), request.monto());

        String descripcion = request.descripcion() == null ? null : request.descripcion().trim();
        Transaccion transaccion = new Transaccion(
                request.monto(),
                request.tipo(),
                descripcion,
                request.fecha(),
                usuario,
                billetera,
                categoria
        );
        transaccionRepository.save(transaccion);

        AlertaPresupuesto alerta = null;
        if (request.tipo() == TipoTransaccion.GASTO) {
            alerta = presupuestoService.registrarGasto(usuario, categoria, request.monto(), request.fecha());
        } else {
            // Los ingresos pueden derivar un porcentaje automático a las metas de ahorro
            metaAhorroService.aplicarAportesAutomaticos(usuario, billetera, transaccion);
        }

        return toResponse(transaccion, alerta);
    }

    /**
     * Anula un movimiento: revierte su efecto en la billetera y, si es gasto, en el presupuesto.
     * La fila se conserva marcada como anulada para auditoría.
     */
    @Transactional
    public void anular(String emailUsuario, Long transaccionId) {
        Usuario usuario = buscarUsuario(emailUsuario);

        // Primero se bloquea el movimiento; luego billetera y presupuesto, siempre en ese orden
        Transaccion transaccion = transaccionRepository.bloquearDeUsuario(transaccionId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "El movimiento no existe"));

        if (transaccion.isAnulada()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El movimiento ya está anulado");
        }

        Billetera billetera = billeteraRepository.bloquearPorId(transaccion.getBilletera().getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La billetera del movimiento no existe"));

        // Anular un ingreso le quita dinero, pero antes vuelven los aportes automáticos que generó
        BigDecimal aportesAutomaticos = BigDecimal.ZERO;
        if (transaccion.getTipo() == TipoTransaccion.INGRESO) {
            aportesAutomaticos = metaAhorroService.sumaAportesAutomaticos(transaccion.getId());
            if (billetera.getSaldoActual().add(aportesAutomaticos).compareTo(transaccion.getMonto()) < 0) {
                throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "El saldo actual de la billetera no permite anular este ingreso");
            }
            metaAhorroService.devolverAportesAutomaticos(transaccion, billetera);
        }

        billetera.revertirMovimiento(transaccion.getTipo(), transaccion.getMonto());

        if (transaccion.getTipo() == TipoTransaccion.GASTO) {
            presupuestoService.revertirGasto(usuario, transaccion.getCategoria(),
                    transaccion.getMonto(), transaccion.getFecha());
        }

        transaccion.anular();
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private TransaccionResponse toResponse(Transaccion transaccion, AlertaPresupuesto alerta) {
        return new TransaccionResponse(
                transaccion.getId(),
                transaccion.getMonto(),
                transaccion.getTipo(),
                transaccion.getDescripcion(),
                transaccion.getFecha(),
                transaccion.getBilletera().getId(),
                transaccion.getBilletera().getNombre(),
                transaccion.getCategoria().getId(),
                transaccion.getCategoria().getNombre(),
                alerta
        );
    }
}
