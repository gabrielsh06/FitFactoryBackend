package com.fitfactory.repositories;

import com.fitfactory.models.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByClaseId(Long claseId);

    Optional<Reserva> findByUsuarioIdAndClaseId(Long usuarioId, Long claseId);

    boolean existsByUsuarioIdAndClaseId(Long usuarioId, Long claseId);

    boolean existsByUsuarioIdAndClaseIdAndEstado(Long usuarioId, Long claseId, String estado);

    /**
     * Retorna la primera reserva CONFIRMADA del usuario cuya clase aún no
     * ha comenzado (fechaHoraInicio futura), ordenada por fecha de clase ASC.
     * Se usa para mostrar la "Próxima Clase" en el Home.
     *
     * @param usuarioId ID del usuario autenticado.
     * @param ahora     Momento actual para filtrar clases futuras.
     */
    @Query("SELECT r FROM Reserva r " +
           "JOIN r.clase c " +
           "WHERE r.usuario.id = :usuarioId " +
           "AND r.estado = 'CONFIRMADA' " +
           "AND c.fechaHoraInicio > :ahora " +
           "ORDER BY c.fechaHoraInicio ASC")
    List<Reserva> findProximasReservasConfirmadas(
            @Param("usuarioId") Long usuarioId,
            @Param("ahora") LocalDateTime ahora);
}
