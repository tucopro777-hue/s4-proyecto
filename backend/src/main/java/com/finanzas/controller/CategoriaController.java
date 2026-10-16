package com.finanzas.controller;

import com.finanzas.dto.CategoriaRequest;
import com.finanzas.dto.CategoriaResponse;
import com.finanzas.model.TipoCategoria;
import com.finanzas.service.CategoriaService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/categorias")
public class CategoriaController {

    private final CategoriaService categoriaService;

    public CategoriaController(CategoriaService categoriaService) {
        this.categoriaService = categoriaService;
    }

    @GetMapping
    public ResponseEntity<List<CategoriaResponse>> listarDelUsuario(
            Authentication autenticacion,
            @RequestParam(required = false) TipoCategoria tipo) {
        return ResponseEntity.ok(categoriaService.listarDeUsuario(autenticacion.getName(), tipo));
    }

    @PostMapping
    public ResponseEntity<CategoriaResponse> crear(Authentication autenticacion,
                                                   @Valid @RequestBody CategoriaRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(categoriaService.crear(autenticacion.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(Authentication autenticacion, @PathVariable Long id) {
        categoriaService.desactivar(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
