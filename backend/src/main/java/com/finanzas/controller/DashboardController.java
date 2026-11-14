package com.finanzas.controller;

import com.finanzas.dto.DashboardResponse;
import com.finanzas.dto.GastoCategoriaResponse;
import com.finanzas.service.DashboardService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    /** GET /api/dashboard/resumen: indicadores del mes para el usuario autenticado. */
    @GetMapping("/resumen")
    public ResponseEntity<DashboardResponse> resumen(Authentication autenticacion) {
        return ResponseEntity.ok(dashboardService.obtenerResumen(autenticacion.getName()));
    }

    /** GET /api/dashboard/gastos-por-categoria: gasto del mes agrupado por categoría. */
    @GetMapping("/gastos-por-categoria")
    public ResponseEntity<List<GastoCategoriaResponse>> gastosPorCategoria(Authentication autenticacion) {
        return ResponseEntity.ok(dashboardService.gastosPorCategoria(autenticacion.getName()));
    }
}
