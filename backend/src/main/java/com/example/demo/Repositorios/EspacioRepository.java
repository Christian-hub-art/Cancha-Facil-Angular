package com.example.demo.Repositorios;

import com.example.demo.Entidades.Espacio;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface EspacioRepository extends JpaRepository<Espacio, Long> {

    List<Espacio> findByNegocioId(Long negocioId);

    List<Espacio> findByTipoDeporteIgnoreCase(String tipoDeporte);

    /**
     * Consulta personalizada (JPQL): espacios cuyo precio por hora esta dentro de
     * un rango, del mas barato al mas caro. Se usa en la pagina de Consultas.
     */
    @Query("select e from Espacio e where e.precioHora between :min and :max order by e.precioHora asc")
    List<Espacio> buscarPorRangoDePrecio(@Param("min") BigDecimal min, @Param("max") BigDecimal max);
}
