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
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
        name = "categorias",
        uniqueConstraints = @UniqueConstraint(name = "uk_categoria_usuario_nombre", columnNames = {"usuario_id", "nombre"})
)
public class Categoria {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String nombre;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoCategoria tipo;

    /** Color en formato hexadecimal, p. ej. #FF8800. */
    @Column(nullable = false, length = 7)
    private String color;

    /** Identificador del icono elegido en el frontend, p. ej. "cart". */
    @Column(nullable = false, length = 30)
    private String icono;

    /** Desactivación lógica: la categoría deja de listarse pero los gastos existentes la conservan. */
    @Column(nullable = false)
    private boolean activa = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    /** Requerido por JPA. */
    protected Categoria() {
    }

    public Categoria(String nombre, TipoCategoria tipo, String color, String icono, Usuario usuario) {
        this.nombre = nombre;
        this.tipo = tipo;
        this.color = color;
        this.icono = icono;
        this.usuario = usuario;
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

    public TipoCategoria getTipo() {
        return tipo;
    }

    public void setTipo(TipoCategoria tipo) {
        this.tipo = tipo;
    }

    public String getColor() {
        return color;
    }

    public void setColor(String color) {
        this.color = color;
    }

    public String getIcono() {
        return icono;
    }

    public void setIcono(String icono) {
        this.icono = icono;
    }

    public boolean isActiva() {
        return activa;
    }

    public Usuario getUsuario() {
        return usuario;
    }
}
