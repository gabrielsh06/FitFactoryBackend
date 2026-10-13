package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "sesiones_entrenamiento")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SesionEntrenamiento {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cliente_id", nullable = false)
    private Usuario cliente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id")
    private Rutina rutina;

    @Column(name = "fecha_inicio", nullable = false)
    private LocalDateTime fechaInicio;

    @Column(name = "fecha_fin")
    private LocalDateTime fechaFin;

    @Column(name = "duracion_segundos")
    private Integer duracionSegundos;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private EstadoSesion estado = EstadoSesion.EN_PROGRESO;

    @Column(name = "volumen_total_kg")
    @Builder.Default
    private Double volumenTotalKg = 0.0;

    @Column(name = "total_series")
    @Builder.Default
    private Integer totalSeries = 0;

    @Column(length = 500)
    private String notas;

    @OneToMany(mappedBy = "sesion", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RegistroSerie> registros = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        if (this.fechaInicio == null) {
            this.fechaInicio = LocalDateTime.now();
        }
        if (this.estado == null) {
            this.estado = EstadoSesion.EN_PROGRESO;
        }
    }
}
