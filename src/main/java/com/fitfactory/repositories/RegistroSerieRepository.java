package com.fitfactory.repositories;

import com.fitfactory.models.RegistroSerie;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RegistroSerieRepository extends JpaRepository<RegistroSerie, Long> {

    List<RegistroSerie> findBySesionIdOrderByNumeroSerieAsc(Long sesionId);

    @Query("SELECT r FROM RegistroSerie r " +
           "JOIN r.sesion s " +
           "WHERE s.cliente.id = :clienteId " +
           "AND s.estado != com.fitfactory.models.EstadoSesion.CANCELADA " +
           "AND ((:ejercicioId IS NOT NULL AND r.ejercicio.id = :ejercicioId) OR LOWER(r.nombreEjercicio) = LOWER(:nombreEjercicio)) " +
           "AND r.completada = true " +
           "ORDER BY r.fechaHora DESC")
    List<RegistroSerie> findHistorialEjercicio(@Param("clienteId") Long clienteId,
                                               @Param("ejercicioId") Long ejercicioId,
                                               @Param("nombreEjercicio") String nombreEjercicio,
                                               Pageable pageable);

    @Query("SELECT MAX(r.pesoKg) FROM RegistroSerie r " +
           "JOIN r.sesion s " +
           "WHERE s.cliente.id = :clienteId " +
           "AND s.estado != com.fitfactory.models.EstadoSesion.CANCELADA " +
           "AND ((:ejercicioId IS NOT NULL AND r.ejercicio.id = :ejercicioId) OR LOWER(r.nombreEjercicio) = LOWER(:nombreEjercicio)) " +
           "AND r.completada = true " +
           "AND r.tipoSerie IN (com.fitfactory.models.TipoSerie.NORMAL, com.fitfactory.models.TipoSerie.AL_FALLO)")
    Double findMaxPesoHistorico(@Param("clienteId") Long clienteId,
                                @Param("ejercicioId") Long ejercicioId,
                                @Param("nombreEjercicio") String nombreEjercicio);

    @Query("SELECT r FROM RegistroSerie r " +
           "JOIN r.sesion s " +
           "WHERE s.cliente.id = :clienteId " +
           "AND s.estado != com.fitfactory.models.EstadoSesion.CANCELADA " +
           "AND ((:ejercicioId IS NOT NULL AND r.ejercicio.id = :ejercicioId) OR LOWER(r.nombreEjercicio) = LOWER(:nombreEjercicio)) " +
           "AND r.completada = true " +
           "ORDER BY r.fechaHora DESC")
    List<RegistroSerie> findEvolucionEjercicio(@Param("clienteId") Long clienteId,
                                               @Param("ejercicioId") Long ejercicioId,
                                               @Param("nombreEjercicio") String nombreEjercicio);

    /**
     * Cuenta las series marcadas como Récord Personal que pertenecen a
     * las sesiones cuyos IDs se indican.  Se utiliza para calcular los
     * PRs de la semana en las estadísticas del Home.
     *
     * @param sesionIds Lista de IDs de sesiones FINALIZADAS de la semana.
     */
    @Query("SELECT COUNT(r) FROM RegistroSerie r " +
           "WHERE r.sesion.id IN :sesionIds " +
           "AND r.esRecordPersonal = true " +
           "AND r.completada = true")
    long countRecordsPersonalesBySesionIds(@Param("sesionIds") List<Long> sesionIds);
}
