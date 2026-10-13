package com.fitfactory.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ActualizarRolRequest {
    @NotBlank(message = "El rol es obligatorio")
    private String rol;
}
