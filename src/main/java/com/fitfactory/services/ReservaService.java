package com.fitfactory.services;

import com.fitfactory.dto.response.ReservaResponse;
import com.fitfactory.models.Clase;
import com.fitfactory.models.Reserva;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.ClaseRepository;
import com.fitfactory.repositories.ReservaRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservaService {

    @Autowired
    private ReservaRepository reservaRepository;
    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional
    public ReservaResponse reservarClase(Long claseId, Long usuarioId) {
        Usuario usuario = usuarioRepository.findById(usuarioId)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
                
        Clase clase = claseRepository.findById(claseId)
                .orElseThrow(() -> new IllegalArgumentException("Clase no encontrada"));

        // Validar si ya existe reserva confirmada
        if (reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(usuarioId, claseId, "CONFIRMADA")) {
            throw new IllegalArgumentException("Ya tienes una reserva confirmada para esta clase");
        }

        // Validar cupos
        if (clase.getCuposOcupados() >= clase.getCuposTotales()) {
            throw new IllegalArgumentException("La clase está llena, no hay cupos disponibles");
        }

        // Recuperar reserva anterior si existía (cancelada) o crear nueva
        Optional<Reserva> reservaExistente = reservaRepository.findByUsuarioIdAndClaseId(usuarioId, claseId);
        Reserva reserva;
        
        if (reservaExistente.isPresent()) {
            reserva = reservaExistente.get();
            reserva.setEstado("CONFIRMADA");
        } else {
            reserva = Reserva.builder()
                    .usuario(usuario)
                    .clase(clase)
                    .estado("CONFIRMADA")
                    .build();
        }

        // Incrementar cupos
        clase.setCuposOcupados(clase.getCuposOcupados() + 1);
        claseRepository.save(clase);
        
        reservaRepository.save(reserva);

        return mapToReservaResponse(reserva);
    }

    @Transactional
    public void cancelarReserva(Long reservaId, Long usuarioId) {
        Reserva reserva = reservaRepository.findById(reservaId)
                .orElseThrow(() -> new IllegalArgumentException("Reserva no encontrada"));

        if (!reserva.getUsuario().getId().equals(usuarioId)) {
            throw new IllegalArgumentException("Esta reserva no te pertenece");
        }

        if (!"CONFIRMADA".equals(reserva.getEstado())) {
            throw new IllegalArgumentException("La reserva ya está cancelada o no está confirmada");
        }

        // Cambiar estado y liberar cupo
        reserva.setEstado("CANCELADA");
        Clase clase = reserva.getClase();
        clase.setCuposOcupados(Math.max(0, clase.getCuposOcupados() - 1));
        
        claseRepository.save(clase);
        reservaRepository.save(reserva);
    }

    public List<ReservaResponse> obtenerMisReservas(Long usuarioId) {
        return reservaRepository.findByUsuarioId(usuarioId).stream()
                .map(this::mapToReservaResponse)
                .collect(Collectors.toList());
    }

    /**
     * Retorna la próxima clase confirmada del usuario (la más próxima en el tiempo).
     *
     * <p>Se filtra por reservas CONFIRMADA cuya clase aún no ha comenzado.
     * Si el usuario no tiene ninguna reserva futura confirmada se retorna
     * {@code Optional.empty()} y el controller responde HTTP 204.</p>
     *
     * @param usuarioId ID extraído del JWT.
     */
    @Transactional(readOnly = true)
    public Optional<ReservaResponse> obtenerProximaClase(Long usuarioId) {
        List<Reserva> proximas = reservaRepository
                .findProximasReservasConfirmadas(usuarioId, LocalDateTime.now());
        if (proximas.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(mapToReservaResponse(proximas.get(0)));
    }

    private ReservaResponse mapToReservaResponse(Reserva reserva) {
        return ReservaResponse.builder()
                .id(reserva.getId())
                .claseId(reserva.getClase().getId())
                .tituloClase(reserva.getClase().getTitulo())
                .categoria(reserva.getClase().getCategoria())
                .fechaHoraInicio(reserva.getClase().getFechaHoraInicio())
                .sala(reserva.getClase().getSala())
                .estado(reserva.getEstado())
                .fechaReserva(reserva.getFechaReserva())
                .build();
    }
}
