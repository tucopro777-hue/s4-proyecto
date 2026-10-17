package com.finanzas.controller;

import com.finanzas.dto.TransaccionRequest;
import com.finanzas.dto.TransaccionResponse;
import com.finanzas.service.TransaccionService;
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
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    @GetMapping
    public ResponseEntity<List<TransaccionResponse>> listarDelUsuario(Authentication autenticacion) {
        return ResponseEntity.ok(transaccionService.listarDeUsuario(autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<TransaccionResponse> registrar(Authentication autenticacion,
                                                         @Valid @RequestBody TransaccionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(transaccionService.registrar(autenticacion.getName(), request));
    }

    /** Anulación lógica: revierte el saldo de la billetera y el consumo del presupuesto. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> anular(Authentication autenticacion, @PathVariable Long id) {
        transaccionService.anular(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
