package com.fitfactory.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ClaseRequest {
    @NotBlank(message = "El título es obligatorio")
    private String titulo;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;
    
    @NotNull(message = "La fecha de inicio es obligatoria")
    @Future(message = "La fecha de inicio debe ser en el futuro")
    private LocalDateTime fechaHoraInicio;
    
    @NotNull(message = "La fecha de fin es obligatoria")
    @Future(message = "La fecha de fin debe ser en el futuro")
    private LocalDateTime fechaHoraFin;
    
    private String sala;
    
    @NotNull(message = "Los cupos totales son obligatorios")
    @Min(value = 1, message = "Debe haber al menos 1 cupo")
    private Integer cuposTotales;
}
