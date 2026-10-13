package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO devuelto por GET /api/usuarios/me.
 * Contiene todos los datos publicos del usuario autenticado.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UsuarioPerfilResponse {

    private Long id;
    private String nombre;
    private String email;
    private String telefono;

    /** Lista de roles: ["ROLE_CLIENTE"], ["ROLE_ADMIN"], etc. */
    private List<String> roles;

    /** Plan actual: "BASICO", "PRO", "ELITE" */
    private String planMembresia;

    /** Si la membresia esta activa o no */
    private Boolean membresiaActiva;

    /** URL de foto de perfil (puede ser null) */
    private String fotoUrl;
}
