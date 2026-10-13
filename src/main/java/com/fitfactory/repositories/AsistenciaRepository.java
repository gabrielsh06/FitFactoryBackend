package com.fitfactory.repositories;

import com.fitfactory.models.Asistencia;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AsistenciaRepository extends JpaRepository<Asistencia, Long> {

    List<Asistencia> findByUsuarioIdOrderByFechaHoraAccesoDesc(Long usuarioId);

    List<Asistencia> findByUsuarioIdOrderByFechaHoraAccesoDesc(Long usuarioId, Pageable pageable);

    Page<Asistencia> findByUsuarioId(Long usuarioId, Pageable pageable);

    @Query("SELECT a FROM Asistencia a WHERE a.usuario.id = :usuarioId AND a.fechaHoraAcceso >= :desde AND a.fechaHoraAcceso <= :hasta")
    Page<Asistencia> findByUsuarioIdAndPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta,
            Pageable pageable
    );

    @Query("SELECT a FROM Asistencia a WHERE a.usuario.id = :usuarioId AND a.fechaHoraAcceso >= :desde AND a.fechaHoraAcceso <= :hasta ORDER BY a.fechaHoraAcceso DESC")
    List<Asistencia> findByUsuarioIdAndPeriodo(
            @Param("usuarioId") Long usuarioId,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.fechaHoraAcceso >= :desde AND a.fechaHoraAcceso <= :hasta")
    long countTotalGlobalByPeriodo(
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );

    @Query("SELECT COUNT(a) FROM Asistencia a WHERE a.accesoPermitido = :permitido AND a.fechaHoraAcceso >= :desde AND a.fechaHoraAcceso <= :hasta")
    long countGlobalByAccesoPermitidoAndPeriodo(
            @Param("permitido") Boolean permitido,
            @Param("desde") LocalDateTime desde,
            @Param("hasta") LocalDateTime hasta
    );
}
