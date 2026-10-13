package com.fitfactory;

import com.fitfactory.models.ERol;
import com.fitfactory.models.Rol;
import com.fitfactory.repositories.RolRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * DataInitializer: Carga los datos iniciales en la base de datos
 * al arrancar la aplicación, si aún no existen.
 *
 * Inserta los 3 roles base: ROLE_CLIENTE, ROLE_ENTRENADOR, ROLE_ADMIN.
 */
@Configuration
public class DataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializer.class);

    @Bean
    public CommandLineRunner initRoles(RolRepository rolRepository) {
        return args -> {
            logger.info("=== FitFactory DataInitializer: Verificando roles en BD ===");

            for (ERol eRol : ERol.values()) {
                if (rolRepository.findByNombre(eRol).isEmpty()) {
                    Rol rol = Rol.builder().nombre(eRol).build();
                    rolRepository.save(rol);
                    logger.info("  ✔ Rol insertado: {}", eRol.name());
                } else {
                    logger.info("  ✓ Rol ya existe: {}", eRol.name());
                }
            }

            logger.info("=== DataInitializer completado. Roles listos. ===");
        };
    }
}
