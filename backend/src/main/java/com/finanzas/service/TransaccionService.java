package com.finanzas.service;

import com.finanzas.dto.TransaccionRequest;
import com.finanzas.dto.TransaccionResponse;
import com.finanzas.model.Billetera;
import com.finanzas.model.Categoria;
import com.finanzas.model.Transaccion;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.TransaccionRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class TransaccionService {

    private final TransaccionRepository transaccionRepository;
    private final BilleteraRepository billeteraRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public TransaccionService(TransaccionRepository transaccionRepository,
                              BilleteraRepository billeteraRepository,
                              CategoriaRepository categoriaRepository,
                              UsuarioRepository usuarioRepository) {
        this.transaccionRepository = transaccionRepository;
        this.billeteraRepository = billeteraRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<TransaccionResponse> listarDeUsuario(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return transaccionRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    /**
     * Registra el movimiento y actualiza el saldo de la billetera en la misma transacción de BD.
     * La billetera se bloquea con FOR UPDATE, así que la validación de saldo y la resta son atómicas.
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

        return toResponse(transaccion);
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private TransaccionResponse toResponse(Transaccion transaccion) {
        return new TransaccionResponse(
                transaccion.getId(),
                transaccion.getMonto(),
                transaccion.getTipo(),
                transaccion.getDescripcion(),
                transaccion.getFecha(),
                transaccion.getBilletera().getId(),
                transaccion.getBilletera().getNombre(),
                transaccion.getCategoria().getId(),
                transaccion.getCategoria().getNombre()
        );
    }
}
