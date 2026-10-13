package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Estadísticas semanales del usuario autenticado.
 *
 * <p>La "semana actual" se define como el período de 7 días completos que
 * terminan a la medianoche del día actual (UTC-5 Colombia / Bogotá).
 * Es decir: desde el inicio del día de hace 6 días hasta el fin del día
 * de hoy (inclusive).  Este criterio evita sorpresas por cambios de zona
 * horaria y es coherente con {@code LocalDateTime.now()} que utiliza el
 * resto de la aplicación.</p>
 *
 * <p>Todas las métricas excluyen sesiones {@code CANCELADA} y series con
 * {@code completada = false}.  Si no existen datos se retornan ceros, nunca
 * valores inventados.</p>
 *
 * <ul>
 *   <li>{@code sesionesFinalizadas} — nº de sesiones con estado {@code FINALIZADA}.</li>
 *   <li>{@code totalSeries} — suma de {@code totalSeries} de esas sesiones.</li>
 *   <li>{@code volumenTotalKg} — suma de {@code volumenTotalKg} de esas sesiones.</li>
 *   <li>{@code recordsPersonales} — nº de series marcadas {@code esRecordPersonal=true}
 *       dentro de esas sesiones.</li>
 *   <li>{@code minutosTotales} — suma de {@code duracionSegundos}/60 redondeada.</li>
 * </ul>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsSemanalesResponse {
    private int sesionesFinalizadas;
    private int totalSeries;
    private double volumenTotalKg;
    private int recordsPersonales;
    private int minutosTotales;
}
