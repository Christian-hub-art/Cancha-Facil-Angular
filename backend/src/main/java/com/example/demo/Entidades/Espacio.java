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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "espacios")
@Data
@ToString(exclude = {"negocio", "reservas", "calificaciones"})
@EqualsAndHashCode(exclude = {"negocio", "reservas", "calificaciones"})
@NoArgsConstructor
@AllArgsConstructor
public class Espacio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "negocio_id", nullable = false)
    private Negocio negocio;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El tipo de deporte es obligatorio")
    @Size(max = 60, message = "El tipo de deporte no puede superar 60 caracteres")
    @Column(name = "tipo_deporte", length = 60)
    private String tipoDeporte;

    // precision/scale = NUMERIC(10,2) en PostgreSQL: dinero sin errores de redondeo.
    @NotNull(message = "El precio por hora es obligatorio")
    @DecimalMin(value = "0.0", message = "El precio por hora no puede ser negativo")
    @Digits(integer = 8, fraction = 2, message = "El precio admite hasta 8 enteros y 2 decimales")
    @Column(name = "precio_hora", nullable = false, precision = 10, scale = 2)
    private BigDecimal precioHora;

    @Min(value = 1, message = "La capacidad minima es 1 persona")
    @Max(value = 100, message = "La capacidad maxima es 100 personas")
    private Integer capacidad;

    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    @Column(length = 500)
    private String descripcion;

    @OneToMany(mappedBy = "espacio", cascade = CascadeType.ALL)
    private List<Reserva> reservas = new ArrayList<>();

    @OneToMany(mappedBy = "espacio", cascade = CascadeType.ALL)
    private List<Calificacion> calificaciones = new ArrayList<>();
}
