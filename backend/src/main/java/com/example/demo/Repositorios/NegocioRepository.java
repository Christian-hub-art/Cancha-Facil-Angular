package com.example.demo.Repositorios;

import com.example.demo.Entidades.Negocio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NegocioRepository extends JpaRepository<Negocio, Long> {

    Negocio findByNitIgnoreCase(String nit);

    List<Negocio> findByAdministradorId(Long administradorId);

    List<Negocio> findByNombreContainingIgnoreCase(String nombre);
}
