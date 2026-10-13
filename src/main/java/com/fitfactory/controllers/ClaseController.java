package com.fitfactory.controllers;

import com.fitfactory.dto.request.ClaseRequest;
import com.fitfactory.dto.response.ClaseResponse;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.ClaseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@Tag(name = "Clases", description = "Gestión del catálogo y programación de clases grupales")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/clases")
public class ClaseController {

    @Autowired
    private ClaseService claseService;

    @Operation(summary = "Listar clases disponibles", description = "Retorna el catálogo de clases con cupos disponibles de forma paginada.")
    @GetMapping
    public ResponseEntity<Page<ClaseResponse>> obtenerClases(
            @PageableDefault(size = 10, sort = "fechaHoraInicio", direction = Sort.Direction.ASC) Pageable pageable) {
        Long usuarioId = null;
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof UserDetailsImpl) {
            usuarioId = ((UserDetailsImpl) auth.getPrincipal()).getId();
        }

        Page<ClaseResponse> clases = claseService.obtenerClasesPaginadas(LocalDateTime.now(), usuarioId, pageable);
        return ResponseEntity.ok(clases);
    }

    @Operation(summary = "Crear nueva clase grupal", description = "Permite a administradores y entrenadores programar una nueva clase.")
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<ClaseResponse> crearClase(@Valid @RequestBody ClaseRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        ClaseResponse response = claseService.crearClase(request, userDetails.getId());
        return ResponseEntity.ok(response);
    }
}
