package com.fitfactory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO para el endpoint POST /api/auth/register.
 * El rol es opcional; si no se envía, se asigna ROLE_CLIENTE por defecto.
 */
@Data
public class RegisterRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 100, message = "El nombre debe tener entre 2 y 100 caracteres")
    private String nombre;

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, max = 50, message = "La contraseña debe tener entre 6 y 50 caracteres")
    private String password;

    @Pattern(regexp = "^[+]?[0-9]{7,15}$", message = "Formato de teléfono inválido")
    private String telefono;

    /**
     * Rol deseado: "ROLE_CLIENTE", "ROLE_ENTRENADOR", "ROLE_ADMIN".
     * Si es null o vacío, se asigna ROLE_CLIENTE automáticamente.
     */
    private String rol;
}
