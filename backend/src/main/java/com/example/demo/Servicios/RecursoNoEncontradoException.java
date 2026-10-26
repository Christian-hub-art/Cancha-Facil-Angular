package com.example.demo.Servicios;

/**
 * Se lanza cuando se busca un registro por id y no existe en la base de datos.
 * El manejador global la convierte en la pagina de error 404.
 */
public class RecursoNoEncontradoException extends RuntimeException {

    public RecursoNoEncontradoException(String recurso, Long id) {
        super(recurso + " con id " + id + " no existe");
    }

    public RecursoNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
