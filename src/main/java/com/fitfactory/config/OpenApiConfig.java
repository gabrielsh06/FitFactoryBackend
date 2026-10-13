package com.fitfactory.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración de Swagger / OpenAPI 3 para FitFactory Backend.
 *
 * <p>Características:
 * <ul>
 *   <li>Esquema de seguridad global {@code bearerAuth} (JWT Bearer Token).</li>
 *   <li>Aplica a todos los endpoints por defecto vía {@code @SecurityRequirement}.</li>
 *   <li>Los endpoints públicos (/api/auth/**) no requieren token pero el botón
 *       "Authorize" en Swagger UI sigue disponible para comodidad del developer.</li>
 * </ul>
 *
 * <p>Acceso: {@code http://localhost:8082/swagger-ui.html}
 */
@Configuration
@OpenAPIDefinition(
        info = @Info(
                title       = "FitFactory API",
                version     = "v1.0",
                description = "REST API para la aplicación de gestión de gimnasio FitFactory. " +
                              "Usa el botón 'Authorize' e ingresa tu JWT con el formato: Bearer <token>",
                contact     = @Contact(name = "Equipo FitFactory UTP"),
                license     = @License(name = "MIT")
        ),
        servers = {
                @Server(url = "http://localhost:8082", description = "Desarrollo local"),
        },
        // Aplica bearerAuth a TODOS los endpoints por defecto.
        // Los endpoints públicos tienen permitAll() en SecurityConfig, así que el token
        // es opcional para ellos aunque aparezca el candado en Swagger UI.
        security = @SecurityRequirement(name = "bearerAuth")
)
@SecurityScheme(
        name         = "bearerAuth",
        type         = SecuritySchemeType.HTTP,
        scheme       = "bearer",
        bearerFormat = "JWT",
        description  = "Ingresa el JWT obtenido en /api/auth/login. Formato: Bearer <token>"
)
public class OpenApiConfig {
    // Sin beans adicionales: springdoc escanea las anotaciones OpenAPI automáticamente.
}
