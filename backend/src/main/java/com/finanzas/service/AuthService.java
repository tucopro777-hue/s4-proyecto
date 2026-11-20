package com.finanzas.service;

import com.finanzas.dto.AuthResponse;
import com.finanzas.dto.LoginRequest;
import com.finanzas.dto.RegistroRequest;
import com.finanzas.model.Usuario;
import com.finanzas.repository.UsuarioRepository;
import com.finanzas.security.JwtService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;

@Service
public class AuthService {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    private final int maxIntentosLogin;
    private final int minutosBloqueo;

    public AuthService(UsuarioRepository usuarioRepository,
                       PasswordEncoder passwordEncoder,
                       JwtService jwtService,
                       @Value("${app.seguridad.max-intentos-login:3}") int maxIntentosLogin,
                       @Value("${app.seguridad.minutos-bloqueo:15}") int minutosBloqueo) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.maxIntentosLogin = maxIntentosLogin;
        this.minutosBloqueo = minutosBloqueo;
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

    /*
     * noRollbackFor: los fallos se cuentan en la base de datos aunque la respuesta sea un error;
     * sin esto, la excepción haría rollback y el contador nunca subiría.
     */
    @Transactional(noRollbackFor = {CuentaBloqueadaException.class, ResponseStatusException.class})
    public AuthResponse login(LoginRequest request) {
        LocalDateTime ahora = LocalDateTime.now();
        Optional<Usuario> encontrado = usuarioRepository.findByEmail(normalizarEmail(request.email()));
        if (encontrado.isEmpty()) {
            throw credencialesInvalidas();
        }
        Usuario usuario = encontrado.get();

        // Con la cuenta bloqueada no se comprueba la contraseña
        if (usuario.estaBloqueada(ahora)) {
            throw bloqueada(usuario, ahora);
        }

        if (!passwordEncoder.matches(request.password(), usuario.getPassword())) {
            usuario.registrarFalloLogin(ahora, maxIntentosLogin, minutosBloqueo);
            usuarioRepository.save(usuario);
            if (usuario.estaBloqueada(ahora)) {
                throw bloqueada(usuario, ahora);
            }
            throw credencialesInvalidas();
        }

        usuario.limpiarBloqueo();
        return crearRespuesta(usuario);
    }

    private String normalizarEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private AuthResponse crearRespuesta(Usuario usuario) {
        String token = jwtService.generarToken(usuario.getEmail());
        return new AuthResponse(token, "Bearer", usuario.getEmail(), usuario.getNombre());
    }

    private CuentaBloqueadaException bloqueada(Usuario usuario, LocalDateTime ahora) {
        long segundos = Duration.between(ahora, usuario.getBloqueadoHasta()).getSeconds();
        return new CuentaBloqueadaException(Math.max(segundos, 1));
    }

    // Mismo mensaje para email inexistente y contraseña incorrecta, para no revelar qué emails existen
    private ResponseStatusException credencialesInvalidas() {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Credenciales inválidas");
    }

}
