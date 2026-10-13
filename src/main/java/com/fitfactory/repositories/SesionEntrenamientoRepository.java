package com.fitfactory.repositories;

import com.fitfactory.models.EstadoSesion;
import com.fitfactory.models.SesionEntrenamiento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface SesionEntrenamientoRepository extends JpaRepository<SesionEntrenamiento, Long> {

    Optional<SesionEntrenamiento> findFirstByClienteIdAndEstadoOrderByFechaInicioDesc(Long clienteId, EstadoSesion estado);

    List<SesionEntrenamiento> findByClienteIdOrderByFechaInicioDesc(Long clienteId);

    List<SesionEntrenamiento> findByClienteIdAndEstado(Long clienteId, EstadoSesion estado);

    List<SesionEntrenamiento> findByClienteIdAndEstadoOrderByFechaInicioDesc(Long clienteId, EstadoSesion estado);

    /**
     * Retorna las sesiones FINALIZADAS del cliente cuya fecha de inicio cae
     * dentro del rango [desde, hasta].  Se usa para calcular las estadísticas
     * semanales sin cargar el historial completo.
     *
     * @param clienteId ID del cliente autenticado (obtenido del JWT).
     * @param desde     Inicio del período (inclusive).
     * @param hasta     Fin del período (inclusive).
     */
    @Query("SELECT s FROM SesionEntrenamiento s " +
           "WHERE s.cliente.id = :clienteId " +
           "AND s.estado = com.fitfactory.models.EstadoSesion.FINALIZADA " +
           "AND s.fechaInicio >= :desde " +
           "AND s.fechaInicio <= :hasta")
    List<SesionEntrenamiento> findSemanalesFinalizadas(
            @Param("clienteId") Long clienteId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta);
}
