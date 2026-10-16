package com.finanzas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "transacciones")
public class Transaccion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, precision = 12, scale = 2)
    @Positive
    private BigDecimal monto;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoTransaccion tipo;

    @Column(length = 255)
    private String descripcion;

    @Column(nullable = false)
    private LocalDate fecha;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billetera_id", nullable = false)
    private Billetera billetera;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "categoria_id", nullable = false)
    private Categoria categoria;

    /** Requerido por JPA. */
    protected Transaccion() {
    }

    public Transaccion(BigDecimal monto, TipoTransaccion tipo, String descripcion, LocalDate fecha,
                       Usuario usuario, Billetera billetera, Categoria categoria) {
        this.monto = monto;
        this.tipo = tipo;
        this.descripcion = descripcion;
        this.fecha = fecha;
        this.usuario = usuario;
        this.billetera = billetera;
        this.categoria = categoria;
    }

    public Long getId() {
        return id;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public TipoTransaccion getTipo() {
        return tipo;
    }

    public String getDescripcion() {
        return descripcion;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public Billetera getBilletera() {
        return billetera;
    }

    public Categoria getCategoria() {
        return categoria;
    }
}
