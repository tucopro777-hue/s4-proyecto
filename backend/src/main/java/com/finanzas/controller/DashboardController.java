package com.finanzas.controller;

import com.finanzas.dto.DashboardResponse;
import com.finanzas.dto.GastoCategoriaResponse;
import com.finanzas.dto.ProyeccionFinancieraResponse;
import com.finanzas.service.DashboardService;
import com.finanzas.service.ProyeccionFinancieraService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;
    private final ProyeccionFinancieraService proyeccionFinancieraService;

    public DashboardController(DashboardService dashboardService,
                               ProyeccionFinancieraService proyeccionFinancieraService) {
        this.dashboardService = dashboardService;
        this.proyeccionFinancieraService = proyeccionFinancieraService;
    }

    /** GET /api/dashboard/proyeccion: probabilidades del próximo mes según la cadena de Markov. */
    @GetMapping("/proyeccion")
    public ResponseEntity<ProyeccionFinancieraResponse> proyeccion(Authentication autenticacion) {
        return ResponseEntity.ok(proyeccionFinancieraService.proyectar(autenticacion.getName()));
    }

    /** GET /api/dashboard/resumen: indicadores del mes para el usuario autenticado. */
    @GetMapping("/resumen")
    public ResponseEntity<DashboardResponse> resumen(Authentication autenticacion, @RequestParam(defaultValue = "BOB") String divisa) {
        return ResponseEntity.ok(dashboardService.obtenerResumen(autenticacion.getName(), divisa));
    }

    /** GET /api/dashboard/gastos-por-categoria: gasto del mes agrupado por categoría. */
    @GetMapping("/gastos-por-categoria")
    public ResponseEntity<List<GastoCategoriaResponse>> gastosPorCategoria(Authentication autenticacion) {
        return ResponseEntity.ok(dashboardService.gastosPorCategoria(autenticacion.getName()));
    }
}
