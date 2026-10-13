package com.fitfactory.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class EjercicioResponse {
    private Long id;
    private String nombre;
    private String grupoMuscular;
    private Integer series;
    private String repeticiones;
    private Integer descansoSegundos;
    private String instrucciones;
}
