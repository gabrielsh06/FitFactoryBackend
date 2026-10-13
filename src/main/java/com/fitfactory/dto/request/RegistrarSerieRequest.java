package com.fitfactory.dto.request;

import com.fitfactory.models.TipoSerie;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

@Data
public class RegistrarSerieRequest {
    private Long serieId;
    private Long ejercicioId;

    @NotBlank(message = "El nombre del ejercicio es obligatorio")
    private String nombreEjercicio;

    @NotNull(message = "El número de serie es obligatorio")
    private Integer numeroSerie;

    private TipoSerie tipoSerie = TipoSerie.NORMAL;

    @NotNull(message = "El peso levantado es obligatorio")
    private Double pesoKg;

    @NotNull(message = "Las repeticiones son obligatorias")
    @Positive(message = "Las repeticiones deben ser mayores a 0")
    private Integer repeticiones;

    private Boolean completada = true;

    private Integer rpe;
}
