package com.fitfactory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class ReservaResponse {
    private Long id;
    private Long claseId;
    private String tituloClase;
    private String categoria;
    private LocalDateTime fechaHoraInicio;
    private String sala;
    private String estado;
    private LocalDateTime fechaReserva;
}
