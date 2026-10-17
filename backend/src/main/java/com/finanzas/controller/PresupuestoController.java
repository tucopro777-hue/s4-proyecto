package com.finanzas.controller;

import com.finanzas.dto.ActualizarPresupuestoRequest;
import com.finanzas.dto.PresupuestoRequest;
import com.finanzas.dto.PresupuestoResponse;
import com.finanzas.service.PresupuestoService;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/presupuestos")
public class PresupuestoController {

    private final PresupuestoService presupuestoService;

    public PresupuestoController(PresupuestoService presupuestoService) {
        this.presupuestoService = presupuestoService;
    }

    @GetMapping
    public ResponseEntity<List<PresupuestoResponse>> listarDelPeriodo(Authentication autenticacion,
                                                                      @RequestParam int anio,
                                                                      @RequestParam int mes) {
        return ResponseEntity.ok(presupuestoService.listarDeUsuario(autenticacion.getName(), anio, mes));
    }

    @PostMapping
    public ResponseEntity<PresupuestoResponse> crear(Authentication autenticacion,
                                                     @Valid @RequestBody PresupuestoRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(presupuestoService.crear(autenticacion.getName(), request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<PresupuestoResponse> actualizar(Authentication autenticacion,
                                                          @PathVariable Long id,
                                                          @Valid @RequestBody ActualizarPresupuestoRequest request) {
        return ResponseEntity.ok(presupuestoService.actualizar(autenticacion.getName(), id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(Authentication autenticacion, @PathVariable Long id) {
        presupuestoService.eliminar(autenticacion.getName(), id);
        return ResponseEntity.noContent().build();
    }
}
