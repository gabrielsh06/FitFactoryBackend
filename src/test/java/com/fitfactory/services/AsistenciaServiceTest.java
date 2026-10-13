package com.fitfactory.services;

import com.fitfactory.dto.response.AsistenciaResponse;
import com.fitfactory.dto.response.AsistenciasStatsResponse;
import com.fitfactory.models.Asistencia;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.AsistenciaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class AsistenciaServiceTest {

    @Mock
    private AsistenciaRepository asistenciaRepository;

    @InjectMocks
    private AsistenciaService asistenciaService;

    private Usuario usuario;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .build();
    }

    @Test
    @DisplayName("Usuario sin asistencias devuelve lista vacía sin errores")
    void testUsuarioSinAsistencias() {
        when(asistenciaRepository.findByUsuarioIdOrderByFechaHoraAccesoDesc(1L))
                .thenReturn(List.of());

        List<AsistenciaResponse> result = asistenciaService.obtenerHistorial(1L, null, null);

        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("Usuario con múltiples asistencias mantiene orden descendente")
    void testUsuarioConMultiplesAsistenciasOrdenDescendente() {
        LocalDateTime t1 = LocalDateTime.now().minusHours(2);
        LocalDateTime t2 = LocalDateTime.now().minusDays(1);
        LocalDateTime t3 = LocalDateTime.now().minusDays(3);

        Asistencia a1 = Asistencia.builder().id(101L).usuario(usuario).fechaHoraAcceso(t1).accesoPermitido(true).observacion("INGRESO_EXITOSO").build();
        Asistencia a2 = Asistencia.builder().id(102L).usuario(usuario).fechaHoraAcceso(t2).accesoPermitido(true).observacion("INGRESO_EXITOSO").build();
        Asistencia a3 = Asistencia.builder().id(103L).usuario(usuario).fechaHoraAcceso(t3).accesoPermitido(false).observacion("PASE_EXPIRADO").build();

        when(asistenciaRepository.findByUsuarioIdOrderByFechaHoraAccesoDesc(1L))
                .thenReturn(List.of(a1, a2, a3));

        List<AsistenciaResponse> result = asistenciaService.obtenerHistorial(1L, null, null);

        assertEquals(3, result.size());
        assertEquals(101L, result.get(0).getId());
        assertEquals(t1, result.get(0).getFechaHoraAcceso());
        assertTrue(result.get(0).getAccesoPermitido());
        assertEquals(102L, result.get(1).getId());
        assertEquals(103L, result.get(2).getId());
        assertFalse(result.get(2).getAccesoPermitido());
    }

    @Test
    @DisplayName("Filtro por período delega a findByUsuarioIdAndPeriodo")
    void testFiltroPeriodo() {
        LocalDate desde = LocalDate.now().minusDays(7);
        LocalDate hasta = LocalDate.now();

        LocalDateTime t1 = LocalDateTime.now().minusDays(2);
        Asistencia a1 = Asistencia.builder().id(101L).usuario(usuario).fechaHoraAcceso(t1).accesoPermitido(true).observacion("INGRESO_EXITOSO").build();

        when(asistenciaRepository.findByUsuarioIdAndPeriodo(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(a1));

        List<AsistenciaResponse> result = asistenciaService.obtenerHistorial(1L, desde, hasta);

        assertEquals(1, result.size());
        assertEquals(101L, result.get(0).getId());
    }

    @Test
    @DisplayName("Aislamiento entre usuarios: solo se consultan registros del usuarioId especificado")
    void testAislamientoUsuarios() {
        when(asistenciaRepository.findByUsuarioIdOrderByFechaHoraAccesoDesc(1L))
                .thenReturn(List.of(Asistencia.builder().id(1L).usuario(usuario).fechaHoraAcceso(LocalDateTime.now()).accesoPermitido(true).build()));

        when(asistenciaRepository.findByUsuarioIdOrderByFechaHoraAccesoDesc(2L))
                .thenReturn(List.of());

        List<AsistenciaResponse> user1List = asistenciaService.obtenerHistorial(1L, null, null);
        List<AsistenciaResponse> user2List = asistenciaService.obtenerHistorial(2L, null, null);

        assertEquals(1, user1List.size());
        assertTrue(user2List.isEmpty());
    }

    @Test
    @DisplayName("Cálculo de estadísticas semanales y distribución por día")
    void testEstadisticasSemanales() {
        LocalDate hoy = LocalDate.now();
        LocalDate lunes = hoy.with(DayOfWeek.MONDAY);

        Asistencia aLunes = Asistencia.builder().id(1L).usuario(usuario)
                .fechaHoraAcceso(lunes.atTime(8, 30))
                .accesoPermitido(true)
                .build();
        Asistencia aMiercoles = Asistencia.builder().id(2L).usuario(usuario)
                .fechaHoraAcceso(lunes.plusDays(2).atTime(18, 0))
                .accesoPermitido(true)
                .build();
        Asistencia aViernes = Asistencia.builder().id(3L).usuario(usuario)
                .fechaHoraAcceso(lunes.plusDays(4).atTime(19, 0))
                .accesoPermitido(true)
                .build();

        List<Asistencia> listaSemana = List.of(aLunes, aMiercoles, aViernes);

        when(asistenciaRepository.findByUsuarioIdAndPeriodo(eq(1L), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(listaSemana);

        AsistenciasStatsResponse stats = asistenciaService.obtenerEstadisticas(1L, "week");

        assertNotNull(stats);
        assertEquals(3, stats.getTotalAsistencias());
        assertEquals(3, stats.getTotalPermitidos());
        assertEquals(0, stats.getTotalDenegados());
        assertNotNull(stats.getDistribucionSemanal());
        assertEquals(7, stats.getDistribucionSemanal().size());

        // 3 de 7 días asistidos = (3/7)*100 = 42.9%
        assertEquals(42.9, stats.getTasaAsistenciaSemanal(), 0.1);
    }
}
