package com.example.demo.Repositorios;

import com.example.demo.Entidades.Notificacion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface NotificacionRepository extends JpaRepository<Notificacion, Long> {

    List<Notificacion> findByUsuarioId(Long usuarioId);

    /**
     * Spring Data no sabe derivar el SQL de un nombre en espanol como este, asi que
     * la consulta se escribe a mano en JPQL (se consultan entidades, no tablas).
     */
    @Query("select n from Notificacion n where n.usuario.id = :usuarioId and n.leido = false")
    List<Notificacion> findByUsuarioIdYNoLeidas(@Param("usuarioId") Long usuarioId);
}
