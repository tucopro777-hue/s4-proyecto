package com.finanzas.service;

import com.finanzas.dto.CategoriaRequest;
import com.finanzas.dto.CategoriaResponse;
import com.finanzas.model.Categoria;
import com.finanzas.repository.CategoriaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class CategoriaService {

    private final CategoriaRepository categoriaRepository;

    public CategoriaService(CategoriaRepository categoriaRepository) {
        this.categoriaRepository = categoriaRepository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listar() {
        return categoriaRepository.findAllByOrderByNombreAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public CategoriaResponse crear(CategoriaRequest request) {
        String nombre = request.nombre().trim();

        if (categoriaRepository.existsByNombre(nombre)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Ya existe una categoría con ese nombre");
        }

        Categoria categoria = new Categoria(nombre, request.color(), request.tipo().trim());
        categoriaRepository.save(categoria);

        return toResponse(categoria);
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.getId(),
                categoria.getNombre(),
                categoria.getColor(),
                categoria.getTipo()
        );
    }
}
