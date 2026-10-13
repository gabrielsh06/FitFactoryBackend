package com.fitfactory;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Punto de entrada de FitFactory Backend.
 *
 * <p>{@code @EnableAsync} habilita el pool de hilos de Spring para la ejecución
 * de métodos anotados con {@code @Async} (ej. EmailService). Sin esta anotación
 * el {@code @Async} se ignora y el envío de email bloquea el hilo HTTP.
 */
@SpringBootApplication
@EnableAsync
public class FitFactoryApplication {

    public static void main(String[] args) {
        SpringApplication.run(FitFactoryApplication.class, args);
    }
}
