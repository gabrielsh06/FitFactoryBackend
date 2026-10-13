package com.fitfactory.dto.response;

import com.fitfactory.models.TipoSerie;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EvolucionSerieDTO {
    private Long id;
    private Long sesionId;
    private Integer numeroSerie;
    private TipoSerie tipoSerie;
    private Double pesoKg;
    private Integer repeticiones;
    private Boolean completada;
    private Boolean esRecordPersonal;
    private Integer rpe;
    private LocalDateTime fechaHora;
}
