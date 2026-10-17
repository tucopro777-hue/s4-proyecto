package com.finanzas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.YearMonth;

/** Límite de gasto de una categoría de GASTO para un mes concreto. */
@Entity
@Table(
        name = "presupuestos",
        uniqueConstraints = @UniqueConstraint(
                name = "uk_presupuesto_periodo",
                columnNames = {"usuario_id", "categoria_id", "anio", "mes"})
)
public class Presupuesto {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    @Column(nullable = false, precision = 12, scale = 2)
    @Positive
    private BigDecimal montoLimite;

    @Column(nullable = false)
    private int anio;

    /** Mes del 1 al 12. */
    @Column(nullable = false)
    private int mes;

    /** Suma de los gastos del periodo no anulados; se mantiene al registrar y anular movimientos. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoConsumido = BigDecimal.ZERO;

    /** Requerido por JPA. */
    protected Presupuesto() {
    }

    public Presupuesto(Usuario usuario, Categoria categoria, BigDecimal montoLimite, YearMonth periodo,
                       BigDecimal consumidoInicial) {
        this.usuario = usuario;
        this.categoria = categoria;
        this.montoLimite = montoLimite;
        this.anio = periodo.getYear();
        this.mes = periodo.getMonthValue();
        this.saldoConsumido = consumidoInicial;
    }

    public void cambiarLimite(BigDecimal nuevoLimite) {
        this.montoLimite = nuevoLimite;
    }

    public void registrarConsumo(BigDecimal monto) {
        this.saldoConsumido = saldoConsumido.add(monto);
    }

    public void revertirConsumo(BigDecimal monto) {
        this.saldoConsumido = saldoConsumido.subtract(monto);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public Categoria getCategoria() {
        return categoria;
    }

    public BigDecimal getMontoLimite() {
        return montoLimite;
    }

    public YearMonth getPeriodo() {
        return YearMonth.of(anio, mes);
    }

    public BigDecimal getSaldoConsumido() {
        return saldoConsumido;
    }
}
