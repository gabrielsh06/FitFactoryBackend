package com.fitfactory.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarPerfilRequest {
    
    @NotBlank(message = "El nombre es obligatorio")
    private String nombre;
    
    private String telefono;
    
    @Min(value = 30, message = "El peso debe ser al menos 30 kg")
    @Max(value = 300, message = "El peso no puede exceder 300 kg")
    private Double peso;
    
    @Min(value = 100, message = "La altura debe ser al menos 100 cm")
    @Max(value = 250, message = "La altura no puede exceder 250 cm")
    private Double altura;
    
    private String objetivoFitness;
}
