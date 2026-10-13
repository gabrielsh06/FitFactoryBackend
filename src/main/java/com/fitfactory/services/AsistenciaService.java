package com.fitfactory.services;

import com.fitfactory.dto.response.AsistenciaResponse;
import com.fitfactory.dto.response.AsistenciasStatsResponse;
import com.fitfactory.models.Asistencia;
import com.fitfactory.repositories.AsistenciaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AsistenciaService {

    @Autowired
    private AsistenciaRepository asistenciaRepository;

    /**
     * Consulta el historial completo o acotado de asistencias del usuario autenticado,
     * ordenado descendentemente por fecha y hora (más reciente a más antiguo).
     */
    @Transactional(readOnly = true)
    public List<AsistenciaResponse> obtenerHistorial(Long usuarioId, LocalDate desde, LocalDate hasta) {
        List<Asistencia> asistencias;
        if (desde != null && hasta != null) {
            LocalDateTime desdeDt = desde.atStartOfDay();
            LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);
            asistencias = asistenciaRepository.findByUsuarioIdAndPeriodo(usuarioId, desdeDt, hastaDt);
        } else {
            asistencias = asistenciaRepository.findByUsuarioIdOrderByFechaHoraAccesoDesc(usuarioId);
        }

        return asistencias.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Consulta el historial paginado de asistencias del usuario autenticado.
     */
    @Transactional(readOnly = true)
    public Page<AsistenciaResponse> obtenerHistorialPaginado(Long usuarioId, LocalDate desde, LocalDate hasta, Pageable pageable) {
        Page<Asistencia> asistencias;
        if (desde != null && hasta != null) {
            LocalDateTime desdeDt = desde.atStartOfDay();
            LocalDateTime hastaDt = hasta.atTime(LocalTime.MAX);
            asistencias = asistenciaRepository.findByUsuarioIdAndPeriodo(usuarioId, desdeDt, hastaDt, pageable);
        } else {
            asistencias = asistenciaRepository.findByUsuarioId(usuarioId, pageable);
        }

        return asistencias.map(this::mapToResponse);
    }

    /**
     * Calcula estadísticas agregadas de asistencia para el período seleccionado:
     * - "day": el día de hoy
     * - "week": la semana en curso (Lunes a Domingo)
     * - "month": el mes en curso (primer al último día del mes)
     */
    @Transactional(readOnly = true)
    public AsistenciasStatsResponse obtenerEstadisticas(Long usuarioId, String periodo) {
        LocalDate hoy = LocalDate.now();
        LocalDateTime inicioPeriodo;
        LocalDateTime finPeriodo = hoy.atTime(LocalTime.MAX);

        String per = (periodo != null) ? periodo.toLowerCase() : "week";

        LocalDate inicioSemana = hoy.with(DayOfWeek.MONDAY);
        LocalDate finSemana = hoy.with(DayOfWeek.SUNDAY);

        if ("day".equals(per)) {
            inicioPeriodo = hoy.atStartOfDay();
        } else if ("month".equals(per)) {
            inicioPeriodo = hoy.withDayOfMonth(1).atStartOfDay();
        } else { // "week" por defecto
            inicioPeriodo = inicioSemana.atStartOfDay();
            finPeriodo = finSemana.atTime(LocalTime.MAX);
        }

        List<Asistencia> registros = asistenciaRepository.findByUsuarioIdAndPeriodo(usuarioId, inicioPeriodo, finPeriodo);

        long permitidos = registros.stream().filter(r -> Boolean.TRUE.equals(r.getAccesoPermitido())).count();
        long denegados = registros.size() - permitidos;

        // Distribución semanal fija para los gráficos (Lunes a Domingo)
        LocalDateTime inicioSemanaDt = inicioSemana.atStartOfDay();
        LocalDateTime finSemanaDt = finSemana.atTime(LocalTime.MAX);
        List<Asistencia> registrosSemana = asistenciaRepository.findByUsuarioIdAndPeriodo(usuarioId, inicioSemanaDt, finSemanaDt);

        List<AsistenciasStatsResponse.DiaAsistenciaDTO> distribucion = new ArrayList<>();
        String[] labels = {"Mon", "Tue", "Wed", "Thurs", "Fri", "Sat", "Sun"};
        DayOfWeek[] days = {DayOfWeek.MONDAY, DayOfWeek.TUESDAY, DayOfWeek.WEDNESDAY, DayOfWeek.THURSDAY, DayOfWeek.FRIDAY, DayOfWeek.SATURDAY, DayOfWeek.SUNDAY};

        int diasConAsistencia = 0;
        DateTimeFormatter isoFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        for (int i = 0; i < 7; i++) {
            LocalDate diaFecha = inicioSemana.with(days[i]);
            long accesosPermitidosDia = registrosSemana.stream()
                    .filter(r -> Boolean.TRUE.equals(r.getAccesoPermitido()))
                    .filter(r -> r.getFechaHoraAcceso().toLocalDate().equals(diaFecha))
                    .count();

            if (accesosPermitidosDia > 0) {
                diasConAsistencia++;
            }

            distribucion.add(AsistenciasStatsResponse.DiaAsistenciaDTO.builder()
                    .dia(labels[i])
                    .fecha(diaFecha.format(isoFormat))
                    .asistencias(accesosPermitidosDia)
                    .build());
        }

        // Tasa de cumplimiento semanal: (dias asistidos / 7) * 100
        double tasaCumplimiento = Math.round(((double) diasConAsistencia / 7.0) * 1000.0) / 10.0;

        return AsistenciasStatsResponse.builder()
                .totalAsistencias(registros.size())
                .totalPermitidos(permitidos)
                .totalDenegados(denegados)
                .distribucionSemanal(distribucion)
                .tasaAsistenciaSemanal(tasaCumplimiento)
                .build();
    }

    /**
     * Consulta administrativa global de métricas de accesos agregadas para un rango o fecha.
     */
    @Transactional(readOnly = true)
    public com.fitfactory.dto.response.AdminAccesosStatsResponse obtenerStatsAccesosGlobales(LocalDate fecha) {
        LocalDate objetivo = (fecha != null) ? fecha : LocalDate.now();
        LocalDateTime inicioDia = objetivo.atStartOfDay();
        LocalDateTime finDia = objetivo.atTime(LocalTime.MAX);

        long total = asistenciaRepository.countTotalGlobalByPeriodo(inicioDia, finDia);
        long permitidos = asistenciaRepository.countGlobalByAccesoPermitidoAndPeriodo(true, inicioDia, finDia);
        long rechazados = total - permitidos;

        return com.fitfactory.dto.response.AdminAccesosStatsResponse.builder()
                .fecha(objetivo)
                .totalAccesos(total)
                .accesosPermitidos(permitidos)
                .accesosRechazados(rechazados)
                .build();
    }

    private AsistenciaResponse mapToResponse(Asistencia a) {
        return AsistenciaResponse.builder()
                .id(a.getId())
                .fechaHoraAcceso(a.getFechaHoraAcceso())
                .accesoPermitido(a.getAccesoPermitido())
                .observacion(a.getObservacion())
                .build();
    }
}
