package com.example.demo.pagos;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

/** Guarda y busca ordenes. findByReferencia lo usan el webhook y la consulta de estado. */
public interface OrdenRepository extends JpaRepository<Orden, Long> {

    Optional<Orden> findByReferencia(String referencia);
}
