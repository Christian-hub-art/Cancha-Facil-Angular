package com.example.demo.pagos;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Una compra que se paga con Wompi. Tabla "ordenes".
 *
 * Se crea en estado PENDING cuando el usuario presiona "Pagar con Wompi".
 * El estado final (APPROVED, DECLINED, VOIDED o ERROR) lo pone el webhook
 * cuando Wompi avisa como termino la transaccion.
 */
@Entity
@Table(name = "ordenes")
@Getter
@Setter
@NoArgsConstructor
public class Orden {

    public static final String PENDIENTE = "PENDING";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codigo unico que comparten la orden y Wompi, ej. ORD-3f2a... */
    @Column(nullable = false, unique = true, length = 60)
    private String referencia;

    /** Wompi trabaja en centavos: $15.000 = 1500000. */
    @Column(name = "monto_en_centavos", nullable = false)
    private Long montoEnCentavos;

    @Column(nullable = false, length = 3)
    private String moneda = "COP";

    /** Que se esta pagando, ej. "1 hora en Cancha Bosque 1". */
    @Column(length = 200)
    private String descripcion;

    /** Estado de la transaccion segun Wompi. */
    @Column(nullable = false, length = 20)
    private String estado = PENDIENTE;

    /** Id de la transaccion en Wompi; llega con el webhook. */
    @Column(name = "transaccion_id", length = 60)
    private String transaccionId;

    @Column(name = "creada_en", nullable = false)
    private LocalDateTime creadaEn;

    @Column(name = "actualizada_en")
    private LocalDateTime actualizadaEn;

    @PrePersist
    void alCrear() {
        creadaEn = LocalDateTime.now();
        actualizadaEn = creadaEn;
    }

    @PreUpdate
    void alActualizar() {
        actualizadaEn = LocalDateTime.now();
    }
}
