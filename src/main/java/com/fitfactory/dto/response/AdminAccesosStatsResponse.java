package com.fitfactory.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAccesosStatsResponse {
    private LocalDate fecha;
    private long totalAccesos;
    private long accesosPermitidos;
    private long accesosRechazados;
}
