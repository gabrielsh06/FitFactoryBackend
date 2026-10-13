package com.fitfactory.controllers;

import com.fitfactory.dto.request.CrearRutinaRequest;
import com.fitfactory.dto.response.RutinaResponse;
import com.fitfactory.security.services.UserDetailsImpl;
import com.fitfactory.services.RutinaService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/rutinas")
public class RutinaController {

    @Autowired
    private RutinaService rutinaService;

    @GetMapping("/mi-rutina")
    @PreAuthorize("hasRole('CLIENTE')")
    public ResponseEntity<List<RutinaResponse>> obtenerMiRutina() {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        List<RutinaResponse> rutinas = rutinaService.obtenerMiRutina(userDetails.getId());
        return ResponseEntity.ok(rutinas);
    }

    @PostMapping("/asignar")
    @PreAuthorize("hasAnyRole('ADMIN', 'ENTRENADOR')")
    public ResponseEntity<RutinaResponse> asignarRutina(@Valid @RequestBody CrearRutinaRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RutinaResponse rutina = rutinaService.asignarRutina(request, userDetails.getId());
        return ResponseEntity.status(HttpStatus.CREATED).body(rutina);
    }
}
