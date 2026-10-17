package com.finanzas.controller;

import com.finanzas.dto.AporteRequest;
import com.finanzas.dto.MetaAhorroRequest;
import com.finanzas.dto.MetaAhorroResponse;
import com.finanzas.service.MetaAhorroService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/metas")
public class MetaAhorroController {

    private final MetaAhorroService metaAhorroService;

    public MetaAhorroController(MetaAhorroService metaAhorroService) {
        this.metaAhorroService = metaAhorroService;
    }

    @GetMapping
    public ResponseEntity<List<MetaAhorroResponse>> listarDelUsuario(Authentication autenticacion) {
        return ResponseEntity.ok(metaAhorroService.listarDeUsuario(autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<MetaAhorroResponse> crear(Authentication autenticacion,
                                                    @Valid @RequestBody MetaAhorroRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(metaAhorroService.crear(autenticacion.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MetaAhorroResponse> actualizar(Authentication autenticacion,
                                                         @PathVariable Long id,
                                                         @Valid @RequestBody MetaAhorroRequest request) {
        return ResponseEntity.ok(metaAhorroService.actualizar(autenticacion.getName(), id, request));
    }

    /** Único camino que suma dinero a la meta: un aporte explícito. */
    @PostMapping("/{id}/aportes")
    public ResponseEntity<MetaAhorroResponse> aportar(Authentication autenticacion,
                                                      @PathVariable Long id,
                                                      @Valid @RequestBody AporteRequest request) {
        return ResponseEntity.ok(metaAhorroService.aportar(autenticacion.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication autenticacion, @PathVariable Long id) {
        metaAhorroService.eliminar(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
