package com.example.demo.Repositorios;

import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Repositorio de usuarios.
 *
 * Al extender JpaRepository ya no hay que escribir la implementacion: Spring Data
 * la genera en tiempo de ejecucion. De ahi salen gratis findAll, findById, save,
 * deleteById, existsById, count, etc.
 *
 * Los metodos de abajo son "derived queries": Spring lee el nombre del metodo y
 * arma el SQL solo. findByEmailIgnoreCase se traduce a
 * "select * from usuarios where lower(email) = lower(?)".
 */
@Repository
public interface UsuarioRepository extends JpaRepository<Usuario, Long> {

    Usuario findByEmailIgnoreCase(String email);

    /** Consulta derivada: la usa el registro para avisar que el email ya existe. */
    boolean existsByEmailIgnoreCase(String email);

    List<Usuario> findByRol(Rol rol);

    /** Busqueda parcial: "jua" encuentra a "Juan" y a "Juana". */
    List<Usuario> findByNombreContainingIgnoreCase(String nombre);
}
