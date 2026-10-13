package com.fitfactory.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AccesoResponse {
    private boolean permitido;
    private String nombre;
    private String planMembresia;
    private String mensajeCorto;
}
