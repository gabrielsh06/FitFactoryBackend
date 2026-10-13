package com.fitfactory.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class RutinaResponse {
    private Long id;
    private String titulo;
    private String objetivo;
    private String diaSemana;
    private Long entrenadorId;
    private String nombreEntrenador;
    private List<EjercicioResponse> ejercicios;
}
