package com.finanzas.controller;

import com.finanzas.dto.BilleteraRequest;
import com.finanzas.dto.BilleteraResponse;
import com.finanzas.service.BilleteraService;
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
@RequestMapping("/api/billeteras")
public class BilleteraController {

    private final BilleteraService billeteraService;

    public BilleteraController(BilleteraService billeteraService) {
        this.billeteraService = billeteraService;
    }

    @GetMapping
    public ResponseEntity<List<BilleteraResponse>> listarDelUsuario(Authentication autenticacion) {
        return ResponseEntity.ok(billeteraService.listarDeUsuario(autenticacion.getName()));
    }

    @PostMapping
    public ResponseEntity<BilleteraResponse> crear(Authentication autenticacion,
                                                   @Valid @RequestBody BilleteraRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(billeteraService.crear(autenticacion.getName(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> desactivar(Authentication autenticacion, @PathVariable Long id) {
        billeteraService.desactivar(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
