package com.example.demo.Servicios;

import java.util.List;

import com.example.demo.Entidades.Negocio;

public interface NegocioService {

    List<Negocio> listar();

    Negocio buscarPorId(Long id);

    Negocio obtenerPorId(Long id);

    List<Negocio> buscarPorNombre(String nombre);

    List<Negocio> listarPorAdministrador(Long administradorId);

    Negocio guardar(Negocio negocio, Long administradorId);

    void eliminar(Long id);
}
