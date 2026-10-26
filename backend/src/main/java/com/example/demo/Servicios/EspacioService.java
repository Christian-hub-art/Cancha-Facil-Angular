package com.example.demo.Servicios;

import java.math.BigDecimal;
import java.util.List;

import com.example.demo.Entidades.Espacio;

public interface EspacioService {

    List<Espacio> listar();

    Espacio buscarPorId(Long id);

    Espacio obtenerPorId(Long id);

    List<Espacio> listarPorNegocio(Long negocioId);

    List<Espacio> buscarPorDeporte(String tipoDeporte);

    /** Consulta personalizada: espacios con precio por hora entre min y max. */
    List<Espacio> buscarPorRangoDePrecio(BigDecimal min, BigDecimal max);

    Espacio guardar(Espacio espacio, Long negocioId);

    void eliminar(Long id);
}
