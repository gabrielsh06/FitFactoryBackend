package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

/**
 * Entidad que representa un Usuario registrado en FitFactory.
 * Puede tener múltiples roles (cliente, entrenador, admin).
 */
@Entity
@Table(name = "usuarios",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = "email")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(nullable = false, unique = true, length = 150)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(length = 20)
    private String telefono;

    @Column(name = "plan_membresia", length = 50)
    @Builder.Default
    private String planMembresia = "BASICO";

    @Column(name = "membresia_activa")
    @Builder.Default
    private Boolean membresiaActiva = false;

    @Column(name = "foto_url", length = 300)
    private String fotoUrl;

    @Column(name = "activo", nullable = false)
    @Builder.Default
    private Boolean activo = true;

    @Column(name = "peso")
    private Double peso;

    @Column(name = "altura")
    private Double altura;

    @Column(name = "imc")
    private Double imc;

    @Column(name = "objetivo_fitness", length = 100)
    private String objetivoFitness;

    // ── Recuperación de contraseña (OTP) ───────────────────────────────────
    /** Código OTP hasheado (BCrypt). Null cuando no hay reset pendiente. */
    @Column(name = "otp_code", length = 100)
    private String otpCode;

    /** Momento de expiración del OTP. Null cuando no hay reset pendiente. */
    @Column(name = "otp_expiry")
    private java.time.LocalDateTime otpExpiry;
    // ──────────────────────────────────────────────────────────────────────

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "usuario_roles",
            joinColumns = @JoinColumn(name = "usuario_id"),
            inverseJoinColumns = @JoinColumn(name = "rol_id")
    )
    @Builder.Default
    private Set<Rol> roles = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        this.fechaCreacion = LocalDateTime.now();
    }
}
