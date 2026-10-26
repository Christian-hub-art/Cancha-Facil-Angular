package com.example.demo.Controladores;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;

/**
 * Manejo de errores del API REST.
 *
 * ManejadorGlobalExcepciones solo atiende las paginas Thymeleaf y devuelve HTML.
 * Este manejador cubre el API REST y responde JSON con el codigo HTTP correcto,
 * para que lo documentado en Swagger (404, 400) coincida con lo que se recibe.
 * Sin el, estas excepciones llegaban al cliente como un 500.
 */
@RestControllerAdvice(assignableTypes = UsuarioRestController.class)
public class ManejadorErroresApi {

    /** Id inexistente en PUT o DELETE -> 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<Map<String, Object>> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Regla de negocio violada (email repetido, contrasena vacia...) -> 400. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<Map<String, Object>> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Parametro con tipo incorrecto: /api/usuarios/abc o /api/usuarios/rol/OTRO -> 400. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> tipoInvalido(MethodArgumentTypeMismatchException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "Valor no valido para '" + ex.getName() + "'");
    }

    private ResponseEntity<Map<String, Object>> respuesta(HttpStatus estado, String mensaje) {
        return ResponseEntity.status(estado).body(Map.of(
                "estado", estado.value(),
                "error", estado.getReasonPhrase(),
                "mensaje", mensaje == null ? "" : mensaje));
    }
}
