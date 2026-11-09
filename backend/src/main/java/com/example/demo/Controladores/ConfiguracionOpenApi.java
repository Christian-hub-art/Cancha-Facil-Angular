package com.example.demo.Controladores;

import org.springframework.context.annotation.Configuration;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.info.Info;

/**
 * Datos generales que muestra Swagger UI en el encabezado de la documentacion
 * (titulo, version y descripcion). springdoc lee esta anotacion al generar
 * /v3/api-docs; el resto de la documentacion sale de los controladores REST.
 */
@Configuration
@OpenAPIDefinition(info = @Info(
        title = "CanchaFacil API",
        version = "1.0",
        description = "API REST de CanchaFacil, la aplicacion de reservas de espacios deportivos. "
                + "La consume el frontend Angular."))
public class ConfiguracionOpenApi {
}
