package com.fitfactory.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Utilidades JWT para generación, validación y extracción de claims.
 * Usa jjwt 0.12.x con firma HMAC-SHA256 (HS256).
 */
@Component
public class JwtUtils {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtils.class);

    @Value("${fitfactory.app.jwtSecret}")
    private String jwtSecret;

    @Value("${fitfactory.app.jwtExpirationMs}")
    private int jwtExpirationMs;

    // ─────────────────────────────────────────────────────────
    // Clave de firma derivada del secret en Base64 (o raw UTF-8)
    // ─────────────────────────────────────────────────────────
    private SecretKey signingKey() {
        // Si el secret tiene 64+ chars UTF-8, lo usamos directamente con HMAC-SHA256
        byte[] keyBytes = jwtSecret.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // ─────────────────────────────────────────────────────────
    // Generar token a partir de la autenticación de Spring Security
    // ─────────────────────────────────────────────────────────
    public String generateJwtToken(Authentication authentication) {
        com.fitfactory.security.services.UserDetailsImpl userPrincipal =
                (com.fitfactory.security.services.UserDetailsImpl) authentication.getPrincipal();

        List<String> roles = userPrincipal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userPrincipal.getUsername())           // email como subject
                .claim("id", userPrincipal.getId())
                .claim("nombre", userPrincipal.getNombre())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    /**
     * Genera token directamente desde un UserDetailsImpl (útil en /register).
     */
    public String generateTokenFromUserDetails(com.fitfactory.security.services.UserDetailsImpl userDetails) {
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());

        return Jwts.builder()
                .subject(userDetails.getUsername())
                .claim("id", userDetails.getId())
                .claim("nombre", userDetails.getNombre())
                .claim("roles", roles)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpirationMs))
                .signWith(signingKey(), Jwts.SIG.HS256)
                .compact();
    }

    // ─────────────────────────────────────────────────────────
    // Extraer username (email) del token
    // ─────────────────────────────────────────────────────────
    public String getUsernameFromJwtToken(String token) {
        return parseClaims(token).getSubject();
    }

    // ─────────────────────────────────────────────────────────
    // Validar token
    // ─────────────────────────────────────────────────────────
    public boolean validateJwtToken(String authToken) {
        try {
            parseClaims(authToken);
            return true;
        } catch (SignatureException e) {
            logger.error("Firma JWT inválida: {}", e.getMessage());
        } catch (MalformedJwtException e) {
            logger.error("Token JWT malformado: {}", e.getMessage());
        } catch (ExpiredJwtException e) {
            logger.error("Token JWT expirado: {}", e.getMessage());
        } catch (UnsupportedJwtException e) {
            logger.error("Token JWT no soportado: {}", e.getMessage());
        } catch (IllegalArgumentException e) {
            logger.error("JWT claims vacío: {}", e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────
    // Helper privado
    // ─────────────────────────────────────────────────────────
    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
