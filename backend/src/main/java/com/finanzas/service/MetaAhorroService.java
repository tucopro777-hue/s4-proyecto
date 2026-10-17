package com.finanzas.service;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.MetaAhorroResponse;
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
import java.util.List;

/**
 * Metas de ahorro. El monto acumulado cambia solo con aportes explícitos; las transacciones
 * de la billetera no las modifican.
 */
@Service
public class MetaAhorroService {

    private static final BigDecimal CIEN = new BigDecimal("100");

    private final MetaAhorroRepository metaAhorroRepository;
    private final UsuarioRepository usuarioRepository;

    public MetaAhorroService(MetaAhorroRepository metaAhorroRepository,
                             UsuarioRepository usuarioRepository) {
        this.metaAhorroRepository = metaAhorroRepository;
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

        MetaAhorro meta = new MetaAhorro(
                usuario,
                request.nombre().trim(),
                request.montoObjetivo(),
                request.fechaLimite()
        );
        metaAhorroRepository.save(meta);

        return toResponse(meta);
    }

    /** Cambia nombre, objetivo y fecha límite. El monto acumulado no se modifica aquí. */
    @Transactional
    public MetaAhorroResponse actualizar(String emailUsuario, Long metaId, MetaAhorroRequest request) {
        MetaAhorro meta = buscarDeUsuario(emailUsuario, metaId);

        meta.actualizar(request.nombre().trim(), request.montoObjetivo(), request.fechaLimite());

        return toResponse(meta);
    }

    /** Registra un aporte explícito. Bloquea la meta para que aportes simultáneos se sumen bien. */
    @Transactional
    public MetaAhorroResponse aportar(String emailUsuario, Long metaId, AporteRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        MetaAhorro meta = metaAhorroRepository.bloquearDeUsuario(metaId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La meta no existe"));

        meta.aportar(request.monto());

        return toResponse(meta);
    }

    @Transactional
    public void eliminar(String emailUsuario, Long metaId) {
        MetaAhorro meta = buscarDeUsuario(emailUsuario, metaId);
        metaAhorroRepository.delete(meta);
    }

    private MetaAhorro buscarDeUsuario(String emailUsuario, Long metaId) {
        Usuario usuario = buscarUsuario(emailUsuario);

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
                meta.getMontoActual().compareTo(meta.getMontoObjetivo()) >= 0
        );
    }
}
