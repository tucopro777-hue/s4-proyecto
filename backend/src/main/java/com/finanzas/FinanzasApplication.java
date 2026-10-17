package com.finanzas; // Paquete raíz de la aplicación

import org.springframework.boot.SpringApplication; // Clase para arrancar Spring Boot
import org.springframework.boot.autoconfigure.SpringBootApplication; // Anotación de configuración principal

@SpringBootApplication // Activa escaneo de componentes y autoconfiguración
public class FinanzasApplication { // Clase principal de la aplicación

    public static void main(String[] args) { // Punto de entrada de Java
        SpringApplication.run(FinanzasApplication.class, args); // Levanta el servidor y el contexto
    }
}
