package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ResumenSesionResponse {
    private Long sesionId;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionSegundos;
    private Integer totalEjercicios;
    private Integer totalSeries;
    private Double volumenTotalKg;
    private Integer recordsPersonales;
    private List<String> detallesRecords;
    private String notas;
}
