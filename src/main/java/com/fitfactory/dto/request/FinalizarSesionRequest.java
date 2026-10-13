package com.fitfactory.dto.request;

import lombok.Data;

@Data
public class FinalizarSesionRequest {
    private String notas;
    private Integer duracionSegundos;
}
