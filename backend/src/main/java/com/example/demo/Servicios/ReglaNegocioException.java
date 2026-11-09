package com.example.demo.Servicios;

/**
 * Se lanza cuando los datos son validos en forma pero rompen una regla del
 * negocio: email repetido, horario cruzado, reserva ya calificada, etc.
 *
 * Los controladores la atrapan para volver a mostrar el formulario con el
 * mensaje; si nadie la atrapa, el manejador global muestra la pagina de error.
 */
public class ReglaNegocioException extends RuntimeException {

    public ReglaNegocioException(String mensaje) {
        super(mensaje);
    }
}
