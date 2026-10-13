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
public class AsistenciasStatsResponse {

    private long totalAsistencias;
    private long totalPermitidos;
    private long totalDenegados;
    private List<DiaAsistenciaDTO> distribucionSemanal;
    private double tasaAsistenciaSemanal; // porcentaje de días asistidos en la semana (0-100)
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DiaAsistenciaDTO {
        private String dia;       // "Mon", "Tue", "Wed", "Thurs", "Fri", "Sat", "Sun"
        private String fecha;     // "YYYY-MM-DD"
        private long asistencias; // Cantidad de accesos permitidos ese día
    }
}
