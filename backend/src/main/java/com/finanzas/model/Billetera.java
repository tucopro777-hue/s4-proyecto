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
import jakarta.validation.constraints.PositiveOrZero;

import java.math.BigDecimal;

@Entity
@Table(name = "billeteras")
public class Billetera {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    @PositiveOrZero
    private BigDecimal saldoInicial;

    /** Saldo vigente: arranca en el saldo inicial y lo modifican las transacciones. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal saldoActual;

    /** Código ISO de 3 letras, p. ej. BOB o USD. */
    @Column(nullable = false, length = 3)
    private String divisa;

    /** Desactivación lógica: la billetera deja de listarse pero conserva su historial. */
    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Requerido por JPA. */
    protected Billetera() {
    }

    public Billetera(String nombre, BigDecimal saldoInicial, String divisa, Usuario usuario) {
        this.nombre = nombre;
        this.saldoInicial = saldoInicial;
        this.saldoActual = saldoInicial;
        this.divisa = divisa;
        this.usuario = usuario;
    }

    /** Un ingreso suma al saldo y un gasto lo resta. Quien llama debe validar el saldo antes. */
    public void aplicarMovimiento(TipoTransaccion tipo, BigDecimal monto) {
        this.saldoActual = tipo == TipoTransaccion.INGRESO
                ? saldoActual.add(monto)
                : saldoActual.subtract(monto);
    }

    public void desactivar() {
        this.activa = false;
    }

    public Long getId() {
        return id;
    }

    public String getNombre() {
        return nombre;
    }

    public void setNombre(String nombre) {
        this.nombre = nombre;
    }

    public BigDecimal getSaldoInicial() {
        return saldoInicial;
    }

    public void setSaldoInicial(BigDecimal saldoInicial) {
        this.saldoInicial = saldoInicial;
    }

    public BigDecimal getSaldoActual() {
        return saldoActual;
    }

    public String getDivisa() {
        return divisa;
    }

    public void setDivisa(String divisa) {
        this.divisa = divisa;
    }

    public boolean isActiva() {
        return activa;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
