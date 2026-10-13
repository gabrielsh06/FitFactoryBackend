package com.fitfactory.services;

import com.fitfactory.dto.request.ClaseRequest;
import com.fitfactory.dto.response.ClaseResponse;
import com.fitfactory.models.Clase;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.ClaseRepository;
import com.fitfactory.repositories.ReservaRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ClaseService {

    @Autowired
    private ClaseRepository claseRepository;
    @Autowired
    private UsuarioRepository usuarioRepository;
    @Autowired
    private ReservaRepository reservaRepository;

    public ClaseResponse crearClase(ClaseRequest request, Long entrenadorId) {
        Usuario entrenador = usuarioRepository.findById(entrenadorId)
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));

        if (request.getFechaHoraInicio().isAfter(request.getFechaHoraFin())) {
            throw new IllegalArgumentException("La fecha de inicio debe ser anterior a la fecha de fin");
        }

        Clase clase = Clase.builder()
                .titulo(request.getTitulo())
                .categoria(request.getCategoria())
                .fechaHoraInicio(request.getFechaHoraInicio())
                .fechaHoraFin(request.getFechaHoraFin())
                .sala(request.getSala())
                .cuposTotales(request.getCuposTotales())
                .cuposOcupados(0)
                .entrenador(entrenador)
                .build();

        claseRepository.save(clase);
        return mapToClaseResponse(clase, false);
    }

    public List<ClaseResponse> obtenerClasesSemana(LocalDateTime fechaInicio, LocalDateTime fechaFin, Long usuarioId) {
        List<Clase> clases = claseRepository.findClasesDisponibles(fechaInicio != null ? fechaInicio : LocalDateTime.now());

        return clases.stream().map(clase -> {
            boolean reservadaPorMi = false;
            if (usuarioId != null) {
                reservadaPorMi = reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(usuarioId, clase.getId(), "CONFIRMADA");
            }
            return mapToClaseResponse(clase, reservadaPorMi);
        }).collect(Collectors.toList());
    }

    public Page<ClaseResponse> obtenerClasesPaginadas(LocalDateTime fechaInicio, Long usuarioId, Pageable pageable) {
        Page<Clase> clases = claseRepository.findClasesDisponibles(
                fechaInicio != null ? fechaInicio : LocalDateTime.now(), pageable);

        return clases.map(clase -> {
            boolean reservadaPorMi = false;
            if (usuarioId != null) {
                reservadaPorMi = reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(usuarioId, clase.getId(), "CONFIRMADA");
            }
            return mapToClaseResponse(clase, reservadaPorMi);
        });
    }

    private ClaseResponse mapToClaseResponse(Clase clase, boolean reservadaPorMi) {
        return ClaseResponse.builder()
                .id(clase.getId())
                .titulo(clase.getTitulo())
                .categoria(clase.getCategoria())
                .fechaHoraInicio(clase.getFechaHoraInicio())
                .fechaHoraFin(clase.getFechaHoraFin())
                .sala(clase.getSala())
                .cuposTotales(clase.getCuposTotales())
                .cuposOcupados(clase.getCuposOcupados())
                .cuposDisponibles(clase.getCuposTotales() - clase.getCuposOcupados())
                .nombreEntrenador(clase.getEntrenador().getNombre())
                .reservadaPorMi(reservadaPorMi)
                .build();
    }
}
