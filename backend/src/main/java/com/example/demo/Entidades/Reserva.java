package com.example.demo.Entidades;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Entity
@Table(name = "reservas")
@Data
@ToString(exclude = {"usuario", "espacio", "pago", "calificacion"})
@EqualsAndHashCode(exclude = {"usuario", "espacio", "pago", "calificacion"})
@NoArgsConstructor
@AllArgsConstructor
public class Reserva {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "espacio_id", nullable = false)
    private Espacio espacio;

    // Convierte el valor de <input type="date"> (2026-08-24) a LocalDate
    @NotNull(message = "La fecha es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate fecha;

    // Convierte el valor de <input type="time"> (18:00) a LocalTime
    @NotNull(message = "La hora de inicio es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Column(name = "hora_inicio", nullable = false)
    private LocalTime horaInicio;

    @NotNull(message = "La hora de fin es obligatoria")
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    @Column(name = "hora_fin", nullable = false)
    private LocalTime horaFin;

    @Column(length = 20)
    private String estado;

    @Column(name = "fecha_creacion")
    private LocalDateTime fechaCreacion;

    // Lado inverso: la columna reserva_id vive en las tablas pagos y calificaciones.
    // El cascade hace que al borrar la reserva se borren tambien su pago y su
    // calificacion, en vez de fallar por la llave foranea.
    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Pago pago;

    @OneToOne(mappedBy = "reserva", cascade = CascadeType.ALL)
    private Calificacion calificacion;
}
