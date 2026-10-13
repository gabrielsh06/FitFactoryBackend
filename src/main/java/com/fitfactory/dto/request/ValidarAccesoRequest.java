package com.fitfactory.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ValidarAccesoRequest {
    
    @NotNull(message = "El ID de usuario es obligatorio")
    private Long usuarioId;

    @NotNull(message = "El timestamp es obligatorio")
    private Long timestamp;
}
