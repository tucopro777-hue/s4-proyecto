package com.finanzas.service;

import com.finanzas.dto.*;
import com.finanzas.model.Usuario;
import com.finanzas.model.Billetera;
import com.finanzas.repository.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class AnalisisFinancieroService {
    private final UsuarioRepository usuarios;
    private final TransaccionRepository transacciones;
    private final BilleteraRepository billeteras;
    private final MarkovService markov;
    private final VmeService vme;

    public AnalisisFinancieroService(UsuarioRepository usuarios, TransaccionRepository transacciones,
            BilleteraRepository billeteras, MarkovService markov, VmeService vme) {
        this.usuarios = usuarios; this.transacciones = transacciones; this.billeteras = billeteras;
        this.markov = markov; this.vme = vme;
    }

    public List<String> divisas(String email) {
        Long id = usuario(email).getId();
        // Incluye billeteras inactivas porque su historial sigue siendo válido.
        return billeteras.findByUsuarioIdOrderByNombreAsc(id).stream().map(Billetera::getDivisa).distinct().sorted().toList();
    }

    public MarkovResponse predecir(String email, String divisa) {
        return predecir(usuario(email), divisa);
    }

    public VmeResponse evaluar(String email, VmeRequest request) {
        Usuario usuario = usuario(email);
        BigDecimal disponible = disponible(usuario, request.divisa());
        return vme.calcular(disponible, predecir(usuario, request.divisa()), request);
    }

    /** Ejemplo didáctico: coeficientes ficticios, nunca tasas de productos financieros. */
    public VmeResponse evaluarAutomaticamente(String email, EvaluacionAutomaticaRequest request) {
        Usuario usuario = usuario(email);
        BigDecimal saldo = disponible(usuario, request.divisa());
        BigDecimal monto = request.monto();
        if (monto.compareTo(saldo) > 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El monto no puede superar el saldo disponible");
        }
        // Se evalúa únicamente el monto solicitado, no todo el patrimonio del usuario.
        var resultado = vme.calcular(monto, predecir(usuario, request.divisa()),
                new VmeRequest(request.divisa(), BigDecimal.ZERO, List.of(
                        new VmeRequest.Alternativa("Guardar mi dinero", BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO),
                        new VmeRequest.Alternativa("Ahorrar", monto, monto.multiply(new BigDecimal("0.04")), monto.multiply(new BigDecimal("-0.02"))),
                        new VmeRequest.Alternativa("Invertir", monto, monto.multiply(new BigDecimal("0.20")), monto.multiply(new BigDecimal("-0.24"))))));
        return new VmeResponse(resultado.divisa(), resultado.disponible(), resultado.reservaMinima(),
                resultado.markov(), resultado.alternativas(), resultado.recomendadas(),
                "SIMULACIÓN: FINDEX no tiene tasas ni rendimientos reales para comparar estas opciones. "
                + "Los resultados monetarios son ficticios; las probabilidades proceden de tu historial. "
                + "El resultado esperado es el monto final, no la ganancia. No mueve dinero.");
    }

    public BigDecimal disponible(String email, String divisa) {
        return disponible(usuario(email), divisa);
    }

    private BigDecimal disponible(Usuario usuario, String divisa) {
        return billeteras.findByUsuarioIdAndActivaTrueOrderByNombreAsc(usuario.getId()).stream()
                .filter(b -> divisa.equals(b.getDivisa())).map(Billetera::getSaldoActual)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private MarkovResponse predecir(Usuario usuario, String divisa) {
        return markov.calcular(transacciones.findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(usuario.getId()),
                divisa, LocalDate.now());
    }

    private Usuario usuario(String email) {
        return usuarios.findByEmail(email).orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuario no encontrado"));
    }
}
