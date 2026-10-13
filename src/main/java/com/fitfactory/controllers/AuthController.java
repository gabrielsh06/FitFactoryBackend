package com.fitfactory.controllers;

import com.fitfactory.dto.request.*;
import com.fitfactory.dto.response.AuthResponse;
import com.fitfactory.dto.response.MessageResponse;
import com.fitfactory.models.ERol;
import com.fitfactory.models.Rol;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.RolRepository;
import com.fitfactory.repositories.UsuarioRepository;
import com.fitfactory.security.jwt.JwtUtils;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.EmailService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Controlador REST de autenticación y recuperación de contraseña.
 *
 * <pre>
 * POST /api/auth/login              → Autentica y devuelve JWT
 * POST /api/auth/register           → Registra usuario y devuelve JWT
 * POST /api/auth/forgot-password    → Genera OTP y envía email
 * POST /api/auth/verify-otp         → Valida OTP sin cambiar contraseña
 * POST /api/auth/reset-password     → Valida OTP y actualiza contraseña
 * </pre>
 */
@Tag(name = "Auth", description = "Endpoints públicos de autenticación y recuperación de contraseña")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired private AuthenticationManager authenticationManager;
    @Autowired private UsuarioRepository usuarioRepository;
    @Autowired private RolRepository rolRepository;
    @Autowired private PasswordEncoder passwordEncoder;
    @Autowired private JwtUtils jwtUtils;
    @Autowired private EmailService emailService;

    @Value("${fitfactory.mail.otp-expiry-minutes:15}")
    private int otpExpiryMinutes;

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/login
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Iniciar sesión", description = "Autentica email + contraseña y retorna JWT.")
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest loginRequest) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        Usuario usuario = usuarioRepository.findByEmail(userDetails.getEmail()).orElseThrow();

        return ResponseEntity.ok(AuthResponse.builder()
                .token(jwt).tipo("Bearer")
                .id(userDetails.getId()).nombre(userDetails.getNombre())
                .email(userDetails.getEmail()).roles(roles)
                .planMembresia(usuario.getPlanMembresia())
                .membresiaActiva(usuario.getMembresiaActiva())
                .peso(usuario.getPeso()).altura(usuario.getAltura())
                .imc(usuario.getImc()).objetivoFitness(usuario.getObjetivoFitness())
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/register
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Registrar usuario", description = "Registra un nuevo usuario y retorna JWT.")
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest registerRequest) {
        if (usuarioRepository.existsByEmail(registerRequest.getEmail())) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body(MessageResponse.error("Error: El email ya está registrado."));
        }

        Set<Rol> roles = new HashSet<>();
        ERol rolEnum = resolveRol(registerRequest.getRol());
        Rol rol = rolRepository.findByNombre(rolEnum)
                .orElseThrow(() -> new RuntimeException(
                        "Error: Rol '" + rolEnum.name() + "' no encontrado en BD."));
        roles.add(rol);

        Usuario nuevoUsuario = Usuario.builder()
                .nombre(registerRequest.getNombre())
                .email(registerRequest.getEmail())
                .password(passwordEncoder.encode(registerRequest.getPassword()))
                .telefono(registerRequest.getTelefono())
                .planMembresia("BASICO").membresiaActiva(false)
                .roles(roles).build();

        usuarioRepository.save(nuevoUsuario);

        UserDetailsImpl userDetails = UserDetailsImpl.build(nuevoUsuario);
        String jwt = jwtUtils.generateTokenFromUserDetails(userDetails);
        List<String> rolesStr = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority).collect(Collectors.toList());

        return ResponseEntity.status(HttpStatus.CREATED).body(AuthResponse.builder()
                .token(jwt).tipo("Bearer")
                .id(nuevoUsuario.getId()).nombre(nuevoUsuario.getNombre())
                .email(nuevoUsuario.getEmail()).roles(rolesStr)
                .planMembresia(nuevoUsuario.getPlanMembresia())
                .membresiaActiva(nuevoUsuario.getMembresiaActiva())
                .peso(nuevoUsuario.getPeso()).altura(nuevoUsuario.getAltura())
                .imc(nuevoUsuario.getImc()).objetivoFitness(nuevoUsuario.getObjetivoFitness())
                .build());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/forgot-password
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * SEGURIDAD: siempre devuelve 200 OK aunque el email no exista,
     * para evitar el "user enumeration" (no informar si el email está registrado).
     */
    @Operation(summary = "Solicitar recuperación de contraseña",
               description = "Genera un OTP de 6 dígitos y lo envía al email (válido por 15 min).")
    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        usuarioRepository.findByEmail(request.getEmail()).ifPresent(usuario -> {
            String codigoPlano = generarOtp();
            String codigoHash  = passwordEncoder.encode(codigoPlano);

            usuario.setOtpCode(codigoHash);
            usuario.setOtpExpiry(LocalDateTime.now().plusMinutes(otpExpiryMinutes));
            usuarioRepository.save(usuario);

            // Envío asíncrono — no bloquea el hilo HTTP
            emailService.enviarCodigoRecuperacion(
                    usuario.getEmail(), usuario.getNombre(), codigoPlano, otpExpiryMinutes);
        });

        return ResponseEntity.ok(MessageResponse.ok(
                "Si ese email está registrado, recibirás un código de verificación en breve."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/verify-otp
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Verificar código OTP",
               description = "Valida el código sin cambiar la contraseña. Útil para la pantalla de verificación.")
    @PostMapping("/verify-otp")
    public ResponseEntity<MessageResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email no registrado."));

        validarOtp(usuario, request.getCodigo());
        return ResponseEntity.ok(MessageResponse.ok("Código verificado correctamente."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // POST /api/auth/reset-password
    // ─────────────────────────────────────────────────────────────────────────

    @Operation(summary = "Restablecer contraseña",
               description = "Valida OTP y actualiza la contraseña. El OTP se invalida tras el uso.")
    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        Usuario usuario = usuarioRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Email no registrado."));

        validarOtp(usuario, request.getCodigo());

        // Actualizar contraseña y limpiar OTP
        usuario.setPassword(passwordEncoder.encode(request.getNuevaPassword()));
        usuario.setOtpCode(null);
        usuario.setOtpExpiry(null);
        usuarioRepository.save(usuario);

        // Notificación de cambio exitoso (asíncrona)
        emailService.enviarConfirmacionCambioContrasena(usuario.getEmail(), usuario.getNombre());

        return ResponseEntity.ok(MessageResponse.ok("Contraseña restablecida exitosamente."));
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Helpers privados
    // ─────────────────────────────────────────────────────────────────────────

    /** Genera un OTP numérico de 6 dígitos. */
    private String generarOtp() {
        return String.format("%06d", new Random().nextInt(1_000_000));
    }

    /**
     * Valida que el OTP sea correcto y no haya expirado.
     * Lanza {@link IllegalArgumentException} (→ 409) en caso contrario.
     */
    private void validarOtp(Usuario usuario, String codigoPlano) {
        if (usuario.getOtpCode() == null || usuario.getOtpExpiry() == null) {
            throw new IllegalArgumentException(
                    "No hay un código de recuperación activo. Solicita uno nuevo.");
        }
        if (LocalDateTime.now().isAfter(usuario.getOtpExpiry())) {
            throw new IllegalArgumentException(
                    "El código ha expirado. Solicita uno nuevo desde la pantalla de recuperación.");
        }
        if (!passwordEncoder.matches(codigoPlano, usuario.getOtpCode())) {
            throw new IllegalArgumentException("Código incorrecto. Verifica e intenta de nuevo.");
        }
    }

    private ERol resolveRol(String rolStr) {
        if (rolStr == null || rolStr.isBlank()) return ERol.ROLE_CLIENTE;
        return switch (rolStr.toUpperCase().trim()) {
            case "ROLE_ENTRENADOR", "ENTRENADOR" -> ERol.ROLE_ENTRENADOR;
            case "ROLE_ADMIN",     "ADMIN"       -> ERol.ROLE_ADMIN;
            default                               -> ERol.ROLE_CLIENTE;
        };
    }
}

