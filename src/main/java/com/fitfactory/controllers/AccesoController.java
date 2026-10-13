package com.fitfactory.controllers;

import com.fitfactory.dto.request.ValidarAccesoRequest;
import com.fitfactory.dto.response.AccesoResponse;
import com.fitfactory.dto.response.AsistenciaResponse;
import com.fitfactory.dto.response.AsistenciasStatsResponse;
import com.fitfactory.models.Asistencia;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.AsistenciaRepository;
import com.fitfactory.repositories.UsuarioRepository;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Tag(name = "Accesos y Asistencias", description = "Validación de accesos al gimnasio y consulta de asistencias")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/accesos")
public class AccesoController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    @Autowired
    private AsistenciaService asistenciaService;

    @Operation(summary = "Validar acceso por código QR", description = "Valida el timestamp y estado de membresía del usuario para autorizar el ingreso.")
    @PostMapping("/validar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<AccesoResponse> validarAcceso(@Valid @RequestBody ValidarAccesoRequest request) {
        
        long currentTime = System.currentTimeMillis();
        long diffSeconds = (currentTime - request.getTimestamp()) / 1000;

        Usuario usuario = usuarioRepository.findById(request.getUsuarioId()).orElse(null);

        // Validaciones base
        if (usuario == null) {
            return registrarYResponder(null, false, "Usuario no encontrado", "USUARIO_NO_ENCONTRADO");
        }
        
        // Validación de tiempo (QR válido por 90 segundos)
        if (diffSeconds > 90 || diffSeconds < -5) { // Tolerancia para reloj atrasado del cliente
            return registrarYResponder(usuario, false, "Pase expirado", "PASE_EXPIRADO");
        }
        
        // Validación de estado de cuenta y membresía
        if (!usuario.getActivo()) {
            return registrarYResponder(usuario, false, "Cuenta inactiva", "CUENTA_INACTIVA");
        }
        
        if (!usuario.getMembresiaActiva()) {
            return registrarYResponder(usuario, false, "Membresía inactiva", "MEMBRESIA_INACTIVA");
        }

        // Si pasa todas las validaciones
        return registrarYResponder(usuario, true, "Pase válido", "INGRESO_EXITOSO");
    }

    @Operation(summary = "Historial de asistencias paginado", description = "Retorna el historial paginado de asistencias del usuario autenticado con filtros opcionales de fecha.")
    @GetMapping("/historial")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<Page<AsistenciaResponse>> obtenerMisAsistencias(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            @PageableDefault(size = 10, sort = "fechaHoraAcceso", direction = Sort.Direction.DESC) Pageable pageable) {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(asistenciaService.obtenerHistorialPaginado(userDetails.getId(), desde, hasta, pageable));
    }

    @Operation(summary = "Estadísticas de asistencias", description = "Retorna las métricas y gráfico de concurrencia del usuario según el período.")
    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AsistenciasStatsResponse> obtenerMisEstadisticas(
            @RequestParam(defaultValue = "week") String periodo) {
        UserDetailsImpl userDetails =
                (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ResponseEntity.ok(asistenciaService.obtenerEstadisticas(userDetails.getId(), periodo));
    }

    private ResponseEntity<AccesoResponse> registrarYResponder(Usuario usuario, boolean permitido, String mensajeCorto, String observacion) {
        // Registrar la asistencia
        if (usuario != null) {
            Asistencia asistencia = Asistencia.builder()
                    .usuario(usuario)
                    .fechaHoraAcceso(LocalDateTime.now())
                    .accesoPermitido(permitido)
                    .observacion(observacion)
                    .build();
            asistenciaRepository.save(asistencia);
        }

        AccesoResponse response = AccesoResponse.builder()
                .permitido(permitido)
                .nombre(usuario != null ? usuario.getNombre() : "Desconocido")
                .planMembresia(usuario != null ? usuario.getPlanMembresia() : "-")
                .mensajeCorto(mensajeCorto)
                .build();

        return ResponseEntity.ok(response);
    }
}
