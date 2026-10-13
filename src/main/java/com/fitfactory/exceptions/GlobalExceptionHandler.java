package com.fitfactory.exceptions;

import com.fitfactory.dto.response.MessageResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.HashMap;
import java.util.Map;

/**
 * Manejador global de excepciones para toda la API REST de FitFactory.
 *
 * <pre>
 * 400 → MethodArgumentNotValidException  (validaciones @Valid)
 * 401 → BadCredentialsException          (credenciales inválidas)
 * 403 → AccessDeniedException            (sin permisos / rol incorrecto)
 * 403 → DisabledException                (cuenta desactivada)
 * 404 → NoResourceFoundException         (endpoint no encontrado)
 * 409 → IllegalArgumentException         (conflictos de negocio)
 * 500 → Exception                        (errores inesperados — sin exponer detalles al cliente)
 * </pre>
 *
 * SEGURIDAD: Ningún handler expone stack traces ni mensajes internos al cliente.
 * Los detalles completos se registran únicamente en el log del servidor.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    // ── 400 - Errores de validación @Valid ─────────────────────────────────────
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationErrors(MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Datos inválidos. Revisa los campos.");
        body.put("errors", fieldErrors);
        log.warn("[Validación] Campos inválidos: {}", fieldErrors);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(body);
    }

    // ── 401 - Credenciales incorrectas ─────────────────────────────────────────
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<MessageResponse> handleBadCredentials(BadCredentialsException ex) {
        log.warn("[Auth] Intento de login con credenciales inválidas");
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(MessageResponse.error("Credenciales inválidas. Verifica tu correo y contraseña."));
    }

    // ── 403 - Cuenta desactivada ────────────────────────────────────────────────
    @ExceptionHandler(DisabledException.class)
    public ResponseEntity<MessageResponse> handleDisabledException(DisabledException ex) {
        log.warn("[Auth] Intento de login en cuenta desactivada");
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error("Cuenta desactivada. Contacta al administrador."));
    }

    // ── 403 - Sin permisos / rol insuficiente ───────────────────────────────────
    // IMPORTANTE: Este handler evita que Spring Security devuelva HTML o mensajes
    // internos cuando un usuario autenticado accede a un recurso sin el rol correcto.
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<MessageResponse> handleAccessDenied(AccessDeniedException ex) {
        log.warn("[Autorización] Acceso denegado: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(MessageResponse.error("No tienes permisos para realizar esta acción."));
    }

    // ── 404 - Endpoint no encontrado (Spring MVC 6.x) ──────────────────────────
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<MessageResponse> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("[404] Recurso no encontrado: {}", ex.getResourcePath());
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(MessageResponse.error("El recurso solicitado no existe."));
    }

    // ── 409 - Conflictos de negocio ─────────────────────────────────────────────
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<MessageResponse> handleIllegalArgument(IllegalArgumentException ex) {
        log.warn("[Negocio] Conflicto: {}", ex.getMessage());
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(MessageResponse.error(ex.getMessage()));
    }

    // ── 500 - Error inesperado ──────────────────────────────────────────────────
    // CRÍTICO: NO exponer ex.getMessage() ni el stack trace al cliente.
    // El mensaje de error interno se registra en el servidor para debugging.
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleGenericException(Exception ex) {
        log.error("[Error Interno] Excepción no controlada: {}", ex.getMessage(), ex);
        Map<String, Object> body = new HashMap<>();
        body.put("success", false);
        body.put("message", "Error interno del servidor. Intenta más tarde.");
        // Sin campo 'details' → no filtramos información interna al cliente
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }
}

