package com.fitfactory.controllers;

import com.fitfactory.dto.request.ActualizarRolRequest;
import com.fitfactory.dto.response.AdminAccesosStatsResponse;
import com.fitfactory.dto.response.MessageResponse;
import com.fitfactory.dto.response.UsuarioAdminResponse;
import com.fitfactory.models.ERol;
import com.fitfactory.models.Rol;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.RolRepository;
import com.fitfactory.repositories.UsuarioRepository;
import com.fitfactory.services.AsistenciaService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Tag(name = "Admin - Usuarios", description = "Endpoints de administración para gestión de usuarios, roles y métricas de acceso")
@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class AdminUsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @Autowired
    private AsistenciaService asistenciaService;

    @Operation(summary = "Listar usuarios paginados", description = "Retorna el listado paginado de usuarios con soporte de filtros y ordenamiento.")
    @GetMapping
    public ResponseEntity<Page<UsuarioAdminResponse>> listarUsuarios(
            @PageableDefault(size = 10, sort = "id", direction = Sort.Direction.DESC) Pageable pageable) {
        Page<UsuarioAdminResponse> usuarios = usuarioRepository.findAll(pageable)
                .map(this::mapearADto);
        return ResponseEntity.ok(usuarios);
    }

    @Operation(summary = "Alternar estado de activación", description = "Activa o desactiva la cuenta de un usuario.")
    @PatchMapping("/{id}/toggle-estado")
    public ResponseEntity<MessageResponse> alternarEstado(@PathVariable Long id) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));
        
        usuario.setActivo(!usuario.getActivo());
        usuarioRepository.save(usuario);
        
        String accion = usuario.getActivo() ? "activada" : "desactivada";
        return ResponseEntity.ok(MessageResponse.ok("Cuenta " + accion + " exitosamente."));
    }

    @Operation(summary = "Actualizar rol de usuario", description = "Asigna un nuevo rol (ROLE_CLIENTE, ROLE_ENTRENADOR, ROLE_ADMIN) al usuario.")
    @PutMapping("/{id}/rol")
    public ResponseEntity<MessageResponse> actualizarRol(@PathVariable Long id, @Valid @RequestBody ActualizarRolRequest request) {
        Usuario usuario = usuarioRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        ERol rolEnum;
        try {
            rolEnum = ERol.valueOf(request.getRol());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rol inválido. Usa ROLE_CLIENTE, ROLE_ENTRENADOR o ROLE_ADMIN");
        }

        Rol rol = rolRepository.findByNombre(rolEnum)
                .orElseThrow(() -> new IllegalArgumentException("El rol no existe en la BD."));

        Set<Rol> roles = new HashSet<>();
        roles.add(rol);
        usuario.setRoles(roles);
        
        usuarioRepository.save(usuario);
        
        return ResponseEntity.ok(MessageResponse.ok("Rol actualizado exitosamente a " + request.getRol()));
    }

    @Operation(summary = "Métricas globales de accesos", description = "Obtiene estadísticas del día o fecha seleccionada de accesos permitidos y denegados.")
    @GetMapping("/accesos/stats")
    public ResponseEntity<AdminAccesosStatsResponse> obtenerStatsAccesosHoy(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        return ResponseEntity.ok(asistenciaService.obtenerStatsAccesosGlobales(fecha));
    }

    private UsuarioAdminResponse mapearADto(Usuario usuario) {
        List<String> rolesStr = usuario.getRoles().stream()
                .map(r -> r.getNombre().name())
                .collect(Collectors.toList());

        return UsuarioAdminResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .roles(rolesStr)
                .estadoActivo(usuario.getActivo())
                .fechaCreacion(usuario.getFechaCreacion())
                .build();
    }
}
