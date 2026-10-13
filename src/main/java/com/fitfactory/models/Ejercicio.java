package com.fitfactory.models;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "ejercicios")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Ejercicio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String nombre;

    @Column(name = "grupo_muscular", length = 50)
    private String grupoMuscular;

    private Integer series;

    @Column(length = 20)
    private String repeticiones;

    @Column(name = "descanso_segundos")
    private Integer descansoSegundos;

    @Column(length = 255)
    private String instrucciones;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rutina_id", nullable = false)
    private Rutina rutina;
}
