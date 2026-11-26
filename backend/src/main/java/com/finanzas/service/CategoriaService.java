package com.finanzas.service;

import com.finanzas.dto.CategoriaRequest;
import com.finanzas.dto.CategoriaResponse;
import com.finanzas.model.Categoria;
import com.finanzas.model.TipoCategoria;
import com.finanzas.model.Usuario;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public CategoriaService(CategoriaRepository categoriaRepository,
                            UsuarioRepository usuarioRepository) {
        this.categoriaRepository = categoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    /** Lista las categorías activas del usuario; si se indica tipo, solo las de ese tipo. */
    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarDeUsuario(String emailUsuario, TipoCategoria tipo) {
        Usuario usuario = buscarUsuario(emailUsuario);

        List<Categoria> categorias = tipo == null
                ? categoriaRepository.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuario.getId())
                : categoriaRepository.findByUsuarioIdAndTipoAndActivaTrueOrderByNombreAsc(usuario.getId(), tipo);

        return categorias.stream().map(this::toResponse).toList();
    }

    @Transactional
    public CategoriaResponse crear(String emailUsuario, CategoriaRequest request) {
        Usuario usuario = buscarUsuario(emailUsuario);
        String nombre = request.nombre().trim();

        if (categoriaRepository.existsByUsuarioIdAndNombreIgnoreCase(usuario.getId(), nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya tienes una categoría con ese nombre");
        }

        Categoria categoria = new Categoria(nombre, request.tipo(), request.color(), request.icono(), usuario);
        categoriaRepository.save(categoria);

        return toResponse(categoria);
    }

    /** Desactivación lógica: la fila se conserva para no romper los gastos que la usan. */
    @Transactional
    public void desactivar(String emailUsuario, Long categoriaId) {
        Usuario usuario = buscarUsuario(emailUsuario);

        Categoria categoria = categoriaRepository.findByIdAndUsuarioId(categoriaId, usuario.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "La categoría no existe"));

        categoria.desactivar();
    }

    private Usuario buscarUsuario(String email) {
        // El token es válido pero el usuario ya no existe: se trata como no autenticado
        return usuarioRepository.findByEmail(email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getTipo(),
                categoria.getColor(),
                categoria.getIcono()
        );
    }
}
