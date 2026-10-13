package com.fitfactory.services;

import com.fitfactory.dto.response.ReservaResponse;
import com.fitfactory.models.Clase;
import com.fitfactory.models.Reserva;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.ClaseRepository;
import com.fitfactory.repositories.ReservaRepository;
import com.fitfactory.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ReservaServiceTest {

    @Mock
    private ReservaRepository reservaRepository;

    @Mock
    private ClaseRepository claseRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private ReservaService reservaService;

    private Usuario usuario;
    private Usuario otroUsuario;
    private Clase claseConCupos;
    private Clase claseLlena;

    @BeforeEach
    void setUp() {
        usuario = Usuario.builder()
                .id(1L)
                .nombre("Juan Perez")
                .email("juan@fitfactory.com")
                .build();

        otroUsuario = Usuario.builder()
                .id(2L)
                .nombre("Maria Gomez")
                .email("maria@fitfactory.com")
                .build();

        claseConCupos = Clase.builder()
                .id(10L)
                .titulo("CrossFit Matutino")
                .categoria("Funcional")
                .fechaHoraInicio(LocalDateTime.now().plusDays(1))
                .fechaHoraFin(LocalDateTime.now().plusDays(1).plusHours(1))
                .sala("Sala A")
                .cuposTotales(20)
                .cuposOcupados(5)
                .build();

        claseLlena = Clase.builder()
                .id(11L)
                .titulo("Spinning Avanzado")
                .categoria("Cardio")
                .fechaHoraInicio(LocalDateTime.now().plusDays(2))
                .fechaHoraFin(LocalDateTime.now().plusDays(2).plusHours(1))
                .sala("Sala B")
                .cuposTotales(15)
                .cuposOcupados(15)
                .build();
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pruebas de reservarClase
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe reservar clase exitosamente cuando hay cupos disponibles")
    void testReservarClase_Exito_NuevaReserva() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(claseRepository.findById(10L)).thenReturn(Optional.of(claseConCupos));
        when(reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(1L, 10L, "CONFIRMADA")).thenReturn(false);
        when(reservaRepository.findByUsuarioIdAndClaseId(1L, 10L)).thenReturn(Optional.empty());
        when(claseRepository.save(any(Clase.class))).thenReturn(claseConCupos);
        when(reservaRepository.save(any(Reserva.class))).thenAnswer(invocation -> {
            Reserva r = invocation.getArgument(0);
            r.setId(100L);
            r.setFechaReserva(LocalDateTime.now());
            return r;
        });

        ReservaResponse response = reservaService.reservarClase(10L, 1L);

        assertNotNull(response);
        assertEquals("CONFIRMADA", response.getEstado());
        assertEquals(10L, response.getClaseId());
        assertEquals("CrossFit Matutino", response.getTituloClase());
        assertEquals(6, claseConCupos.getCuposOcupados()); // 5 + 1
        verify(claseRepository).save(claseConCupos);
        verify(reservaRepository).save(any(Reserva.class));
    }

    @Test
    @DisplayName("Debe reactivar reserva previa cancelada si el usuario vuelve a reservar")
    void testReservarClase_Exito_ReactivarReservaPrevia() {
        Reserva reservaPrevia = Reserva.builder()
                .id(99L)
                .usuario(usuario)
                .clase(claseConCupos)
                .estado("CANCELADA")
                .build();

        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(claseRepository.findById(10L)).thenReturn(Optional.of(claseConCupos));
        when(reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(1L, 10L, "CONFIRMADA")).thenReturn(false);
        when(reservaRepository.findByUsuarioIdAndClaseId(1L, 10L)).thenReturn(Optional.of(reservaPrevia));
        when(claseRepository.save(any(Clase.class))).thenReturn(claseConCupos);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reservaPrevia);

        ReservaResponse response = reservaService.reservarClase(10L, 1L);

        assertNotNull(response);
        assertEquals("CONFIRMADA", response.getEstado());
        assertEquals("CONFIRMADA", reservaPrevia.getEstado());
        assertEquals(6, claseConCupos.getCuposOcupados());
        verify(reservaRepository).save(reservaPrevia);
    }

    @Test
    @DisplayName("Debe lanzar excepción si el usuario no existe")
    void testReservarClase_Falla_UsuarioNoEncontrado() {
        when(usuarioRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.reservarClase(10L, 999L));

        assertEquals("Usuario no encontrado", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la clase no existe")
    void testReservarClase_Falla_ClaseNoEncontrada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(claseRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.reservarClase(999L, 1L));

        assertEquals("Clase no encontrada", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si ya existe una reserva confirmada para la misma clase")
    void testReservarClase_Falla_ReservaDuplicada() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(claseRepository.findById(10L)).thenReturn(Optional.of(claseConCupos));
        when(reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(1L, 10L, "CONFIRMADA")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.reservarClase(10L, 1L));

        assertEquals("Ya tienes una reserva confirmada para esta clase", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la clase no tiene cupos disponibles")
    void testReservarClase_Falla_SinCuposDisponibles() {
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(usuario));
        when(claseRepository.findById(11L)).thenReturn(Optional.of(claseLlena));
        when(reservaRepository.existsByUsuarioIdAndClaseIdAndEstado(1L, 11L, "CONFIRMADA")).thenReturn(false);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.reservarClase(11L, 1L));

        assertEquals("La clase está llena, no hay cupos disponibles", ex.getMessage());
        assertEquals(15, claseLlena.getCuposOcupados());
        verify(reservaRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pruebas de cancelarReserva
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe cancelar reserva confirmada y liberar cupo exitosamente")
    void testCancelarReserva_Exito() {
        Reserva reserva = Reserva.builder()
                .id(50L)
                .usuario(usuario)
                .clase(claseConCupos)
                .estado("CONFIRMADA")
                .build();

        int cuposIniciales = claseConCupos.getCuposOcupados(); // 5
        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reserva));
        when(claseRepository.save(any(Clase.class))).thenReturn(claseConCupos);
        when(reservaRepository.save(any(Reserva.class))).thenReturn(reserva);

        reservaService.cancelarReserva(50L, 1L);

        assertEquals("CANCELADA", reserva.getEstado());
        assertEquals(cuposIniciales - 1, claseConCupos.getCuposOcupados()); // 4
        verify(claseRepository).save(claseConCupos);
        verify(reservaRepository).save(reserva);
    }

    @Test
    @DisplayName("Debe lanzar excepción si la reserva a cancelar no existe")
    void testCancelarReserva_Falla_ReservaNoEncontrada() {
        when(reservaRepository.findById(999L)).thenReturn(Optional.empty());

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.cancelarReserva(999L, 1L));

        assertEquals("Reserva no encontrada", ex.getMessage());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la reserva pertenece a otro usuario")
    void testCancelarReserva_Falla_NoPerteneceAUsuario() {
        Reserva reservaDeOtro = Reserva.builder()
                .id(50L)
                .usuario(otroUsuario) // ID 2
                .clase(claseConCupos)
                .estado("CONFIRMADA")
                .build();

        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reservaDeOtro));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.cancelarReserva(50L, 1L)); // Intenta cancelar usuario 1

        assertEquals("Esta reserva no te pertenece", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    @Test
    @DisplayName("Debe lanzar excepción si la reserva ya se encuentra cancelada")
    void testCancelarReserva_Falla_YaCancelada() {
        Reserva reservaYaCancelada = Reserva.builder()
                .id(50L)
                .usuario(usuario)
                .clase(claseConCupos)
                .estado("CANCELADA")
                .build();

        when(reservaRepository.findById(50L)).thenReturn(Optional.of(reservaYaCancelada));

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> reservaService.cancelarReserva(50L, 1L));

        assertEquals("La reserva ya está cancelada o no está confirmada", ex.getMessage());
        verify(reservaRepository, never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Pruebas de consultas
    // ─────────────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("Debe listar las reservas del usuario mapeadas correctamente a DTO")
    void testObtenerMisReservas() {
        Reserva r1 = Reserva.builder()
                .id(1L)
                .usuario(usuario)
                .clase(claseConCupos)
                .estado("CONFIRMADA")
                .build();

        when(reservaRepository.findByUsuarioId(1L)).thenReturn(List.of(r1));

        List<ReservaResponse> misReservas = reservaService.obtenerMisReservas(1L);

        assertEquals(1, misReservas.size());
        assertEquals(1L, misReservas.get(0).getId());
        assertEquals("CrossFit Matutino", misReservas.get(0).getTituloClase());
    }

    @Test
    @DisplayName("Debe retornar la próxima clase confirmada si existe")
    void testObtenerProximaClase_Existe() {
        Reserva r = Reserva.builder()
                .id(1L)
                .usuario(usuario)
                .clase(claseConCupos)
                .estado("CONFIRMADA")
                .build();

        when(reservaRepository.findProximasReservasConfirmadas(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of(r));

        Optional<ReservaResponse> proxima = reservaService.obtenerProximaClase(1L);

        assertTrue(proxima.isPresent());
        assertEquals("CrossFit Matutino", proxima.get().getTituloClase());
    }

    @Test
    @DisplayName("Debe retornar Optional.empty() si el usuario no tiene clases futuras")
    void testObtenerProximaClase_Vacio() {
        when(reservaRepository.findProximasReservasConfirmadas(eq(1L), any(LocalDateTime.class)))
                .thenReturn(List.of());

        Optional<ReservaResponse> proxima = reservaService.obtenerProximaClase(1L);

        assertTrue(proxima.isEmpty());
    }
}
