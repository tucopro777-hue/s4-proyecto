package com.finanzas.controller;

import com.finanzas.dto.*;
import com.finanzas.service.AnalisisFinancieroService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;
import java.util.List;

@RestController
@Validated
@RequestMapping("/api/dashboard/analisis")
public class AnalisisFinancieroController {
    private final AnalisisFinancieroService service;
    public AnalisisFinancieroController(AnalisisFinancieroService service) { this.service = service; }

    @GetMapping("/divisas")
    public List<String> divisas(Authentication auth) { return service.divisas(auth.getName()); }

    @GetMapping("/markov")
    public MarkovResponse markov(Authentication auth, @RequestParam @Pattern(regexp = "[A-Z]{3}") String divisa) {
        return service.predecir(auth.getName(), divisa);
    }

    @PostMapping("/vme")
    public VmeResponse vme(Authentication auth, @Valid @RequestBody VmeRequest request) {
        return service.evaluar(auth.getName(), request);
    }

    @PostMapping("/vme/automatico")
    public VmeResponse automatico(Authentication auth, @Valid @RequestBody EvaluacionAutomaticaRequest request) {
        return service.evaluarAutomaticamente(auth.getName(), request);
    }

    @GetMapping("/disponible")
    public java.math.BigDecimal disponible(Authentication auth, @RequestParam @Pattern(regexp = "[A-Z]{3}") String divisa) {
        return service.disponible(auth.getName(), divisa);
    }

    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    @ResponseStatus(org.springframework.http.HttpStatus.BAD_REQUEST)
    public java.util.Map<String, String> parametrosInvalidos() {
        return java.util.Map.of("message", "La divisa debe ser un código de tres letras mayúsculas");
    }
}
