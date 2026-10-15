package com.finanzas.service;

import com.finanzas.dto.GastoRequest;
import com.finanzas.dto.GastoResponse;
import com.finanzas.model.Categoria;
import com.finanzas.model.Gasto;
import com.finanzas.model.Usuario;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.GastoRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class GastoService {

    private final GastoRepository gastoRepository;
    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public GastoService(GastoRepository gastoRepository,
                        CategoriaRepository categoriaRepository,
                        UsuarioRepository usuarioRepository) {
        this.gastoRepository = gastoRepository;
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<GastoResponse> listarDeUsuario(String emailUsuario) {
        Usuario usuario = buscarUsuario(emailUsuario);

        return gastoRepository.findByUsuarioIdOrderByFechaDescIdDesc(usuario.getId()).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public GastoResponse registrar(String emailUsuario, GastoRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Categoria categoria = categoriaRepository.findById(request.categoriaId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "La categoría no existe"));

        Gasto gasto = new Gasto(
                request.monto(),
                request.descripcion() == null ? null : request.descripcion().trim(),
                request.fecha(),
                usuario,
                categoria
        );
        gastoRepository.save(gasto);

        return toResponse(gasto);
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private GastoResponse toResponse(Gasto gasto) {
        return new GastoResponse(
                gasto.getId(),
                gasto.getMonto(),
                gasto.getDescripcion(),
                gasto.getFecha(),
                gasto.getCategoria().getId(),
                gasto.getCategoria().getNombre()
        );
    }
}
