package com.finanzas.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    /** Contraseña almacenada como hash BCrypt; nunca en texto plano. */
    @Column(nullable = false)
    private String password;

    @Column(nullable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    /** Fallos de login consecutivos desde el último acceso correcto o desbloqueo. */
    @Column(name = "intentos_fallidos", nullable = false)
    private int intentosFallidos;

    /** Si está en el futuro, la cuenta no admite login hasta esa fecha. */
    @Column(name = "bloqueado_hasta")
    private LocalDateTime bloqueadoHasta;


    /** Requerido por JPA. */
    protected Usuario() {
    }

    public Usuario(String nombre, String email, String passwordHash) {
        this.nombre = nombre;
        this.email = email;
        this.password = passwordHash;
    }

    @PrePersist
    void antesDeGuardar() {
        if (fechaRegistro == null) {
            fechaRegistro = LocalDateTime.now();
        }
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

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public int getIntentosFallidos() {
        return intentosFallidos;
    }

    public LocalDateTime getBloqueadoHasta() {
        return bloqueadoHasta;
    }

    public void setBloqueadoHasta(LocalDateTime bloqueadoHasta) {
        this.bloqueadoHasta = bloqueadoHasta;
    }




    /** true si la cuenta está bloqueada en este momento. */
    public boolean estaBloqueada(LocalDateTime ahora) {
        return bloqueadoHasta != null && bloqueadoHasta.isAfter(ahora);
    }

    /** Registra un fallo de login; al alcanzar el máximo bloquea la cuenta y reinicia el contador. */
    public void registrarFalloLogin(LocalDateTime ahora, int maxIntentos, int minutosBloqueo) {
        intentosFallidos++;
        if (intentosFallidos >= maxIntentos) {
            bloqueadoHasta = ahora.plusMinutes(minutosBloqueo);
            intentosFallidos = 0;
        }
    }

    /** Tras un login correcto o un cambio de contraseña, limpia bloqueo y contadores. */
    public void limpiarBloqueo() {
        intentosFallidos = 0;
        bloqueadoHasta = null;
    }

}
