package com.finanzas.service;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.MetaAhorroResponse;
import com.finanzas.model.AporteMeta;
import com.finanzas.model.Billetera;
import com.finanzas.model.MetaAhorro;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Transaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.AporteMetaRepository;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.MetaAhorroRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

/**
 * Metas de ahorro. El dinero entra a una meta solo desde una billetera: con un aporte manual o
 * con el porcentaje automático de un ingreso. Cada movimiento descuenta o devuelve saldo a la
 * billetera y queda registrado en {@link AporteMeta}.
 *
 * Orden de bloqueos: billetera antes que meta. Todo método que toque ambas respeta ese orden.
 */
@Service
public class MetaAhorroService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final MetaAhorroRepository metaAhorroRepository;
    private final AporteMetaRepository aporteMetaRepository;
    private final BilleteraRepository billeteraRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaAhorroService(MetaAhorroRepository metaAhorroRepository,
                             AporteMetaRepository aporteMetaRepository,
                             BilleteraRepository billeteraRepository,
                             UsuarioRepository usuarioRepository) {
        this.metaAhorroRepository = metaAhorroRepository;
        this.aporteMetaRepository = aporteMetaRepository;
        this.billeteraRepository = billeteraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<MetaAhorroResponse> listarDeUsuario(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return metaAhorroRepository.findByUsuarioIdOrderByNombreAsc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public MetaAhorroResponse crear(String emailUsuario, MetaAhorroRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);
        validarPorcentaje(usuario, null, request.porcentajeAutomatico());

        MetaAhorro meta = new MetaAhorro(
                usuario,
                request.nombre().trim(),
                request.montoObjetivo(),
                request.fechaLimite(),
                request.porcentajeAutomatico()
        );
        metaAhorroRepository.save(meta);

        return toResponse(meta);
    }

    /** Cambia nombre, objetivo, fecha límite y porcentaje automático. El monto acumulado no se modifica. */
    @Transactional
    public MetaAhorroResponse actualizar(String emailUsuario, Long metaId, MetaAhorroRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);
        MetaAhorro meta = buscarDeUsuario(usuario, metaId);
        validarPorcentaje(usuario, metaId, request.porcentajeAutomatico());

        meta.actualizar(request.nombre().trim(), request.montoObjetivo(), request.fechaLimite(),
                request.porcentajeAutomatico());

        return toResponse(meta);
    }

    /** Aporte manual: sale de la billetera indicada. Un saldo insuficiente no deja ningún cambio parcial. */
    @Transactional
    public MetaAhorroResponse aportar(String emailUsuario, Long metaId, AporteRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Billetera billetera = billeteraRepository
                .bloquearActivaDeUsuario(request.billeteraId(), usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "La billetera no existe o está desactivada"));

        MetaAhorro meta = metaAhorroRepository.bloquearDeUsuario(metaId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La meta no existe"));

        if (billetera.getSaldoActual().compareTo(request.monto()) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Saldo insuficiente en la billetera");
        }

        billetera.aplicarMovimiento(TipoTransaccion.GASTO, request.monto());
        meta.aportar(request.monto());
        aporteMetaRepository.save(new AporteMeta(usuario, meta, billetera, null,
                request.monto(), LocalDate.now(), false));

        return toResponse(meta);
    }

    /**
     * Elimina la meta y devuelve lo aportado a cada billetera de origen, para que el dinero
     * no desaparezca al borrar la meta.
     */
    @Transactional
    public void eliminar(String emailUsuario, Long metaId) {
        Usuario usuario = buscarUsuario(emailUsuario);
        MetaAhorro meta = buscarDeUsuario(usuario, metaId);
        List<AporteMeta> aportes = aporteMetaRepository.findByMetaId(meta.getId());

        // Primero las billeteras (en orden de id), luego la meta: mismo orden que el resto de métodos
        aportes.stream()
                .map(aporte -> aporte.getBilletera().getId())
                .distinct()
                .sorted()
                .forEach(billeteraRepository::bloquearPorId);
        metaAhorroRepository.bloquearDeUsuario(meta.getId(), usuario.getId());

        for (AporteMeta aporte : aportes) {
            aporte.getBilletera().revertirMovimiento(TipoTransaccion.GASTO, aporte.getMonto());
        }
        aporteMetaRepository.deleteAll(aportes);
        metaAhorroRepository.delete(meta);
    }

    /**
     * Reparte un ingreso entre las metas con aporte automático activo. Lo llama el registro del
     * ingreso dentro de su misma transacción; la billetera ya está bloqueada por quien llama.
     * Cada parte se redondea hacia abajo, así la suma nunca supera el ingreso.
     */
    @Transactional
    public void aplicarAportesAutomaticos(Usuario usuario, Billetera billetera, Transaccion ingreso) {
        List<MetaAhorro> metas = metaAhorroRepository.bloquearAutomaticasDeUsuario(usuario.getId());

        for (MetaAhorro meta : metas) {
            if (meta.estaCompletada()) {
                continue;
            }

            BigDecimal monto = ingreso.getMonto()
                    .multiply(meta.getPorcentajeAutomatico())
                    .divide(CIEN, 2, RoundingMode.DOWN);
            if (monto.signum() <= 0) {
                continue;
            }

            billetera.aplicarMovimiento(TipoTransaccion.GASTO, monto);
            meta.aportar(monto);
            aporteMetaRepository.save(new AporteMeta(usuario, meta, billetera, ingreso,
                    monto, ingreso.getFecha(), true));
        }
    }

    /** Suma de los aportes automáticos que originó un ingreso. */
    @Transactional(readOnly = true)
    public BigDecimal sumaAportesAutomaticos(Long transaccionId) {
        return aporteMetaRepository.findByTransaccionId(transaccionId).stream()
                .map(AporteMeta::getMonto)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Al anular un ingreso, devuelve a su billetera los aportes automáticos que generó y los
     * quita de las metas. Quien llama debe tener ya bloqueada la billetera.
     */
    @Transactional
    public void devolverAportesAutomaticos(Transaccion ingreso, Billetera billetera) {
        List<AporteMeta> aportes = aporteMetaRepository.findByTransaccionId(ingreso.getId());

        for (AporteMeta aporte : aportes) {
            MetaAhorro meta = metaAhorroRepository.bloquearDeUsuario(
                    aporte.getMeta().getId(), ingreso.getUsuario().getId()).orElseThrow();
            meta.restar(aporte.getMonto());
            billetera.revertirMovimiento(TipoTransaccion.GASTO, aporte.getMonto());
        }
        aporteMetaRepository.deleteAll(aportes);
    }

    private void validarPorcentaje(Usuario usuario, Long metaIdActual, BigDecimal nuevoPorcentaje) {
        if (nuevoPorcentaje == null) {
            return;
        }

        BigDecimal otros = metaAhorroRepository.findByUsuarioIdAndPorcentajeAutomaticoIsNotNull(usuario.getId()).stream()
                .filter(meta -> !Objects.equals(meta.getId(), metaIdActual))
                .map(MetaAhorro::getPorcentajeAutomatico)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (otros.add(nuevoPorcentaje).compareTo(CIEN) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La suma de los porcentajes automáticos de tus metas no puede superar 100 %");
        }
    }

    private MetaAhorro buscarDeUsuario(Usuario usuario, Long metaId) {
        return metaAhorroRepository.findByIdAndUsuarioId(metaId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La meta no existe"));
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private MetaAhorroResponse toResponse(MetaAhorro meta) {
        BigDecimal porcentaje = meta.getMontoActual()
                .multiply(CIEN)
                .divide(meta.getMontoObjetivo(), 1, RoundingMode.HALF_UP);

        return new MetaAhorroResponse(
                meta.getId(),
                meta.getNombre(),
                meta.getMontoObjetivo(),
                meta.getMontoActual(),
                meta.getFechaLimite(),
                porcentaje,
                meta.estaCompletada(),
                meta.getPorcentajeAutomatico()
        );
    }
}
