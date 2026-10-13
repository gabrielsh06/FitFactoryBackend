package com.fitfactory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ClaseResponse {
    private Long id;
    private String titulo;
    private String categoria;
    private LocalDateTime fechaHoraInicio;
    private LocalDateTime fechaHoraFin;
    private String sala;
    private Integer cuposTotales;
    private Integer cuposOcupados;
    private Integer cuposDisponibles;
    private String nombreEntrenador;
    private Boolean reservadaPorMi;
}
