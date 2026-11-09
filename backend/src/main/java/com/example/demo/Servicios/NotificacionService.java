package com.example.demo.Servicios;

import java.util.List;

import com.example.demo.Entidades.Notificacion;

public interface NotificacionService {

    List<Notificacion> listar();

    Notificacion buscarPorId(Long id);

    Notificacion obtenerPorId(Long id);

    List<Notificacion> listarPorUsuario(Long usuarioId);

    List<Notificacion> listarNoLeidas(Long usuarioId);

    Notificacion guardar(Notificacion notificacion, Long usuarioId);

    Notificacion marcarComoLeida(Long id);

    void eliminar(Long id);
}
