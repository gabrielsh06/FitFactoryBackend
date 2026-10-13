package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioAdminResponse {
    private Long id;
    private String nombre;
    private String email;
    private String telefono;
    private List<String> roles;
    private Boolean estadoActivo;
    private LocalDateTime fechaCreacion;
}
