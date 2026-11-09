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

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "negocios")
@Data
@ToString(exclude = {"administrador", "espacios"})
@EqualsAndHashCode(exclude = {"administrador", "espacios"})
@NoArgsConstructor
@AllArgsConstructor
public class Negocio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Lado dueno de la relacion: la columna administrador_id se crea en esta tabla.
    // LAZY = el usuario solo se consulta cuando alguien llama a getAdministrador().
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "administrador_id", nullable = false)
    private Usuario administrador;

    @NotBlank(message = "El nombre es obligatorio")
    @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
    @Column(nullable = false, length = 150)
    private String nombre;

    @NotBlank(message = "El NIT es obligatorio")
    @Pattern(regexp = "^[0-9]{5,15}(-[0-9])?$", message = "El NIT solo admite numeros y un digito de verificacion (ej: 900123456-1)")
    @Column(nullable = false, unique = true, length = 30)
    private String nit;

    @Size(max = 200, message = "La direccion no puede superar 200 caracteres")
    @Column(length = 200)
    private String direccion;

    @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
    @Column(length = 500)
    private String descripcion;

    @OneToMany(mappedBy = "negocio", cascade = CascadeType.ALL)
    private List<Espacio> espacios = new ArrayList<>();
}
