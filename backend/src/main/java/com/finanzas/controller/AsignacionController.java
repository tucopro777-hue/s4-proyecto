package com.finanzas.controller;

import com.finanzas.dto.AsignacionRequest;
import com.finanzas.dto.AsignacionResponse;
import com.finanzas.service.PresupuestoOptimizerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/asignacion")
public class AsignacionController {

    private final PresupuestoOptimizerService presupuestoOptimizerService;

    public AsignacionController(PresupuestoOptimizerService presupuestoOptimizerService) {
        this.presupuestoOptimizerService = presupuestoOptimizerService;
    }

    /** POST /api/asignacion/inteligente: metas recomendadas para el dinero disponible indicado. */
    @PostMapping("/inteligente")
    public ResponseEntity<AsignacionResponse> inteligente(Authentication autenticacion,
                                                          @Valid @RequestBody AsignacionRequest request) {
        return ResponseEntity.ok(presupuestoOptimizerService.recomendar(
                autenticacion.getName(), request.presupuestoDisponible()));
    }
}
