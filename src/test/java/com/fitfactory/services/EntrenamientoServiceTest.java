package com.fitfactory.services;

import com.fitfactory.dto.request.FinalizarSesionRequest;
import com.fitfactory.dto.request.IniciarSesionRequest;
import com.fitfactory.dto.request.RegistrarSerieRequest;
import com.fitfactory.dto.response.RegistroSerieResponse;
import com.fitfactory.dto.response.ResumenSesionResponse;
import com.fitfactory.dto.response.SesionEntrenamientoResponse;
import com.fitfactory.models.*;
import com.fitfactory.repositories.EjercicioRepository;
import com.fitfactory.repositories.RegistroSerieRepository;
import com.fitfactory.repositories.RutinaRepository;
import com.fitfactory.repositories.SesionEntrenamientoRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntrenamientoServiceTest {

    @Mock
    private SesionEntrenamientoRepository sesionRepository;

    @Mock
    private RegistroSerieRepository registroSerieRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private RutinaRepository rutinaRepository;

    @Mock
    private EjercicioRepository ejercicioRepository;

    @InjectMocks
    private EntrenamientoService entrenamientoService;

    private Usuario clienteMock;
    private SesionEntrenamiento sesionMock;

    @BeforeEach
    void setUp() {
        clienteMock = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@test.com")
                .build();

        sesionMock = SesionEntrenamiento.builder()
                .id(100L)
                .cliente(clienteMock)
                .fechaInicio(LocalDateTime.now().minusMinutes(30))
                .estado(EstadoSesion.EN_PROGRESO)
                .volumenTotalKg(0.0)
                .totalSeries(0)
                .registros(new ArrayList<>())
                .build();
    }

    @Test
    @DisplayName("Iniciar sesión: si ya existe una activa, la recupera sin duplicar")
    void testIniciarSesionRecuperaExistente() {
        when(sesionRepository.findFirstByClienteIdAndEstadoOrderByFechaInicioDesc(1L, EstadoSesion.EN_PROGRESO))
                .thenReturn(Optional.of(sesionMock));

        SesionEntrenamientoResponse response = entrenamientoService.iniciarSesion(1L, new IniciarSesionRequest());

        assertNotNull(response);
        assertEquals(100L, response.getId());
        assertEquals(EstadoSesion.EN_PROGRESO, response.getEstado());
        verify(sesionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Registrar serie: calcula volumen acumulado y detecta PR en serie normal")
    void testRegistrarSerieDetectaPRYVolumen() {
        when(sesionRepository.findById(100L)).thenReturn(Optional.of(sesionMock));
        when(registroSerieRepository.findMaxPesoHistorico(1L, 10L, "Press Banca")).thenReturn(60.0);

        RegistroSerie serieGuardada = RegistroSerie.builder()
                .id(500L)
                .sesion(sesionMock)
                .nombreEjercicio("Press Banca")
                .numeroSerie(1)
                .tipoSerie(TipoSerie.NORMAL)
                .pesoKg(70.0)
                .repeticiones(10)
                .completada(true)
                .esRecordPersonal(true)
                .fechaHora(LocalDateTime.now())
                .build();

        when(registroSerieRepository.save(any(RegistroSerie.class))).thenReturn(serieGuardada);
        when(registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(100L))
                .thenReturn(List.of(serieGuardada));

        RegistrarSerieRequest request = new RegistrarSerieRequest();
        request.setEjercicioId(10L);
        request.setNombreEjercicio("Press Banca");
        request.setNumeroSerie(1);
        request.setTipoSerie(TipoSerie.NORMAL);
        request.setPesoKg(70.0);
        request.setRepeticiones(10);
        request.setCompletada(true);

        RegistroSerieResponse response = entrenamientoService.registrarSerie(1L, 100L, request);

        assertNotNull(response);
        assertTrue(response.getEsRecordPersonal());
        assertEquals(70.0, response.getPesoKg());
        verify(sesionRepository, atLeastOnce()).save(sesionMock);
        assertEquals(700.0, sesionMock.getVolumenTotalKg());
        assertEquals(1, sesionMock.getTotalSeries());
    }

    @Test
    @DisplayName("Registrar serie: calentamiento NO debe marcarse como PR")
    void testCalentamientoNoEsPR() {
        when(sesionRepository.findById(100L)).thenReturn(Optional.of(sesionMock));

        RegistroSerie serieCalentamiento = RegistroSerie.builder()
                .id(501L)
                .sesion(sesionMock)
                .nombreEjercicio("Sentadilla")
                .numeroSerie(1)
                .tipoSerie(TipoSerie.CALENTAMIENTO)
                .pesoKg(100.0)
                .repeticiones(10)
                .completada(true)
                .esRecordPersonal(false)
                .fechaHora(LocalDateTime.now())
                .build();

        when(registroSerieRepository.save(any(RegistroSerie.class))).thenReturn(serieCalentamiento);
        when(registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(100L))
                .thenReturn(List.of(serieCalentamiento));

        RegistrarSerieRequest request = new RegistrarSerieRequest();
        request.setNombreEjercicio("Sentadilla");
        request.setNumeroSerie(1);
        request.setTipoSerie(TipoSerie.CALENTAMIENTO);
        request.setPesoKg(100.0);
        request.setRepeticiones(10);
        request.setCompletada(true);

        RegistroSerieResponse response = entrenamientoService.registrarSerie(1L, 100L, request);

        assertNotNull(response);
        assertFalse(response.getEsRecordPersonal());
    }

    @Test
    @DisplayName("Seguridad: Usuario no puede modificar sesión de otro cliente")
    void testSeguridadOtroUsuarioLanzaException() {
        when(sesionRepository.findById(100L)).thenReturn(Optional.of(sesionMock));

        RegistrarSerieRequest request = new RegistrarSerieRequest();
        request.setNombreEjercicio("Press Militar");
        request.setNumeroSerie(1);
        request.setPesoKg(40.0);
        request.setRepeticiones(10);

        assertThrows(SecurityException.class, () -> {
            // Cliente 999 intenta modificar la sesión del cliente 1
            entrenamientoService.registrarSerie(999L, 100L, request);
        });
    }

    @Test
    @DisplayName("Finalizar sesión: cambia a estado FINALIZADA y resume métricas")
    void testFinalizarSesion() {
        when(sesionRepository.findById(100L)).thenReturn(Optional.of(sesionMock));
        when(sesionRepository.save(any(SesionEntrenamiento.class))).thenAnswer(i -> i.getArgument(0));

        RegistroSerie seriePR = RegistroSerie.builder()
                .id(502L)
                .nombreEjercicio("Peso Muerto")
                .numeroSerie(1)
                .tipoSerie(TipoSerie.NORMAL)
                .pesoKg(140.0)
                .repeticiones(5)
                .completada(true)
                .esRecordPersonal(true)
                .build();

        when(registroSerieRepository.findBySesionIdOrderByNumeroSerieAsc(100L))
                .thenReturn(List.of(seriePR));

        FinalizarSesionRequest finReq = new FinalizarSesionRequest();
        finReq.setDuracionSegundos(1800);
        finReq.setNotas("Excelente día de fuerza");

        ResumenSesionResponse resumen = entrenamientoService.finalizarSesion(1L, 100L, finReq);

        assertNotNull(resumen);
        assertEquals(100L, resumen.getSesionId());
        assertEquals(1800, resumen.getDuracionSegundos());
        assertEquals(1, resumen.getRecordsPersonales());
        assertEquals(1, resumen.getTotalEjercicios());
        assertEquals(EstadoSesion.FINALIZADA, sesionMock.getEstado());
    }

    @Test
    @DisplayName("Historial de sesiones: recupera sesiones finalizadas del usuario ordenadas")
    void testObtenerHistorialSesiones() {
        sesionMock.setEstado(EstadoSesion.FINALIZADA);
        when(sesionRepository.findByClienteIdAndEstadoOrderByFechaInicioDesc(1L, EstadoSesion.FINALIZADA))
                .thenReturn(List.of(sesionMock));

        List<SesionEntrenamientoResponse> historial = entrenamientoService.obtenerHistorialSesiones(1L);

        assertNotNull(historial);
        assertEquals(1, historial.size());
        assertEquals(EstadoSesion.FINALIZADA, historial.get(0).getEstado());
    }

    @Test
    @DisplayName("Evolución de ejercicio: consulta series cronológicas y PR")
    void testObtenerEvolucionEjercicio() {
        RegistroSerie serie1 = RegistroSerie.builder()
                .id(1L)
                .sesion(sesionMock)
                .nombreEjercicio("Dominadas")
                .numeroSerie(1)
                .pesoKg(0.0)
                .repeticiones(12)
                .completada(true)
                .fechaHora(LocalDateTime.now().minusDays(3))
                .build();

        RegistroSerie serie2 = RegistroSerie.builder()
                .id(2L)
                .sesion(sesionMock)
                .nombreEjercicio("Dominadas")
                .numeroSerie(2)
                .pesoKg(10.0)
                .repeticiones(8)
                .completada(true)
                .esRecordPersonal(true)
                .fechaHora(LocalDateTime.now().minusDays(1))
                .build();

        when(registroSerieRepository.findEvolucionEjercicio(1L, 5L, "Dominadas"))
                .thenReturn(List.of(serie1, serie2));
        when(registroSerieRepository.findMaxPesoHistorico(1L, 5L, "Dominadas"))
                .thenReturn(10.0);

        var evolucion = entrenamientoService.obtenerEvolucionEjercicio(1L, 5L, "Dominadas");

        assertNotNull(evolucion);
        assertEquals("Dominadas", evolucion.getNombreEjercicio());
        assertEquals(10.0, evolucion.getMaxPesoHistorico());
        assertEquals(2, evolucion.getTotalSeriesCompletadas());
    }
}
