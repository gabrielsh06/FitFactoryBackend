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
public class AsistenciaResponse {

    private Long id;
    private LocalDateTime fechaHoraAcceso;
    private Boolean accesoPermitido;
    private String observacion;
}
