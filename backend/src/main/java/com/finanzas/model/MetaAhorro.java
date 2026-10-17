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
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Meta de ahorro con monto objetivo. Solo cambia mediante aportes explícitos. */
@Entity
@Table(name = "metas_ahorro")
public class MetaAhorro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, precision = 12, scale = 2)
    @Positive
    private BigDecimal montoObjetivo;

    /** Lo acumulado hasta ahora. Empieza en 0 y solo crece con aportes. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal montoActual = BigDecimal.ZERO;

    /** Fecha objetivo para cumplir la meta; opcional. */
    private LocalDate fechaLimite;

    /** Requerido por JPA. */
    protected MetaAhorro() {
    }

    public MetaAhorro(Usuario usuario, String nombre, BigDecimal montoObjetivo, LocalDate fechaLimite) {
        this.usuario = usuario;
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.fechaLimite = fechaLimite;
    }

    public void actualizar(String nombre, BigDecimal montoObjetivo, LocalDate fechaLimite) {
        this.nombre = nombre;
        this.montoObjetivo = montoObjetivo;
        this.fechaLimite = fechaLimite;
    }

    public void aportar(BigDecimal monto) {
        this.montoActual = montoActual.add(monto);
    }

    public Long getId() {
        return id;
    }

    public Usuario getUsuario() {
        return usuario;
    }

    public String getNombre() {
        return nombre;
    }

    public BigDecimal getMontoObjetivo() {
        return montoObjetivo;
    }

    public BigDecimal getMontoActual() {
        return montoActual;
    }

    public LocalDate getFechaLimite() {
        return fechaLimite;
    }
}
