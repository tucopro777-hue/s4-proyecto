package com.finanzas.config; // Configuración de seguridad

import com.finanzas.security.JwtAuthenticationFilter; // Filtro que valida el token JWT
import jakarta.servlet.http.HttpServletResponse; // Respuesta HTTP del servidor
import org.springframework.context.annotation.Bean; // Marca métodos que crean beans
import org.springframework.context.annotation.Configuration; // Marca clase de configuración
import org.springframework.http.HttpMethod; // Métodos HTTP (GET, POST...)
import org.springframework.security.config.Customizer; // Configuración por defecto
import org.springframework.security.config.annotation.web.builders.HttpSecurity; // Constructor de reglas de seguridad
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity; // Activa seguridad web
import org.springframework.security.config.http.SessionCreationPolicy; // Política de sesiones
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder; // Codificador BCrypt
import org.springframework.security.crypto.password.PasswordEncoder; // Interfaz de codificador
import org.springframework.security.web.SecurityFilterChain; // Cadena de filtros de seguridad
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Filtro de login
import org.springframework.web.cors.CorsConfiguration; // Configuración CORS
import org.springframework.web.cors.CorsConfigurationSource; // Fuente de configuración CORS
import org.springframework.web.cors.UrlBasedCorsConfigurationSource; // CORS por rutas URL

import java.util.List; // Lista inmutable

@Configuration // Clase de configuración de Spring
@EnableWebSecurity // Activa la seguridad web
public class SecurityConfig { // Define reglas de acceso

    @Bean // Registra el bean en el contexto
    public SecurityFilterChain securityFilterChain(HttpSecurity http, // Constructor de reglas
                                                   JwtAuthenticationFilter jwtAuthenticationFilter) throws Exception { // Filtro JWT
        http // Inicia la configuración
                .csrf(csrf -> csrf.disable()) // Desactiva CSRF (API sin cookies)
                .cors(Customizer.withDefaults()) // Activa CORS con la configuración del bean
                .sessionManagement(sesion -> sesion.sessionCreationPolicy(SessionCreationPolicy.STATELESS)) // Sin sesiones en servidor
                .authorizeHttpRequests(auth -> auth // Reglas de autorización por ruta
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll() // Permite peticiones preflight CORS

                        .requestMatchers("/api/auth/**").permitAll() // Login y registro son públicos

                        .requestMatchers("/error").permitAll() // Permite ver errores sin autenticar
                        .anyRequest().authenticated()) // El resto requiere estar autenticado
                .exceptionHandling(ex -> ex.authenticationEntryPoint( // Manejo de errores de autenticación
                        (request, response, authException) -> // Respuesta cuando no hay autenticación
                                response.sendError(HttpServletResponse.SC_UNAUTHORIZED))) // Responde 401
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class); // Ejecuta el filtro JWT antes del login

        return http.build(); // Construye la cadena de filtros
    }

    @Bean // Registra el codificador de contraseñas
    public PasswordEncoder passwordEncoder() { // Crea el codificador
        return new BCryptPasswordEncoder(); // Hash BCrypt para contraseñas
    }

    @Bean // Registra la configuración CORS
    public CorsConfigurationSource corsConfigurationSource() { // Crea la fuente CORS
        CorsConfiguration configuracion = new CorsConfiguration(); // Configuración CORS vacía
        configuracion.setAllowedOrigins(List.of("http://localhost:4200")); // Solo permite el frontend local
        configuracion.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")); // Métodos permitidos
        configuracion.setAllowedHeaders(List.of("Authorization", "Content-Type")); // Cabeceras permitidas

        UrlBasedCorsConfigurationSource origen = new UrlBasedCorsConfigurationSource(); // Fuente por URL
        origen.registerCorsConfiguration("/**", configuracion); // Aplica CORS a todas las rutas
        return origen; // Devuelve la fuente configurada
    }
}
