package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * DTO de respuesta JWT enviado tras autenticación exitosa (login o registro).
 * Compatible con los clientes Angular y React Native/Expo.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private String token;

    @Builder.Default
    private String tipo = "Bearer";

    private Long id;
    private String nombre;
    private String email;

    /** Lista de roles: ["ROLE_CLIENTE"], ["ROLE_ADMIN"], etc. */
    private List<String> roles;

    private String planMembresia;
    private Boolean membresiaActiva;

    private Double peso;
    private Double altura;
    private Double imc;
    private String objetivoFitness;
}
