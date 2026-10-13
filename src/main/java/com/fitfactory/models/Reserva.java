package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad relacional que representa la reserva de un Usuario en una Clase.
 * Constraint única: un usuario no puede reservar la misma clase dos veces.
 */
@Entity
@Table(name = "reservas",
        uniqueConstraints = {
                @UniqueConstraint(columnNames = {"usuario_id", "clase_id"}, name = "uk_reserva_usuario_clase")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "clase_id", nullable = false)
    private Clase clase;

    @Column(name = "fecha_reserva", nullable = false)
    private LocalDateTime fechaReserva;

    /**
     * Estado de la reserva: PENDIENTE, CONFIRMADA, CANCELADA.
     */
    @Column(nullable = false, length = 20)
    @Builder.Default
    private String estado = "CONFIRMADA";

    @PrePersist
    protected void onCreate() {
        this.fechaReserva = LocalDateTime.now();
    }
}
