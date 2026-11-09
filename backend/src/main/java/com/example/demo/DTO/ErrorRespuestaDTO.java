package com.example.demo.DTO;

/**
 * Cuerpo JSON que devuelve la API REST cuando algo sale mal, por ejemplo:
 * { "status": 404, "mensaje": "El espacio con id 99 no existe" }
 *
 * Angular lee el codigo con error.status y el texto con error.error.mensaje.
 */
public record ErrorRespuestaDTO(int status, String mensaje) {
}
