package com.fitfactory.services;

import com.fitfactory.dto.request.FinalizarSesionRequest;
import com.fitfactory.dto.request.IniciarSesionRequest;
import com.fitfactory.dto.request.RegistrarSerieRequest;
import com.fitfactory.dto.response.*;
import com.fitfactory.models.*;
import com.fitfactory.repositories.EjercicioRepository;
import com.fitfactory.repositories.RegistroSerieRepository;
import com.fitfactory.repositories.RutinaRepository;
import com.fitfactory.repositories.SesionEntrenamientoRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class EntrenamientoService {

    @Autowired
    private SesionEntrenamientoRepository sesionRepository;

    @Autowired
    private RegistroSerieRepository registroSerieRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RutinaRepository rutinaRepository;

    @Autowired
    private EjercicioRepository ejercicioRepository;

    @Transactional
    public SesionEntrenamientoResponse iniciarSesion(Long clienteId, IniciarSesionRequest request) {
        // Si ya hay una sesión activa, la recuperamos para evitar duplicados accidentales
        Optional<SesionEntrenamiento> activa = sesionRepository
                .findFirstByClienteIdAndEstadoOrderByFechaInicioDesc(clienteId, EstadoSesion.EN_PROGRESO);
        if (activa.isPresent()) {
            return mapToResponse(activa.get());
        }

        Usuario cliente = usuarioRepository.findById(clienteId)
                .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado con ID: " + clienteId));

        Rutina rutina = null;
        if (request != null && request.getRutinaId() != null) {
            rutina = rutinaRepository.findById(request.getRutinaId()).orElse(null);
        }

        SesionEntrenamiento nuevaSesion = SesionEntrenamiento.builder()
                .cliente(cliente)
                .rutina(rutina)
                .fechaInicio(LocalDateTime.now())
                .estado(EstadoSesion.EN_PROGRESO)
                .volumenTotalKg(0.0)
                .totalSeries(0)
                .notas(request != null ? request.getNotas() : null)
                .build();

        SesionEntrenamiento guardada = sesionRepository.save(nuevaSesion);
        return mapToResponse(guardada);
    }

    @Transactional(readOnly = true)
    public Optional<SesionEntrenamientoResponse> obtenerSesionActiva(Long clienteId) {
        return sesionRepository
                .findFirstByClienteIdAndEstadoOrderByFechaInicioDesc(clienteId, EstadoSesion.EN_PROGRESO)
                .map(this::mapToResponse);
    }

    @Transactional
    public RegistroSerieResponse registrarSerie(Long clienteId, Long sesionId, RegistrarSerieRequest request) {
        SesionEntrenamiento sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada con ID: " + sesionId));

        if (!sesion.getCliente().getId().equals(clienteId)) {
            throw new SecurityException("No tienes permisos para modificar esta sesión de entrenamiento");
        }

        if (sesion.getEstado() != EstadoSesion.EN_PROGRESO) {
            throw new IllegalStateException("La sesión ya no está en progreso");
        }

        Ejercicio ejercicio = null;
        if (request.getEjercicioId() != null) {
            ejercicio = ejercicioRepository.findById(request.getEjercicioId()).orElse(null);
        }

        // Criterio determinista de Récord Personal (PR)
        boolean esPR = false;
        if (request.getCompletada() != null && request.getCompletada() &&
                (request.getTipoSerie() == TipoSerie.NORMAL || request.getTipoSerie() == TipoSerie.AL_FALLO)) {
            Double maxHistorico = registroSerieRepository.findMaxPesoHistorico(
                    clienteId, request.getEjercicioId(), request.getNombreEjercicio()
            );
            if ((maxHistorico == null && request.getPesoKg() > 0) ||
                    (maxHistorico != null && request.getPesoKg() > maxHistorico)) {
                esPR = true;
            }
        }

        RegistroSerie serie;
        if (request.getSerieId() != null) {
            serie = registroSerieRepository.findById(request.getSerieId())
                    .orElseThrow(() -> new IllegalArgumentException("Serie no encontrada con ID: " + request.getSerieId()));
            serie.setNumeroSerie(request.getNumeroSerie());
            serie.setTipoSerie(request.getTipoSerie() != null ? request.getTipoSerie() : TipoSerie.NORMAL);
            serie.setPesoKg(request.getPesoKg());
            serie.setRepeticiones(request.getRepeticiones());
            serie.setCompletada(request.getCompletada() != null ? request.getCompletada() : true);
            serie.setEsRecordPersonal(esPR);
            serie.setRpe(request.getRpe());
        } else {
            serie = RegistroSerie.builder()
                    .sesion(sesion)
                    .ejercicio(ejercicio)
                    .nombreEjercicio(request.getNombreEjercicio())
                    .numeroSerie(request.getNumeroSerie())
                    .tipoSerie(request.getTipoSerie() != null ? request.getTipoSerie() : TipoSerie.NORMAL)
                    .pesoKg(request.getPesoKg())
                    .repeticiones(request.getRepeticiones())
                    .completada(request.getCompletada() != null ? request.getCompletada() : true)
                    .esRecordPersonal(esPR)
                    .rpe(request.getRpe())
                    .fechaHora(LocalDateTime.now())
                    .build();
        }

        RegistroSerie guardada = registroSerieRepository.save(serie);

        // Recalcular métricas de la sesión
        recalcularMetricasSesion(sesion);

        return mapSerieToResponse(guardada);
    }

    @Transactional
    public void eliminarSerie(Long clienteId, Long sesionId, Long serieId) {
        SesionEntrenamiento sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada con ID: " + sesionId));

        if (!sesion.getCliente().getId().equals(clienteId)) {
            throw new SecurityException("No tienes permisos para modificar esta sesión");
        }

        RegistroSerie serie = registroSerieRepository.findById(serieId)
                .orElseThrow(() -> new IllegalArgumentException("Serie no encontrada"));

        if (!serie.getSesion().getId().equals(sesionId)) {
            throw new IllegalArgumentException("La serie no corresponde a la sesión indicada");
        }

        registroSerieRepository.delete(serie);
        recalcularMetricasSesion(sesion);
    }

    @Transactional
    public ResumenSesionResponse finalizarSesion(Long clienteId, Long sesionId, FinalizarSesionRequest request) {
        SesionEntrenamiento sesion = sesionRepository.findById(sesionId)
                .orElseThrow(() -> new IllegalArgumentException("Sesión no encontrada con ID: " + sesionId));

        if (!sesion.getCliente().getId().equals(clienteId)) {
            throw new SecurityException("No tienes permisos para finalizar esta sesión");
        }

        LocalDateTime fin = LocalDateTime.now();
        sesion.setFechaFin(fin);
        sesion.setEstado(EstadoSesion.FINALIZADA);

        if (request != null && request.getDuracionSegundos() != null && request.getDuracionSegundos() > 0) {
            sesion.setDuracionSegundos(request.getDuracionSegundos());
        } else {
            long segs = Duration.between(sesion.getFechaInicio(), fin).getSeconds();
            sesion.setDuracionSegundos((int) Math.max(segs, 1));
        }

        if (request != null && request.getNotas() != null && !request.getNotas().isBlank()) {
            sesion.setNotas(request.getNotas());
        }

        recalcularMetricasSesion(sesion);
        SesionEntrenamiento actualizada = sesionRepository.save(sesion);

        // Generar resumen
        List<RegistroSerie> series = registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(sesionId);
        Set<String> ejerciciosUnicos = series.stream()
                .map(RegistroSerie::getNombreEjercicio)
                .collect(Collectors.toSet());

        List<String> detallesPR = new ArrayList<>();
        int recordsCount = 0;
        for (RegistroSerie s : series) {
            if (Boolean.TRUE.equals(s.getEsRecordPersonal())) {
                recordsCount++;
                detallesPR.add(s.getNombreEjercicio() + ": " + s.getPesoKg() + " kg (" + s.getRepeticiones() + " reps)");
            }
        }

        return ResumenSesionResponse.builder()
                .sesionId(actualizada.getId())
                .fechaInicio(actualizada.getFechaInicio())
                .fechaFin(actualizada.getFechaFin())
                .duracionSegundos(actualizada.getDuracionSegundos())
                .totalEjercicios(ejerciciosUnicos.size())
                .totalSeries(actualizada.getTotalSeries())
                .volumenTotalKg(actualizada.getVolumenTotalKg())
                .recordsPersonales(recordsCount)
                .detallesRecords(detallesPR)
                .notas(actualizada.getNotas())
                .build();
    }

    @Transactional(readOnly = true)
    public UltimoHistorialResponse obtenerUltimoHistorial(Long clienteId, Long ejercicioId, String nombreEjercicio) {
        List<RegistroSerie> historial = registroSerieRepository.findHistorialEjercicio(
                clienteId, ejercicioId, nombreEjercicio, PageRequest.of(0, 1)
        );

        Double maxHistorico = registroSerieRepository.findMaxPesoHistorico(clienteId, ejercicioId, nombreEjercicio);

        if (historial.isEmpty()) {
            return UltimoHistorialResponse.builder()
                    .ejercicioId(ejercicioId)
                    .nombreEjercicio(nombreEjercicio)
                    .ultimoPesoKg(null)
                    .ultimasRepeticiones(null)
                    .ultimaFecha(null)
                    .recordPersonalPesoKg(maxHistorico)
                    .recordPersonalReps(null)
                    .build();
        }

        RegistroSerie ultimo = historial.get(0);
        return UltimoHistorialResponse.builder()
                .ejercicioId(ejercicioId)
                .nombreEjercicio(nombreEjercicio)
                .ultimoPesoKg(ultimo.getPesoKg())
                .ultimasRepeticiones(ultimo.getRepeticiones())
                .ultimaFecha(ultimo.getFechaHora())
                .recordPersonalPesoKg(maxHistorico != null ? maxHistorico : ultimo.getPesoKg())
                .recordPersonalReps(ultimo.getRepeticiones())
                .build();
    }

    @Transactional(readOnly = true)
    public List<SesionEntrenamientoResponse> obtenerHistorialSesiones(Long clienteId) {
        List<SesionEntrenamiento> sesiones = sesionRepository
                .findByClienteIdAndEstadoOrderByFechaInicioDesc(clienteId, EstadoSesion.FINALIZADA);

        return sesiones.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Calcula las estadísticas de la "semana actual" para el usuario autenticado.
     *
     * <p>Definición de semana: los últimos 7 días completos, desde las 00:00 del
     * día de hace 6 días hasta las 23:59:59 de hoy.  Se usan los campos
     * pre-calculados {@code totalSeries} y {@code volumenTotalKg} de cada sesión
     * (actualizados en {@link #recalcularMetricasSesion}) para evitar consultas N+1.
     * Los PRs se cuentan con una query de agregación sobre {@code RegistroSerie}.</p>
     *
     * <p>Sesiones {@code CANCELADA} y {@code EN_PROGRESO} se excluyen implícitamente
     * porque la query filtra sólo {@code FINALIZADA}.</p>
     *
     * @param clienteId ID extraído del JWT; nunca aceptado como parámetro externo.
     * @return {@link StatsSemanalesResponse} con ceros si no hay datos.
     */
    @Transactional(readOnly = true)
    public StatsSemanalesResponse obtenerStatsSemanales(Long clienteId) {
        LocalDateTime hasta = LocalDateTime.now().toLocalDate().atTime(23, 59, 59);
        LocalDateTime desde = hasta.toLocalDate().minusDays(6).atStartOfDay();

        List<SesionEntrenamiento> sesiones = sesionRepository
                .findSemanalesFinalizadas(clienteId, desde, hasta);

        if (sesiones.isEmpty()) {
            return StatsSemanalesResponse.builder()
                    .sesionesFinalizadas(0)
                    .totalSeries(0)
                    .volumenTotalKg(0.0)
                    .recordsPersonales(0)
                    .minutosTotales(0)
                    .build();
        }

        int sesionesCount = sesiones.size();
        int totalSeries = sesiones.stream().mapToInt(s -> s.getTotalSeries() != null ? s.getTotalSeries() : 0).sum();
        double volumen = sesiones.stream().mapToDouble(s -> s.getVolumenTotalKg() != null ? s.getVolumenTotalKg() : 0.0).sum();
        int minutos = sesiones.stream()
                .mapToInt(s -> s.getDuracionSegundos() != null ? s.getDuracionSegundos() : 0)
                .sum() / 60;

        List<Long> sesionIds = sesiones.stream().map(SesionEntrenamiento::getId).collect(Collectors.toList());
        long prs = registroSerieRepository.countRecordsPersonalesBySesionIds(sesionIds);

        return StatsSemanalesResponse.builder()
                .sesionesFinalizadas(sesionesCount)
                .totalSeries(totalSeries)
                .volumenTotalKg(Math.round(volumen * 10.0) / 10.0)
                .recordsPersonales((int) prs)
                .minutosTotales(minutos)
                .build();
    }

    @Transactional(readOnly = true)
    public EvolucionEjercicioResponse obtenerEvolucionEjercicio(Long clienteId, Long ejercicioId, String nombreEjercicio) {
        List<RegistroSerie> series = registroSerieRepository
                .findEvolucionEjercicio(clienteId, ejercicioId, nombreEjercicio);

        Double maxHistorico = registroSerieRepository
                .findMaxPesoHistorico(clienteId, ejercicioId, nombreEjercicio);

        List<EvolucionSerieDTO> seriesDTO = series.stream().map(s -> EvolucionSerieDTO.builder()
                .id(s.getId())
                .sesionId(s.getSesion().getId())
                .numeroSerie(s.getNumeroSerie())
                .tipoSerie(s.getTipoSerie())
                .pesoKg(s.getPesoKg())
                .repeticiones(s.getRepeticiones())
                .completada(s.getCompletada())
                .esRecordPersonal(s.getEsRecordPersonal())
                .rpe(s.getRpe())
                .fechaHora(s.getFechaHora())
                .build()
        ).collect(Collectors.toList());

        return EvolucionEjercicioResponse.builder()
                .ejercicioId(ejercicioId)
                .nombreEjercicio(nombreEjercicio)
                .maxPesoHistorico(maxHistorico)
                .totalSeriesCompletadas(seriesDTO.size())
                .series(seriesDTO)
                .build();
    }

    private void recalcularMetricasSesion(SesionEntrenamiento sesion) {
        List<RegistroSerie> series = registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(sesion.getId());
        double volumenTotal = 0.0;
        int seriesCompletadas = 0;

        for (RegistroSerie s : series) {
            if (Boolean.TRUE.equals(s.getCompletada())) {
                seriesCompletadas++;
                if (s.getPesoKg() != null && s.getRepeticiones() != null) {
                    volumenTotal += (s.getPesoKg() * s.getRepeticiones());
                }
            }
        }

        sesion.setTotalSeries(seriesCompletadas);
        sesion.setVolumenTotalKg(Math.round(volumenTotal * 10.0) / 10.0);
        sesionRepository.save(sesion);
    }

    private SesionEntrenamientoResponse mapToResponse(SesionEntrenamiento sesion) {
        List<RegistroSerie> series = registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(sesion.getId());
        List<RegistroSerieResponse> seriesResp = series.stream()
                .map(this::mapSerieToResponse)
                .collect(Collectors.toList());

        return SesionEntrenamientoResponse.builder()
                .id(sesion.getId())
                .clienteId(sesion.getCliente().getId())
                .rutinaId(sesion.getRutina() != null ? sesion.getRutina().getId() : null)
                .rutinaTitulo(sesion.getRutina() != null ? sesion.getRutina().getTitulo() : "Entrenamiento Libre")
                .fechaInicio(sesion.getFechaInicio())
                .fechaFin(sesion.getFechaFin())
                .duracionSegundos(sesion.getDuracionSegundos())
                .estado(sesion.getEstado())
                .volumenTotalKg(sesion.getVolumenTotalKg())
                .totalSeries(sesion.getTotalSeries())
                .notas(sesion.getNotas())
                .series(seriesResp)
                .build();
    }

    private RegistroSerieResponse mapSerieToResponse(RegistroSerie serie) {
        return RegistroSerieResponse.builder()
                .id(serie.getId())
                .ejercicioId(serie.getEjercicio() != null ? serie.getEjercicio().getId() : null)
                .nombreEjercicio(serie.getNombreEjercicio())
                .numeroSerie(serie.getNumeroSerie())
                .tipoSerie(serie.getTipoSerie())
                .pesoKg(serie.getPesoKg())
                .repeticiones(serie.getRepeticiones())
                .completada(serie.getCompletada())
                .esRecordPersonal(serie.getEsRecordPersonal())
                .rpe(serie.getRpe())
                .fechaHora(serie.getFechaHora())
                .build();
    }
}
