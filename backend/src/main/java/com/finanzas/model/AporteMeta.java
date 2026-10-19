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

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Dinero que sale de una billetera hacia una meta. Puede ser manual o automático (porcentaje
 * de un ingreso); en ese caso queda vinculado a la transacción de ingreso que lo originó.
 */
@Entity
@Table(name = "aportes_meta")
public class AporteMeta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "meta_id", nullable = false)
    private MetaAhorro meta;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "billetera_id", nullable = false)
    private Billetera billetera;

    /** Ingreso que originó el aporte automático; null si el aporte fue manual. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "transaccion_id")
    private Transaccion transaccion;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal monto;

    @Column(nullable = false)
    private LocalDate fecha;

    @Column(nullable = false)
    private boolean automatico;

    /** Requerido por JPA. */
    protected AporteMeta() {
    }

    public AporteMeta(Usuario usuario, MetaAhorro meta, Billetera billetera, Transaccion transaccion,
                      BigDecimal monto, LocalDate fecha, boolean automatico) {
        this.usuario = usuario;
        this.meta = meta;
        this.billetera = billetera;
        this.transaccion = transaccion;
        this.monto = monto;
        this.fecha = fecha;
        this.automatico = automatico;
    }

    public Long getId() {
        return id;
    }

    public MetaAhorro getMeta() {
        return meta;
    }

    public Billetera getBilletera() {
        return billetera;
    }

    public BigDecimal getMonto() {
        return monto;
    }

    public boolean isAutomatico() {
        return automatico;
    }
}
