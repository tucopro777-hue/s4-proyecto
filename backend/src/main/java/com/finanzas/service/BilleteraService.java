package com.finanzas.service;

import com.finanzas.dto.BilleteraRequest;
import com.finanzas.dto.BilleteraResponse;
import com.finanzas.model.Billetera;
import com.finanzas.model.Usuario;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class BilleteraService {

    private final BilleteraRepository billeteraRepository;
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public BilleteraService(BilleteraRepository billeteraRepository,
                            UsuarioRepository usuarioRepository,
                            PasswordEncoder passwordEncoder) {
        this.billeteraRepository = billeteraRepository;
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Transactional(readOnly = true)
    public List<BilleteraResponse> listarDeUsuario(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return billeteraRepository.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public BilleteraResponse crear(String emailUsuario, BilleteraRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);
        String nombre = request.nombre().trim();

        if (billeteraRepository.existsByUsuarioIdAndActivaTrueAndNombreIgnoreCase(usuario.getId(), nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una billetera activa con ese nombre");
        }

        Billetera billetera = new Billetera(
                nombre,
                request.saldoInicial(),
                request.divisa(),
                usuario
        );
        billeteraRepository.save(billetera);

        return toResponse(billetera);
    }

    /**
     * Desactivación lógica: la fila se conserva para no romper el historial de movimientos.
     * Exige la contraseña del usuario para confirmar la acción.
     */
    @Transactional
    public void desactivar(String emailUsuario, Long billeteraId, String password) {
        Usuario usuario = buscarUsuario(emailUsuario);

        if (!passwordEncoder.matches(password, usuario.getPassword())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Contraseña incorrecta");
        }

        Billetera billetera = billeteraRepository.findByIdAndUsuarioId(billeteraId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La billetera no existe"));

        billetera.desactivar();
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private BilleteraResponse toResponse(Billetera billetera) {
        return new BilleteraResponse(
                billetera.getId(),
                billetera.getNombre(),
                billetera.getSaldoInicial(),
                billetera.getSaldoActual(),
                billetera.getDivisa(),
                billetera.isActiva()
        );
    }
}
