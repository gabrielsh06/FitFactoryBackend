package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UltimoHistorialResponse {
    private Long ejercicioId;
    private String nombreEjercicio;
    private Double ultimoPesoKg;
    private Integer ultimasRepeticiones;
    private LocalDateTime ultimaFecha;
    private Double recordPersonalPesoKg;
    private Integer recordPersonalReps;
}
