package com.finanzas.service;

import com.finanzas.dto.AuthResponse;
import com.finanzas.dto.LoginRequest;
import com.finanzas.dto.RegistroRequest;
import com.finanzas.model.Usuario;
import com.finanzas.repository.UsuarioRepository;
import com.finanzas.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse registrar(RegistroRequest request) {
        String email = normalizarEmail(request.email());

        if (usuarioRepository.existsByEmail(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "El email ya está registrado");
        }

        Usuario usuario = new Usuario(
                request.nombre().trim(),
                email,
                passwordEncoder.encode(request.password())
        );
        usuarioRepository.save(usuario);

        return crearRespuesta(usuario);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        Usuario usuario = usuarioRepository.findByEmail(normalizarEmail(request.email()))
                .orElseThrow(this::credencialesInvalidas);

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            throw credencialesInvalidas();
        }

        return crearRespuesta(usuario);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse crearRespuesta(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getEmail());
        return new AuthResponse(token, "Bearer", usuario.getEmail(), usuario.getNombre());
    }

    // Mismo mensaje para email inexistente y contraseña incorrecta, para no revelar qué emails existen
    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }
}
