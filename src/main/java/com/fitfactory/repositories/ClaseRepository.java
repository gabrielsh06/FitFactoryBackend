package com.fitfactory.repositories;

import com.fitfactory.models.Clase;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ClaseRepository extends JpaRepository<Clase, Long> {

    List<Clase> findByEntrenadorId(Long entrenadorId);

    List<Clase> findByCategoria(String categoria);

    @Query("SELECT c FROM Clase c WHERE c.fechaHoraInicio >= :desde AND c.cuposOcupados < c.cuposTotales ORDER BY c.fechaHoraInicio ASC")
    List<Clase> findClasesDisponibles(@Param("desde") LocalDateTime desde);

    @Query("SELECT c FROM Clase c WHERE c.fechaHoraInicio >= :desde AND c.cuposOcupados < c.cuposTotales")
    Page<Clase> findClasesDisponibles(@Param("desde") LocalDateTime desde, Pageable pageable);
}
