package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "registros_series")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RegistroSerie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sesion_id", nullable = false)
    private SesionEntrenamiento sesion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ejercicio_id")
    private Ejercicio ejercicio;

    @Column(name = "nombre_ejercicio", nullable = false, length = 120)
    private String nombreEjercicio;

    @Column(name = "numero_serie", nullable = false)
    private Integer numeroSerie;

    @Enumerated(EnumType.STRING)
    @Column(name = "tipo_serie", nullable = false, length = 20)
    @Builder.Default
    private TipoSerie tipoSerie = TipoSerie.NORMAL;

    @Column(name = "peso_kg", nullable = false)
    private Double pesoKg;

    @Column(name = "repeticiones", nullable = false)
    private Integer repeticiones;

    @Column(nullable = false)
    @Builder.Default
    private Boolean completada = true;

    @Column(name = "es_record_personal", nullable = false)
    @Builder.Default
    private Boolean esRecordPersonal = false;

    @Column(name = "rpe")
    private Integer rpe;

    @Column(name = "fecha_hora", nullable = false)
    private LocalDateTime fechaHora;

    @PrePersist
    protected void onCreate() {
        if (this.fechaHora == null) {
            this.fechaHora = LocalDateTime.now();
        }
        if (this.completada == null) {
            this.completada = true;
        }
        if (this.esRecordPersonal == null) {
            this.esRecordPersonal = false;
        }
    }
}
