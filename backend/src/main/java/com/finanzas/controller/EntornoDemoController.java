package com.finanzas.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.*;

/** Comprobación de solo lectura. Esta ruta no existe fuera del perfil demo-local. */
@RestController
@Profile("demo-local")
@RequestMapping("/api/demo")
public class EntornoDemoController {
    private final DataSource dataSource;
    private final Environment environment;
    public EntornoDemoController(DataSource dataSource, Environment environment) {
        this.dataSource = dataSource; this.environment = environment;
    }
    @GetMapping("/entorno")
    public Map<String, Object> entorno(Authentication auth) throws SQLException {
        if (!"demo@finanzas.test".equals(auth.getName())) throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        Set<String> perfiles = Set.of(environment.getActiveProfiles());
        try (var conexion = dataSource.getConnection()) {
            String url = conexion.getMetaData().getURL();
            boolean local = url.matches("jdbc:postgresql://(localhost|127\\.0\\.0\\.1|\\[::1\\]):5432/finanzas(?:\\?.*)?");
            if (!local || perfiles.stream().anyMatch(p -> p.toLowerCase(Locale.ROOT).contains("prod"))
                    || !environment.getProperty("app.demo.enabled", Boolean.class, false)) {
                throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Carga demo no permitida en este entorno");
            }
            return Map.of("entorno", "demostracion-local", "base", conexion.getCatalog(),
                    "servidor", "loopback:5432", "cargaPermitida", true);
        }
    }
}
