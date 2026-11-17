package com.finanzas.service;

import com.finanzas.model.*;
import com.finanzas.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/** Integración HTTP + seguridad + JPA sobre H2; cada prueba revierte sus datos. */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AnalisisFinancieroIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UsuarioRepository usuarios;
    @Autowired BilleteraRepository billeteras;
    @Autowired TransaccionRepository transacciones;
    @Autowired CategoriaRepository categorias;
    @Autowired AnalisisFinancieroService analisis;

    private Usuario crear(String email) { return usuarios.save(new Usuario("Prueba", email, "hash-prueba")); }
    private Billetera caja(Usuario u, String nombre, String divisa, int saldo) {
        return billeteras.save(new Billetera(nombre, BigDecimal.valueOf(saldo), divisa, u));
    }
    private void ingreso(Usuario u, Billetera b, LocalDate fecha, int monto) {
        var c = categorias.findByUsuarioIdAndActivaTrueOrderByNombreAsc(u.getId()).stream().findFirst()
                .orElseGet(() -> categorias.save(new Categoria("Ingreso", TipoCategoria.INGRESO, "#000000", "cash", u)));
        transacciones.save(new Transaccion(BigDecimal.valueOf(monto), TipoTransaccion.INGRESO, "", fecha, u, b, c));
    }

    @Test void consultaSoloUsuarioYDivisaConservandoHistorialDeInactivas() throws Exception {
        var ana = crear("analisis.ana@test.test"); var otro = crear("analisis.otro@test.test");
        var bob = caja(ana, "BOB", "BOB", 1000); var usd = caja(ana, "USD", "USD", 9000);
        var inactiva = caja(ana, "Antigua", "BOB", 500); inactiva.desactivar();
        var ajena = caja(otro, "Ajena", "BOB", 100000);
        var mes = YearMonth.now();
        ingreso(ana, inactiva, mes.minusMonths(2).atDay(1), 100);
        ingreso(ana, bob, mes.minusMonths(1).atDay(1), 100);
        ingreso(ana, bob, LocalDate.now(), 100);
        ingreso(ana, usd, LocalDate.now(), 9000);
        ingreso(otro, ajena, mes.minusMonths(1).atDay(1), 100000);
        assertThat(analisis.disponible(ana.getEmail(), "BOB")).isEqualByComparingTo("1000");
        mvc.perform(get("/api/dashboard/analisis/markov").param("divisa", "BOB").with(user(ana.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.historial.length()").value(2))
                .andExpect(jsonPath("$.estadoActual.ingresos").value(100))
                .andExpect(jsonPath("$.matriz[0].total").value(1))
                .andExpect(jsonPath("$.probabilidades.estable").value(1));
        mvc.perform(get("/api/dashboard/analisis/disponible").param("divisa", "BOB").with(user(ana.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$").value(1000));
        mvc.perform(get("/api/dashboard/analisis/divisas").with(user(otro.getEmail())))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1)).andExpect(jsonPath("$[0]").value("BOB"));
    }

    @Test void vmeSeCalculaEnServidorSinModificarSaldoNiAceptarProbabilidadesDelCliente() throws Exception {
        var u = crear("analisis.vme@test.test"); var b = caja(u, "Caja", "BOB", 1000);
        var mes = YearMonth.now();
        ingreso(u, b, mes.minusMonths(2).atDay(1), 100); ingreso(u, b, mes.minusMonths(1).atDay(1), 100);
        ingreso(u, b, LocalDate.now(), 100);
        mvc.perform(post("/api/dashboard/analisis/vme").with(user(u.getEmail())).contentType("application/json")
                .content("""
                  {"divisa":"BOB","reservaMinima":100,"alternativas":[
                    {"nombre":"Mantener","asignacion":0,"resultadoEstable":0,"resultadoDeficit":0},
                    {"nombre":"Ahorro","asignacion":400,"resultadoEstable":20,"resultadoDeficit":-10}]}
                  """))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(1000))
                .andExpect(jsonPath("$.alternativas[1].vme").value(1020))
                .andExpect(jsonPath("$.recomendadas[0]").value("Ahorro"));
        assertThat(billeteras.findById(b.getId()).orElseThrow().getSaldoActual()).isEqualByComparingTo("1000");
        assertThat(transacciones.findByUsuarioIdAndAnuladaFalseOrderByFechaDescIdDesc(u.getId())).hasSize(3);
    }

    @Test void evaluacionAutomaticaUsaMontoYMarkovSinModificarUsuarios() throws Exception {
        var u = crear("automatico@test.test"); var b = caja(u, "Caja", "BOB", 1000);
        var otro = crear("aislado@test.test"); var ajena = caja(otro, "Ajena", "BOB", 9999);
        var mes = YearMonth.now();
        ingreso(u, b, mes.minusMonths(2).atDay(1), 100);
        ingreso(u, b, mes.minusMonths(1).atDay(1), 100);
        ingreso(u, b, LocalDate.now(), 100);
        mvc.perform(post("/api/dashboard/analisis/vme/automatico").with(user(u.getEmail()))
                .contentType("application/json").content("{\"divisa\":\"BOB\",\"monto\":500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.disponible").value(500))
                .andExpect(jsonPath("$.markov.probabilidades.estable").value(1))
                .andExpect(jsonPath("$.alternativas[2].vme").value(600))
                .andExpect(jsonPath("$.recomendadas[0]").value("Invertir"))
                .andExpect(jsonPath("$.explicacion").value(org.hamcrest.Matchers.containsString("SIMULACIÓN")));
        assertThat(b.getSaldoActual()).isEqualByComparingTo("1000");
        assertThat(ajena.getSaldoActual()).isEqualByComparingTo("9999");
        for (String monto : new String[]{"0", "-1", "1001", "0.001", "null"}) {
            mvc.perform(post("/api/dashboard/analisis/vme/automatico").with(user(u.getEmail()))
                    .contentType("application/json").content("{\"divisa\":\"BOB\",\"monto\":" + monto + "}"))
                    .andExpect(status().isBadRequest());
        }
        mvc.perform(post("/api/dashboard/analisis/vme/automatico").with(user(otro.getEmail()))
                .contentType("application/json").content("{\"divisa\":\"BOB\",\"monto\":500}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.recomendadas").isEmpty())
                .andExpect(jsonPath("$.alternativas[0].vme").isEmpty());
    }

    @Test void endpointsExigenAutenticacion() throws Exception {
        mvc.perform(post("/api/dashboard/analisis/vme/automatico").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/dashboard/analisis/markov").param("divisa", "BOB")).andExpect(status().isUnauthorized());
        mvc.perform(post("/api/dashboard/analisis/vme").contentType("application/json").content("{}"))
                .andExpect(status().isUnauthorized());
    }

    @Test void validaParametrosAnidadosYCamposObligatorios() throws Exception {
        var u = crear("analisis.validacion@test.test");
        mvc.perform(get("/api/dashboard/analisis/markov").param("divisa", "incorrecta").with(user(u.getEmail())))
                .andExpect(status().isBadRequest());
        for (String cuerpo : new String[]{"{}", "{\"divisa\":\"BOB\",\"reservaMinima\":-1,\"alternativas\":[]}",
                "{\"divisa\":\"BOB\",\"reservaMinima\":0,\"alternativas\":[null]}",
                "{\"divisa\":\"BOB\",\"reservaMinima\":0,\"alternativas\":[{\"nombre\":\"A\",\"asignacion\":-1,\"resultadoEstable\":0,\"resultadoDeficit\":0}]}",
                "{\"divisa\":\"BOB\",\"reservaMinima\":0,\"alternativas\":[{\"nombre\":\"A\",\"asignacion\":0,\"resultadoEstable\":0.001,\"resultadoDeficit\":0}]}"}) {
            mvc.perform(post("/api/dashboard/analisis/vme").with(user(u.getEmail())).contentType("application/json").content(cuerpo))
                    .andExpect(status().isBadRequest());
        }
    }
}
