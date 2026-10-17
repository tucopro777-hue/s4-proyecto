package com.finanzas.controller;

import com.finanzas.dto.TraspasoRequest;
import com.finanzas.dto.TraspasoResponse;
import com.finanzas.service.TraspasoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/traspasos")
public class TraspasoController {

    private final TraspasoService traspasoService;

    public TraspasoController(TraspasoService traspasoService) {
        this.traspasoService = traspasoService;
    }

    @GetMapping
    public ResponseEntity<List<TraspasoResponse>> listarDelUsuario(Authentication autenticacion) {
        return ResponseEntity.ok(traspasoService.listarDeUsuario(autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<TraspasoResponse> transferir(Authentication autenticacion,
                                                       @Valid @RequestBody TraspasoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(traspasoService.transferir(autenticacion.getName(), request));
    }
}
