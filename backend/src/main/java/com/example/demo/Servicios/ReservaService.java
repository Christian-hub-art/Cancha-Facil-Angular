package com.example.demo.Servicios;

import java.util.List;
import java.util.Map;

import com.example.demo.Entidades.Reserva;

public interface ReservaService {

    /** Estados posibles de una reserva. */
    String PENDIENTE = "PENDIENTE";
    String CONFIRMADA = "CONFIRMADA";
    String CANCELADA = "CANCELADA";
    String COMPLETADA = "COMPLETADA";

    List<Reserva> listar();

    Reserva buscarPorId(Long id);

    Reserva obtenerPorId(Long id);

    List<Reserva> listarPorUsuario(Long usuarioId);

    /** Reservas del usuario que inicio sesion (consulta JPQL por email). */
    List<Reserva> listarPorEmailDeUsuario(String email);

    List<Reserva> listarPorEspacio(Long espacioId);

    List<Reserva> listarPorEstado(String estado);

    /** Consulta personalizada: cantidad de reservas por estado. */
    Map<String, Long> contarPorEstado();

    Reserva guardar(Reserva reserva, Long usuarioId, Long espacioId);

    Reserva cambiarEstado(Long id, String estado);

    Reserva cancelar(Long id);

    void eliminar(Long id);
}
