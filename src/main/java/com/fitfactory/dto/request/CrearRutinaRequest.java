package com.fitfactory.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class CrearRutinaRequest {

    @NotNull(message = "El ID del cliente es obligatorio")
    private Long clienteId;

    @NotBlank(message = "El título es obligatorio")
    private String titulo;

    private String objetivo;

    private String diaSemana;

    @NotEmpty(message = "Debe haber al menos un ejercicio")
    @Valid
    private List<EjercicioRequest> ejercicios;
}
