package com.fitfactory.services;

import com.fitfactory.dto.request.CrearRutinaRequest;
import com.fitfactory.dto.response.EjercicioResponse;
import com.fitfactory.dto.response.RutinaResponse;
import com.fitfactory.models.Ejercicio;
import com.fitfactory.models.Rutina;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.RutinaRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class RutinaService {

    @Autowired
    private RutinaRepository rutinaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Transactional(readOnly = true)
    public List<RutinaResponse> obtenerMiRutina(Long clienteId) {
        return rutinaRepository.findByClienteId(clienteId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public RutinaResponse asignarRutina(CrearRutinaRequest request, Long entrenadorId) {
        Usuario cliente = usuarioRepository.findById(request.getClienteId())
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));

        Usuario entrenador = usuarioRepository.findById(entrenadorId)
                .orElseThrow(() -> new IllegalArgumentException("Entrenador no encontrado"));

        Rutina rutina = Rutina.builder()
                .titulo(request.getTitulo())
                .objetivo(request.getObjetivo())
                .diaSemana(request.getDiaSemana())
                .cliente(cliente)
                .entrenador(entrenador)
                .build();

        List<Ejercicio> ejercicios = request.getEjercicios().stream().map(req -> Ejercicio.builder()
                .nombre(req.getNombre())
                .grupoMuscular(req.getGrupoMuscular())
                .series(req.getSeries())
                .repeticiones(req.getRepeticiones())
                .descansoSegundos(req.getDescansoSegundos())
                .instrucciones(req.getInstrucciones())
                .rutina(rutina)
                .build()).collect(Collectors.toList());

        rutina.setEjercicios(ejercicios);
        
        Rutina rutinaGuardada = rutinaRepository.save(rutina);
        return mapToResponse(rutinaGuardada);
    }

    private RutinaResponse mapToResponse(Rutina rutina) {
        List<EjercicioResponse> ejerciciosResponse = rutina.getEjercicios().stream().map(e -> EjercicioResponse.builder()
                .id(e.getId())
                .nombre(e.getNombre())
                .grupoMuscular(e.getGrupoMuscular())
                .series(e.getSeries())
                .repeticiones(e.getRepeticiones())
                .descansoSegundos(e.getDescansoSegundos())
                .instrucciones(e.getInstrucciones())
                .build()).collect(Collectors.toList());

        return RutinaResponse.builder()
                .id(rutina.getId())
                .titulo(rutina.getTitulo())
                .objetivo(rutina.getObjetivo())
                .diaSemana(rutina.getDiaSemana())
                .entrenadorId(rutina.getEntrenador().getId())
                .nombreEntrenador(rutina.getEntrenador().getNombre())
                .ejercicios(ejerciciosResponse)
                .build();
    }
}
