package com.fitfactory.security.services;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fitfactory.models.Usuario;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.io.Serial;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementación de UserDetails de Spring Security
 * que adapta la entidad Usuario a la interfaz de seguridad.
 */
@AllArgsConstructor
@Getter
public class UserDetailsImpl implements UserDetails {

    @Serial
    private static final long serialVersionUID = 1L;

    private Long id;
    private String nombre;
    private String email;
    private Boolean activo;

    @JsonIgnore
    private String password;

    private Collection<? extends GrantedAuthority> authorities;

    /**
     * Factory method para construir UserDetailsImpl desde una entidad Usuario.
     */
    public static UserDetailsImpl build(Usuario usuario) {
        List<GrantedAuthority> authorities = usuario.getRoles().stream()
                .map(rol -> new SimpleGrantedAuthority(rol.getNombre().name()))
                .collect(Collectors.toList());

        return new UserDetailsImpl(
                usuario.getId(),
                usuario.getNombre(),
                usuario.getEmail(),
                usuario.getActivo(),
                usuario.getPassword(),
                authorities
        );
    }

    // ── UserDetails interface ──────────────────────────────

    @Override
    public String getUsername() {
        return email; // El username en Spring Security es el email
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return activo != null ? activo : false;
    }
}
