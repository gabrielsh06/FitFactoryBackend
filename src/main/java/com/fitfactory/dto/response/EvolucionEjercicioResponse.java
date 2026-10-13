package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionEjercicioResponse {
    private Long ejercicioId;
    private String nombreEjercicio;
    private Double maxPesoHistorico;
    private Integer totalSeriesCompletadas;
    private List<EvolucionSerieDTO> series;
}
