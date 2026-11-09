package com.example.demo.Controladores;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.DTO.ErrorRespuestaDTO;
import com.example.demo.Servicios.RecursoNoEncontradoException;
import com.example.demo.Servicios.ReglaNegocioException;

/**
 * Manejo de errores SOLO para la API REST (/api/**).
 *
 * ManejadorGlobalExcepciones muestra paginas HTML de Thymeleaf; este, en
 * cambio, responde JSON con el codigo HTTP correcto para que Angular pueda
 * mostrar un mensaje segun error.status. Se limita a los controladores REST
 * con assignableTypes para no mezclarse con las paginas.
 */
@RestControllerAdvice(assignableTypes = {UsuarioRestController.class, EspacioRestController.class})
public class ManejadorErroresApi {

    private static final Logger log = LoggerFactory.getLogger(ManejadorErroresApi.class);

    /** El id no existe -> 404. */
    @ExceptionHandler(RecursoNoEncontradoException.class)
    public ResponseEntity<ErrorRespuestaDTO> noEncontrado(RecursoNoEncontradoException ex) {
        return respuesta(HttpStatus.NOT_FOUND, ex.getMessage());
    }

    /** Regla de negocio (contrasena obligatoria, negocio invalido, precio negativo...) -> 400. */
    @ExceptionHandler(ReglaNegocioException.class)
    public ResponseEntity<ErrorRespuestaDTO> reglaNegocio(ReglaNegocioException ex) {
        return respuesta(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    /** Fallo de @Valid en el cuerpo del POST/PUT -> 400 con el primer mensaje. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuestaDTO> datosInvalidos(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .findFirst()
                .map(error -> error.getDefaultMessage())
                .orElse("Los datos enviados no son validos");
        return respuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    /** JSON mal formado o tipo incorrecto en la URL (/api/espacios/abc) -> 400. */
    @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
    public ResponseEntity<ErrorRespuestaDTO> peticionInvalida(Exception ex) {
        return respuesta(HttpStatus.BAD_REQUEST, "La peticion no tiene el formato esperado");
    }

    /** Llave foranea o valor unico violado en PostgreSQL -> 409. */
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuestaDTO> integridad(DataIntegrityViolationException ex) {
        log.warn("Error de integridad en la API: {}", ex.getMessage());
        return respuesta(HttpStatus.CONFLICT,
                "El registro esta relacionado con otros datos o tiene un valor repetido");
    }

    private ResponseEntity<ErrorRespuestaDTO> respuesta(HttpStatus status, String mensaje) {
        return ResponseEntity.status(status).body(new ErrorRespuestaDTO(status.value(), mensaje));
    }
}
