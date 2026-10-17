package com.finanzas.controller; // Controladores REST

import com.finanzas.dto.AporteRequest; // Datos de un aporte
import com.finanzas.dto.MetaAhorroRequest; // Datos de la meta
import com.finanzas.dto.MetaAhorroResponse; // Datos de la meta devueltos
import com.finanzas.service.MetaAhorroService; // Lógica de metas
import jakarta.validation.Valid; // Activa validaciones del DTO
import org.springframework.http.HttpStatus; // Códigos HTTP
import org.springframework.http.ResponseEntity; // Respuesta HTTP completa
import org.springframework.security.core.Authentication; // Usuario autenticado
import org.springframework.web.bind.annotation.DeleteMapping; // Mapea peticiones DELETE
import org.springframework.web.bind.annotation.GetMapping; // Mapea peticiones GET
import org.springframework.web.bind.annotation.PathVariable; // Lee un valor de la URL
import org.springframework.web.bind.annotation.PostMapping; // Mapea peticiones POST
import org.springframework.web.bind.annotation.PutMapping; // Mapea peticiones PUT
import org.springframework.web.bind.annotation.RequestBody; // Lee el cuerpo JSON
import org.springframework.web.bind.annotation.RequestMapping; // Prefijo de rutas
import org.springframework.web.bind.annotation.RestController; // Controlador que devuelve JSON

import java.util.List; // Lista de respuestas

@RestController // Controlador REST
@RequestMapping("/api/metas") // Rutas bajo /api/metas
public class MetaAhorroController { // Endpoints de metas de ahorro

    private final MetaAhorroService metaAhorroService; // Servicio inyectado

    public MetaAhorroController(MetaAhorroService metaAhorroService) { // Constructor con inyección
        this.metaAhorroService = metaAhorroService; // Guarda el servicio
    }

    @GetMapping // GET /api/metas
    public ResponseEntity<List<MetaAhorroResponse>> listarDelUsuario(Authentication autenticacion) { // Usuario actual
        return ResponseEntity.ok(metaAhorroService.listarDeUsuario(autenticacion.getName())); // Devuelve sus metas (200)
    }

    @PostMapping // POST /api/metas
    public ResponseEntity<MetaAhorroResponse> crear(Authentication autenticacion, // Usuario actual
                                                    @Valid @RequestBody MetaAhorroRequest request) { // Datos validados
        return ResponseEntity.status(HttpStatus.CREATED) // Responde 201
                .body(metaAhorroService.crear(autenticacion.getName(), request)); // Crea la meta del usuario
    }

    @PutMapping("/{id}") // PUT /api/metas/{id}
    public ResponseEntity<MetaAhorroResponse> actualizar(Authentication autenticacion, // Usuario actual
                                                         @PathVariable Long id, // Id de la meta
                                                         @Valid @RequestBody MetaAhorroRequest request) { // Datos validados
        return ResponseEntity.ok(metaAhorroService.actualizar(autenticacion.getName(), id, request)); // Actualiza la meta (200)
    }

    // Único camino que suma dinero a la meta: un aporte explícito
    @PostMapping("/{id}/aportes") // POST /api/metas/{id}/aportes
    public ResponseEntity<MetaAhorroResponse> aportar(Authentication autenticacion, // Usuario actual
                                                      @PathVariable Long id, // Id de la meta
                                                      @Valid @RequestBody AporteRequest request) { // Monto validado
        return ResponseEntity.ok(metaAhorroService.aportar(autenticacion.getName(), id, request)); // Suma el aporte (200)
    }

    @DeleteMapping("/{id}") // DELETE /api/metas/{id}
    public ResponseEntity<Void> eliminar(Authentication autenticacion, @PathVariable Long id) { // Id desde la URL
        metaAhorroService.eliminar(autenticacion.getName(), id); // Elimina la meta del usuario
        return ResponseEntity.noContent().build(); // Responde 204 sin cuerpo
    }
}
