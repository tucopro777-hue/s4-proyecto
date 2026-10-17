package com.finanzas.controller; // Controladores REST

import com.finanzas.dto.AuthResponse; // Respuesta con token
import com.finanzas.dto.LoginRequest; // Datos de login
import com.finanzas.dto.RegistroRequest; // Datos de registro
import com.finanzas.service.AuthService; // Lógica de autenticación
import jakarta.validation.Valid; // Activa validaciones del DTO
import org.springframework.http.HttpStatus; // Códigos HTTP
import org.springframework.http.ResponseEntity; // Respuesta HTTP completa
import org.springframework.web.bind.annotation.PostMapping; // Mapea peticiones POST
import org.springframework.web.bind.annotation.RequestBody; // Lee el cuerpo JSON
import org.springframework.web.bind.annotation.RequestMapping; // Prefijo de rutas
import org.springframework.web.bind.annotation.RestController; // Controlador que devuelve JSON

@RestController // Controlador REST
@RequestMapping("/api/auth") // Todas las rutas empiezan por /api/auth
public class AuthController { // Endpoints de autenticación

    private final AuthService authService; // Servicio inyectado

    public AuthController(AuthService authService) { // Constructor con inyección
        this.authService = authService; // Guarda el servicio
    }

    @PostMapping("/registro") // POST /api/auth/registro
    public ResponseEntity<AuthResponse> registro(@Valid @RequestBody RegistroRequest request) { // Recibe y valida datos
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registrar(request)); // Responde 201 con el token
    }

    @PostMapping("/login") // POST /api/auth/login
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) { // Recibe y valida credenciales
        return ResponseEntity.ok(authService.login(request)); // Responde 200 con el token
    }
}
