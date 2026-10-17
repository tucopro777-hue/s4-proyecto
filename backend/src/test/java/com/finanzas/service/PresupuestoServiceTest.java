package com.finanzas.service;

import com.finanzas.dto.ActualizarPresupuestoRequest;
import com.finanzas.dto.PresupuestoRequest;
import com.finanzas.dto.PresupuestoResponse;
import com.finanzas.model.Billetera;
import com.finanzas.model.Categoria;
import com.finanzas.model.TipoCategoria;
import com.finanzas.model.TipoTransaccion;
import com.finanzas.model.Usuario;
import com.finanzas.dto.TransaccionRequest;
import com.finanzas.repository.BilleteraRepository;
import com.finanzas.repository.CategoriaRepository;
import com.finanzas.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/** Cada prueba se revierte al final (@Transactional). */
@SpringBootTest
@Transactional
class PresupuestoServiceTest {

    private static final String EMAIL = "presupuesto.prueba@finanzas.test";
    private static final LocalDate FECHA = LocalDate.of(2026, 10, 15);

    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private BilleteraRepository billeteraRepository;
    @Autowired
    private CategoriaRepository categoriaRepository;
    @Autowired
    private PresupuestoService presupuestoService;
    @Autowired
    private TransaccionService transaccionService;

    private Categoria comida;
    private Billetera billetera;

    private void prepararDatos() {
        Usuario usuario = usuarioRepository.save(new Usuario("Ana", EMAIL, "hash"));
        billetera = billeteraRepository.save(new Billetera("Caja", new BigDecimal("1000.00"), "BOB", usuario));
        comida = categoriaRepository.save(new Categoria("Comida", TipoCategoria.GASTO, "#FF8800", "food", usuario));
    }

    @Test
    void actualizarCambiaElLimiteSinTocarElConsumo() {
        prepararDatos();
        PresupuestoResponse creado = presupuestoService.crear(EMAIL,
                new PresupuestoRequest(comida.getId(), new BigDecimal("100.00"), 2026, 10));
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("90.00"), TipoTransaccion.GASTO, null, FECHA, billetera.getId(), comida.getId()));

        PresupuestoResponse actualizado = presupuestoService.actualizar(EMAIL, creado.id(),
                new ActualizarPresupuestoRequest(new BigDecimal("200.00")));

        assertThat(actualizado.montoLimite()).isEqualByComparingTo("200.00");
        assertThat(actualizado.saldoConsumido()).isEqualByComparingTo("90.00");
        assertThat(actualizado.nivel()).isEqualTo("OK");
    }

    @Test
    void bajarElLimiteRecalculaElNivel() {
        prepararDatos();
        PresupuestoResponse creado = presupuestoService.crear(EMAIL,
                new PresupuestoRequest(comida.getId(), new BigDecimal("200.00"), 2026, 10));
        transaccionService.registrar(EMAIL, new TransaccionRequest(
                new BigDecimal("90.00"), TipoTransaccion.GASTO, null, FECHA, billetera.getId(), comida.getId()));

        PresupuestoResponse actualizado = presupuestoService.actualizar(EMAIL, creado.id(),
                new ActualizarPresupuestoRequest(new BigDecimal("100.00")));

        assertThat(actualizado.nivel()).isEqualTo("ADVERTENCIA");
    }
}
