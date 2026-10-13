package com.fitfactory.controllers;

import com.fitfactory.dto.request.ActualizarPerfilRequest;
import com.fitfactory.dto.response.UserProfileResponse;
import com.fitfactory.models.Usuario;
import com.fitfactory.repositories.UsuarioRepository;
import com.fitfactory.security.services.UserDetailsImpl;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@CrossOrigin(origins = "*", maxAge = 3600)
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PutMapping("/perfil")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<UserProfileResponse> actualizarPerfil(@Valid @RequestBody ActualizarPerfilRequest request) {
        UserDetailsImpl userDetails = (UserDetailsImpl) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        Usuario usuario = usuarioRepository.findById(userDetails.getId())
                .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado"));

        usuario.setNombre(request.getNombre());
        usuario.setTelefono(request.getTelefono());
        usuario.setPeso(request.getPeso());
        usuario.setAltura(request.getAltura());
        usuario.setObjetivoFitness(request.getObjetivoFitness());

        // Calcular IMC
        if (request.getPeso() != null && request.getAltura() != null) {
            double alturaMetros = request.getAltura() / 100.0;
            double imc = request.getPeso() / (alturaMetros * alturaMetros);
            // Redondear a un decimal
            usuario.setImc(Math.round(imc * 10.0) / 10.0);
        } else {
            usuario.setImc(null);
        }

        usuarioRepository.save(usuario);

        UserProfileResponse response = UserProfileResponse.builder()
                .id(usuario.getId())
                .nombre(usuario.getNombre())
                .email(usuario.getEmail())
                .telefono(usuario.getTelefono())
                .planMembresia(usuario.getPlanMembresia())
                .peso(usuario.getPeso())
                .altura(usuario.getAltura())
                .imc(usuario.getImc())
                .objetivoFitness(usuario.getObjetivoFitness())
                .build();

        return ResponseEntity.ok(response);
    }
}
