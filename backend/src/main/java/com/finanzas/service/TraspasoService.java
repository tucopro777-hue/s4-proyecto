package com.finanzas.service;

import com.finanzas.dto.TraspasoRequest;
import com.finanzas.dto.TraspasoResponse;
import com.finanzas.model.Billetera;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Traspaso;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.TraspasoRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

/**
 * Traspasos entre billeteras del mismo usuario.
 *
 * Atomicidad: todo el método es una única transacción de BD. Si cualquier paso falla con una
 * excepción de tiempo de ejecución (validación, saldo insuficiente, error de BD), Spring hace
 * rollback: el saldo de origen y destino vuelve a su valor anterior y no queda ningún traspaso.
 */
@Service
public class TraspasoService {

    private final TraspasoRepository traspasoRepository;
    private final BilleteraRepository billeteraRepository;
    private final UsuarioRepository usuarioRepository;

    public TraspasoService(TraspasoRepository traspasoRepository,
                           BilleteraRepository billeteraRepository,
                           UsuarioRepository usuarioRepository) {
        this.traspasoRepository = traspasoRepository;
        this.billeteraRepository = billeteraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<TraspasoResponse> listarDeUsuario(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return traspasoRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public TraspasoResponse transferir(String emailUsuario, TraspasoRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        if (request.billeteraOrigenId().equals(request.billeteraDestinoId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "La billetera de origen y la de destino deben ser distintas");
        }

        // Se bloquean ambas filas en orden ascendente de id. Así un traspaso A→B y otro B→A
        // simultáneos piden los bloqueos en el mismo orden y no pueden interbloquearse.
        Long primerId = Math.min(request.billeteraOrigenId(), request.billeteraDestinoId());
        Long segundoId = Math.max(request.billeteraOrigenId(), request.billeteraDestinoId());
        Billetera primera = bloquearActiva(primerId, usuario);
        Billetera segunda = bloquearActiva(segundoId, usuario);

        Billetera origen = primera.getId().equals(request.billeteraOrigenId()) ? primera : segunda;
        Billetera destino = origen == primera ? segunda : primera;

        if (!origen.getDivisa().equals(destino.getDivisa())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Las billeteras deben tener la misma divisa; no hay conversión entre divisas");
        }

        // Se valida antes de modificar nada: un saldo insuficiente no deja ningún cambio parcial
        if (origen.getSaldoActual().compareTo(request.monto()) < 0) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "Saldo insuficiente en la billetera de origen");
        }

        origen.aplicarMovimiento(TipoTransaccion.GASTO, request.monto());
        destino.aplicarMovimiento(TipoTransaccion.INGRESO, request.monto());

        Traspaso traspaso = new Traspaso(
                usuario,
                origen,
                destino,
                request.monto(),
                request.descripcion() == null ? null : request.descripcion().trim(),
                request.fecha()
        );
        traspasoRepository.save(traspaso);

        return toResponse(traspaso);
    }

    private Billetera bloquearActiva(Long billeteraId, Usuario usuario) {
        return billeteraRepository.bloquearActivaDeUsuario(billeteraId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Una de las billeteras no existe o está desactivada"));
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private TraspasoResponse toResponse(Traspaso traspaso) {
        return new TraspasoResponse(
                traspaso.getId(),
                traspaso.getMonto(),
                traspaso.getDescripcion(),
                traspaso.getFecha(),
                traspaso.getBilleteraOrigen().getId(),
                traspaso.getBilleteraOrigen().getNombre(),
                traspaso.getBilleteraDestino().getId(),
                traspaso.getBilleteraDestino().getNombre()
        );
    }
}
