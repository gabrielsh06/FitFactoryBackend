package com.fitfactory.dto.response;

import com.fitfactory.models.EstadoSesion;
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
public class SesionEntrenamientoResponse {
    private Long id;
    private Long clienteId;
    private Long rutinaId;
    private String rutinaTitulo;
    private LocalDateTime fechaInicio;
    private LocalDateTime fechaFin;
    private Integer duracionSegundos;
    private EstadoSesion estado;
    private Double volumenTotalKg;
    private Integer totalSeries;
    private String notas;
    private List<RegistroSerieResponse> series;
}
