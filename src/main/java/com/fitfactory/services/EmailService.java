package com.fitfactory.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

/**
 * Servicio de envío de correos electrónicos para FitFactory.
 *
 * <p>Todos los métodos públicos están anotados con {@code @Async} para no bloquear
 * el hilo HTTP que atiende la petición. Requiere {@code @EnableAsync} en la clase
 * principal de la aplicación.
 *
 * <p><b>Configuración necesaria en application.properties:</b>
 * <pre>
 * spring.mail.host, spring.mail.port, spring.mail.username, spring.mail.password
 * fitfactory.mail.from
 * </pre>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${fitfactory.mail.from}")
    private String mailFrom;

    // ─────────────────────────────────────────────────────────────────────────
    // API pública
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Envía el correo con el código OTP de recuperación de contraseña.
     *
     * <p>La ejecución es <b>asíncrona</b>: el controlador responde 200 OK
     * inmediatamente sin esperar a que el servidor SMTP confirme la entrega.
     *
     * @param destinatario email del usuario que solicitó el reset
     * @param nombre       nombre del usuario (para personalizar el saludo)
     * @param codigo       código OTP de 6 dígitos generado en AuthService
     * @param expiryMin    minutos hasta que expira el código
     */
    @Async
    public void enviarCodigoRecuperacion(String destinatario, String nombre,
                                         String codigo, int expiryMin) {
        String asunto  = "🔐 Código de verificación FitFactory";
        String cuerpo  = buildPasswordResetHtml(nombre, codigo, expiryMin);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    /**
     * Envía confirmación de que la contraseña fue restablecida exitosamente.
     *
     * @param destinatario email del usuario
     * @param nombre       nombre del usuario
     */
    @Async
    public void enviarConfirmacionCambioContrasena(String destinatario, String nombre) {
        String asunto = "✅ Tu contraseña fue actualizada — FitFactory";
        String cuerpo = buildPasswordChangedHtml(nombre);
        enviarHtml(destinatario, asunto, cuerpo);
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helper de envío
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Método base de envío MIME (HTML). Centraliza el manejo de errores SMTP
     * y garantiza que un fallo de correo NUNCA propague excepción al caller
     * (el usuario debe poder seguir usando la app aunque el SMTP falle).
     */
    private void enviarHtml(String destinatario, String asunto, String cuerpoHtml) {
        try {
            MimeMessage mensaje = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mensaje, true, "UTF-8");

            helper.setFrom(mailFrom);
            helper.setTo(destinatario);
            helper.setSubject(asunto);
            helper.setText(cuerpoHtml, true); // true = isHtml

            mailSender.send(mensaje);
            log.info("[EmailService] Correo enviado a {} | Asunto: {}", destinatario, asunto);

        } catch (MessagingException e) {
            log.error("[EmailService] Error al construir el mensaje para {}: {}", destinatario, e.getMessage());
        } catch (MailException e) {
            log.error("[EmailService] Error SMTP al enviar a {}: {}", destinatario, e.getMessage());
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Templates HTML
    // ─────────────────────────────────────────────────────────────────────────

    private String buildPasswordResetHtml(String nombre, String codigo, int expiryMin) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head>
                  <meta charset="UTF-8"/>
                  <title>Recuperación de contraseña — FitFactory</title>
                </head>
                <body style="margin:0;padding:0;background:#0a101d;font-family:'Helvetica Neue',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a101d;padding:40px 0;">
                    <tr><td align="center">
                      <table width="500" cellpadding="0" cellspacing="0"
                             style="background:#111827;border-radius:16px;overflow:hidden;">

                        <!-- Header -->
                        <tr>
                          <td style="background:linear-gradient(135deg,#1a4aff,#7c3aed);padding:32px;text-align:center;">
                            <h1 style="margin:0;color:#fff;font-size:24px;font-weight:700;">
                              🏋️ FitFactory
                            </h1>
                            <p style="margin:8px 0 0;color:rgba(255,255,255,0.8);font-size:14px;">
                              Recuperación de contraseña
                            </p>
                          </td>
                        </tr>

                        <!-- Body -->
                        <tr>
                          <td style="padding:36px 40px;">
                            <p style="color:#e5e7eb;font-size:16px;margin:0 0 16px;">
                              Hola, <strong style="color:#fff;">%s</strong> 👋
                            </p>
                            <p style="color:#9ca3af;font-size:14px;margin:0 0 28px;line-height:1.6;">
                              Recibimos una solicitud para restablecer la contraseña de tu cuenta.
                              Usa el siguiente código de verificación:
                            </p>

                            <!-- OTP Box -->
                            <div style="background:#1f2937;border:2px solid #1a4aff;border-radius:12px;
                                        padding:24px;text-align:center;margin:0 0 28px;">
                              <span style="font-size:40px;font-weight:900;letter-spacing:12px;color:#60a5fa;">
                                %s
                              </span>
                              <p style="margin:12px 0 0;color:#9ca3af;font-size:12px;">
                                ⏱ Este código expira en <strong style="color:#fbbf24;">%d minutos</strong>
                              </p>
                            </div>

                            <p style="color:#6b7280;font-size:12px;margin:0;line-height:1.6;">
                              Si no solicitaste este cambio, puedes ignorar este correo de forma segura.
                              Tu contraseña actual seguirá siendo la misma.
                            </p>
                          </td>
                        </tr>

                        <!-- Footer -->
                        <tr>
                          <td style="background:#0a101d;padding:20px 40px;text-align:center;">
                            <p style="margin:0;color:#4b5563;font-size:11px;">
                              © 2026 FitFactory · Sistema de Gestión de Gimnasio
                            </p>
                          </td>
                        </tr>

                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre, codigo, expiryMin);
    }

    private String buildPasswordChangedHtml(String nombre) {
        return """
                <!DOCTYPE html>
                <html lang="es">
                <head><meta charset="UTF-8"/></head>
                <body style="margin:0;padding:0;background:#0a101d;font-family:'Helvetica Neue',Arial,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0" style="background:#0a101d;padding:40px 0;">
                    <tr><td align="center">
                      <table width="500" cellpadding="0" cellspacing="0"
                             style="background:#111827;border-radius:16px;overflow:hidden;">
                        <tr>
                          <td style="background:linear-gradient(135deg,#059669,#1a4aff);padding:32px;text-align:center;">
                            <h1 style="margin:0;color:#fff;font-size:24px;font-weight:700;">
                              ✅ Contraseña actualizada
                            </h1>
                          </td>
                        </tr>
                        <tr>
                          <td style="padding:36px 40px;">
                            <p style="color:#e5e7eb;font-size:16px;margin:0 0 16px;">
                              Hola, <strong style="color:#fff;">%s</strong>
                            </p>
                            <p style="color:#9ca3af;font-size:14px;line-height:1.6;margin:0 0 20px;">
                              Tu contraseña de FitFactory fue restablecida exitosamente.
                            </p>
                            <p style="color:#ef4444;font-size:13px;line-height:1.6;margin:0;">
                              ⚠️ Si no realizaste este cambio, contacta inmediatamente a soporte.
                            </p>
                          </td>
                        </tr>
                        <tr>
                          <td style="background:#0a101d;padding:20px 40px;text-align:center;">
                            <p style="margin:0;color:#4b5563;font-size:11px;">
                              © 2026 FitFactory · Sistema de Gestión de Gimnasio
                            </p>
                          </td>
                        </tr>
                      </table>
                    </td></tr>
                  </table>
                </body>
                </html>
                """.formatted(nombre);
    }
}
