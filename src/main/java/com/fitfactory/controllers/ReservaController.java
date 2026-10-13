package com.fitfactory.controllers;

import com.fitfactory.dto.response.MessageResponse;
import com.fitfactory.dto.response.ReservaResponse;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.ReservaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "Reservas", description = "Gestión de reservas de clases para clientes")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/reservas")
@PreAuthorize("hasRole('CLIENTE')")
public class ReservaController {

    @Autowired
    private ReservaService reservaService;

    @Operation(summary = "Mis reservas", description = "Lista el historial de reservas de clases del cliente autenticado.")
    @GetMapping("/mis-reservas")
    public ResponseEntity<List<ReservaResponse>> obtenerMisReservas() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<ReservaResponse> reservas = reservaService.obtenerMisReservas(userDetails.getId());
        return ResponseEntity.ok(reservas);
    }

    @Operation(summary = "Reservar clase", description = "Reserva un cupo en la clase indicada si hay cupos disponibles.")
    @PostMapping("/{claseId}")
    public ResponseEntity<ReservaResponse> reservarClase(@PathVariable Long claseId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ReservaResponse reserva = reservaService.reservarClase(claseId, userDetails.getId());
        return ResponseEntity.ok(reserva);
    }

    @Operation(summary = "Cancelar reserva", description = "Cancela la reserva y libera el cupo de la clase.")
    @DeleteMapping("/{reservaId}")
    public ResponseEntity<MessageResponse> cancelarReserva(@PathVariable Long reservaId) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        reservaService.cancelarReserva(reservaId, userDetails.getId());
        return ResponseEntity.ok(MessageResponse.ok("Reserva cancelada exitosamente"));
    }

    /**
     * Retorna la próxima clase reservada y confirmada del usuario autenticado.
     * Responde HTTP 204 (sin cuerpo) si no hay clases futuras reservadas.
     */
    @Operation(summary = "Obtener próxima clase", description = "Retorna la clase confirmada más próxima del usuario o 204 No Content.")
    @GetMapping("/proxima")
    public ResponseEntity<?> obtenerProximaClase() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return reservaService.obtenerProximaClase(userDetails.getId())
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.noContent().build());
    }
}
