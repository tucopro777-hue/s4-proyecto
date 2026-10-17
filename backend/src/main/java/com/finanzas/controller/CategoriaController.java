package com.finanzas.controller; // Controladores REST

import com.finanzas.dto.CategoriaRequest; // Datos para crear categoría
import com.finanzas.dto.CategoriaResponse; // Datos de categoría devueltos
import com.finanzas.model.TipoCategoria; // Enum de tipo de categoría
import com.finanzas.service.CategoriaService; // Lógica de categorías
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
import org.springframework.web.bind.annotation.RequestParam; // Lee un parámetro de la URL
import org.springframework.web.bind.annotation.RestController; // Controlador que devuelve JSON

import java.util.List; // Lista de respuestas

@RestController // Controlador REST
@RequestMapping("/api/categorias") // Rutas bajo /api/categorias
public class CategoriaController { // Endpoints de categorías

    private final CategoriaService categoriaService; // Servicio inyectado

    public CategoriaController(CategoriaService categoriaService) { // Constructor con inyección
        this.categoriaService = categoriaService; // Guarda el servicio
    }

    @GetMapping // GET /api/categorias
    public ResponseEntity<List<CategoriaResponse>> listarDelUsuario( // Lista categorías del usuario
            Authentication autenticacion, // Usuario actual
            @RequestParam(required = false) TipoCategoria tipo) { // Filtro opcional por tipo
        return ResponseEntity.ok(categoriaService.listarDeUsuario(autenticacion.getName(), tipo)); // Devuelve la lista (200)
    }

    @PostMapping // POST /api/categorias
    public ResponseEntity<CategoriaResponse> crear(Authentication autenticacion, // Usuario actual
                                                   @Valid @RequestBody CategoriaRequest request) { // Datos validados
        return ResponseEntity.status(HttpStatus.CREATED) // Responde 201
                .body(categoriaService.crear(autenticacion.getName(), request)); // Crea la categoría del usuario
    }

    @DeleteMapping("/{id}") // DELETE /api/categorias/{id}
    public ResponseEntity<Void> desactivar(Authentication autenticacion, @PathVariable Long id) { // Id desde la URL
        categoriaService.desactivar(autenticacion.getName(), id); // Desactiva la categoría del usuario
        return ResponseEntity.noContent().build(); // Responde 204 sin cuerpo
    }
}
