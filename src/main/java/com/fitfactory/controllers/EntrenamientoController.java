package com.fitfactory.controllers;

import com.fitfactory.dto.request.FinalizarSesionRequest;
import com.fitfactory.dto.request.IniciarSesionRequest;
import com.fitfactory.dto.request.RegistrarSerieRequest;
import com.fitfactory.dto.response.*;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.EntrenamientoService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/entrenamientos")
public class EntrenamientoController {

    @Autowired
    private EntrenamientoService entrenamientoService;

    @PostMapping("/iniciar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<SesionEntrenamientoResponse> iniciarSesion(@RequestBody(required = false) IniciarSesionRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        SesionEntrenamientoResponse sesion = entrenamientoService.iniciarSesion(userDetails.getId(), request);
        return ResponseEntity.status(HttpStatus.CREATED).body(sesion);
    }

    @GetMapping("/activa")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<?> obtenerSesionActiva() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Optional<SesionEntrenamientoResponse> sesion = entrenamientoService.obtenerSesionActiva(userDetails.getId());
        if (sesion.isEmpty()) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(sesion.get());
    }

    @PostMapping("/{sesionId}/series")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<RegistroSerieResponse> registrarSerie(
            @PathVariable Long sesionId,
            @Valid @RequestBody RegistrarSerieRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RegistroSerieResponse response = entrenamientoService.registrarSerie(userDetails.getId(), sesionId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @DeleteMapping("/{sesionId}/series/{serieId}")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<Void> eliminarSerie(
            @PathVariable Long sesionId,
            @PathVariable Long serieId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        entrenamientoService.eliminarSerie(userDetails.getId(), sesionId, serieId);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{sesionId}/finalizar")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<ResumenSesionResponse> finalizarSesion(
            @PathVariable Long sesionId,
            @RequestBody(required = false) FinalizarSesionRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ResumenSesionResponse resumen = entrenamientoService.finalizarSesion(userDetails.getId(), sesionId, request);
        return ResponseEntity.ok(resumen);
    }

    @GetMapping("/ejercicios/ultimo-historial")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<UltimoHistorialResponse> obtenerUltimoHistorial(
            @RequestParam(required = false) Long ejercicioId,
            @RequestParam String nombreEjercicio) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        UltimoHistorialResponse historial = entrenamientoService.obtenerUltimoHistorial(userDetails.getId(), ejercicioId, nombreEjercicio);
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/historial")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<SesionEntrenamientoResponse>> obtenerHistorialSesiones() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<SesionEntrenamientoResponse> historial = entrenamientoService.obtenerHistorialSesiones(userDetails.getId());
        return ResponseEntity.ok(historial);
    }

    @GetMapping("/ejercicios/evolucion")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<EvolucionEjercicioResponse> obtenerEvolucionEjercicio(
            @RequestParam(required = false) Long ejercicioId,
            @RequestParam String nombreEjercicio) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        EvolucionEjercicioResponse evolucion = entrenamientoService.obtenerEvolucionEjercicio(userDetails.getId(), ejercicioId, nombreEjercicio);
        return ResponseEntity.ok(evolucion);
    }

    /**
     * Estadísticas de la semana actual del usuario autenticado.
     *
     * <p>Devuelve métricas agregadas de los últimos 7 días (sesiones finalizadas,
     * series, volumen, récords personales y minutos).  El ID del cliente se extrae
     * exclusivamente del JWT, no se acepta como parámetro de ruta o query.</p>
     *
     * @return 200 con {@link StatsSemanalesResponse} (ceros si no hay datos).
     */
    @GetMapping("/stats/semana")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<StatsSemanalesResponse> obtenerStatsSemanales() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        StatsSemanalesResponse stats = entrenamientoService.obtenerStatsSemanales(userDetails.getId());
        return ResponseEntity.ok(stats);
    }
}
