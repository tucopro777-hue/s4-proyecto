package com.finanzas.controller; // Controladores REST

import com.finanzas.dto.BilleteraRequest; // Datos para crear billetera
import com.finanzas.dto.BilleteraResponse; // Datos de billetera devueltos
import com.finanzas.dto.ConfirmarPasswordRequest; // Contraseña para confirmar la desactivación
import com.finanzas.service.BilleteraService; // Lógica de billeteras
import jakarta.validation.Valid; // Activa validaciones del DTO
import org.springframework.http.HttpStatus; // Códigos HTTP
import org.springframework.http.ResponseEntity; // Respuesta HTTP completa
import org.springframework.security.core.Authentication; // Usuario autenticado
import org.springframework.web.bind.annotation.DeleteMapping; // Mapea peticiones DELETE
import org.springframework.web.bind.annotation.GetMapping; // Mapea peticiones GET
import org.springframework.web.bind.annotation.PathVariable; // Lee un valor de la URL
import org.springframework.web.bind.annotation.PostMapping; // Mapea peticiones POST
import org.springframework.web.bind.annotation.RequestBody; // Lee el cuerpo JSON
import org.springframework.web.bind.annotation.RequestMapping; // Prefijo de rutas
import org.springframework.web.bind.annotation.RestController; // Controlador que devuelve JSON

import java.util.List; // Lista de respuestas

@RestController // Controlador REST
@RequestMapping("/api/billeteras") // Rutas bajo /api/billeteras
public class BilleteraController { // Endpoints de billeteras

    private final BilleteraService billeteraService; // Servicio inyectado

    public BilleteraController(BilleteraService billeteraService) { // Constructor con inyección
        this.billeteraService = billeteraService; // Guarda el servicio
    }

    @GetMapping // GET /api/billeteras
    public ResponseEntity<List<BilleteraResponse>> listarDelUsuario(Authentication autenticacion) { // Usuario actual
        return ResponseEntity.ok(billeteraService.listarDeUsuario(autenticacion.getName())); // Devuelve sus billeteras (200)
    }

    @PostMapping // POST /api/billeteras
    public ResponseEntity<BilleteraResponse> crear(Authentication autenticacion, // Usuario actual
                                                   @Valid @RequestBody BilleteraRequest request) { // Datos validados
        return ResponseEntity.status(HttpStatus.CREATED) // Responde 201
                .body(billeteraService.crear(autenticacion.getName(), request)); // Crea la billetera del usuario
    }

    @DeleteMapping("/{id}") // DELETE /api/billeteras/{id} con la contraseña en el cuerpo
    public ResponseEntity<Void> desactivar(Authentication autenticacion, // Usuario actual
                                           @PathVariable Long id, // Id desde la URL
                                           @Valid @RequestBody ConfirmarPasswordRequest request) { // Contraseña validada
        billeteraService.desactivar(autenticacion.getName(), id, request.password()); // Desactiva si la contraseña es correcta
        return ResponseEntity.noContent().build(); // Responde 204 sin cuerpo
    }
}
