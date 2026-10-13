package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Entidad que representa una Clase del gimnasio FitFactory.
 * Cada clase está dirigida por un entrenador (Usuario con ROLE_ENTRENADOR).
 */
@Entity
@Table(name = "clases")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Clase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String titulo;

    @Column(nullable = false, length = 80)
    private String categoria;

    @Column(name = "fecha_hora_inicio", nullable = false)
    private LocalDateTime fechaHoraInicio;

    @Column(name = "fecha_hora_fin", nullable = false)
    private LocalDateTime fechaHoraFin;

    @Column(length = 80)
    private String sala;

    @Column(name = "cupos_totales", nullable = false)
    private Integer cuposTotales;

    @Column(name = "cupos_ocupados", nullable = false)
    @Builder.Default
    private Integer cuposOcupados = 0;

    /** Descripción breve de la clase */
    @Column(length = 500)
    private String descripcion;

    /** Entrenador que imparte la clase */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "entrenador_id", nullable = false)
    private Usuario entrenador;
}
