package com.finanzas.controller;

import com.finanzas.dto.GastoRequest;
import com.finanzas.dto.GastoResponse;
import com.finanzas.service.GastoService;
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
@RequestMapping("/api/gastos")
public class GastoController {

    private final GastoService gastoService;

    public GastoController(GastoService gastoService) {
        this.gastoService = gastoService;
    }

    @GetMapping
    public ResponseEntity<List<GastoResponse>> listarDelUsuario(Authentication autenticacion) {
        return ResponseEntity.ok(gastoService.listarDeUsuario(autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<GastoResponse> registrar(Authentication autenticacion,
                                                   @Valid @RequestBody GastoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(gastoService.registrar(autenticacion.getName(), request));
    }
}
