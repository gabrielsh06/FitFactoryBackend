package com.fitfactory.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO para POST /api/auth/verify-otp.
 * Verifica que el código OTP recibido por email sea válido y no haya expirado.
 */
@Data
public class VerifyOtpRequest {

    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "El código OTP es obligatorio")
    @Pattern(regexp = "\\d{6}", message = "El código debe tener exactamente 6 dígitos")
    private String codigo;
}
