package com.example.demo.Repositorios;

import com.example.demo.Entidades.Reserva;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface ReservaRepository extends JpaRepository<Reserva, Long> {

    List<Reserva> findByUsuarioId(Long usuarioId);

    List<Reserva> findByEspacioId(Long espacioId);

    List<Reserva> findByEstadoIgnoreCase(String estado);

    /** Las reservas de un espacio en un dia concreto: base del control de cruces. */
    List<Reserva> findByEspacioIdAndFecha(Long espacioId, LocalDate fecha);

    /**
     * Consulta personalizada (JPQL): reservas activas del mismo espacio y dia cuyo
     * horario se cruza con [horaInicio, horaFin). Dos rangos se cruzan cuando
     * uno empieza antes de que el otro termine y termina despues de que el otro empieza.
     * excluirId sirve para ignorar la propia reserva cuando se esta editando
     * (se manda -1 si es una reserva nueva).
     */
    @Query("select r from Reserva r "
            + "where r.espacio.id = :espacioId "
            + "and r.fecha = :fecha "
            + "and (r.estado is null or r.estado <> 'CANCELADA') "
            + "and r.horaInicio < :horaFin "
            + "and r.horaFin > :horaInicio "
            + "and r.id <> :excluirId")
    List<Reserva> buscarCruces(@Param("espacioId") Long espacioId,
                               @Param("fecha") LocalDate fecha,
                               @Param("horaInicio") LocalTime horaInicio,
                               @Param("horaFin") LocalTime horaFin,
                               @Param("excluirId") Long excluirId);

    /**
     * Consulta personalizada (JPQL): reservas del usuario que inicio sesion,
     * buscado por su email, de la mas reciente a la mas antigua.
     * "join fetch" trae el espacio en la misma consulta.
     */
    @Query("select r from Reserva r join fetch r.espacio "
            + "where lower(r.usuario.email) = lower(:email) "
            + "order by r.fecha desc, r.horaInicio desc")
    List<Reserva> buscarPorEmailDeUsuario(@Param("email") String email);

    /**
     * Consulta personalizada (JPQL con group by): cuantas reservas hay en cada estado.
     * Cada fila llega como Object[]{estado, cantidad}.
     */
    @Query("select r.estado, count(r) from Reserva r group by r.estado order by r.estado")
    List<Object[]> contarPorEstado();
}
