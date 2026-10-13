package com.fitfactory.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EjercicioRequest {

    @NotBlank(message = "El nombre del ejercicio es obligatorio")
    private String nombre;

    private String grupoMuscular;

    @NotNull(message = "El número de series es obligatorio")
    private Integer series;

    @NotBlank(message = "El número de repeticiones es obligatorio")
    private String repeticiones;

    private Integer descansoSegundos;

    private String instrucciones;
}
