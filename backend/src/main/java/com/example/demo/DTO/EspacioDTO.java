package com.example.demo.DTO;

import java.math.BigDecimal;

import com.example.demo.Entidades.Espacio;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * DTO de la API REST de espacios (canchas).
 *
 * Es el JSON que se acordo con el frontend Angular (espacio.model.ts):
 * { id, nombre, tipoDeporte, precioHora, capacidad, descripcion, negocioId, negocioNombre }
 *
 * No se envia la entidad Espacio directamente porque tiene relaciones
 * (negocio, reservas, calificaciones) que apuntan de vuelta al espacio y
 * producirian un JSON infinito. En lugar del objeto Negocio completo solo se
 * envia su id y su nombre.
 *
 * Las anotaciones de validacion se revisan con @Valid en el controlador: si
 * algo no cumple, el backend responde 400 con el mensaje.
 */
public record EspacioDTO(
        Long id,

        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no puede superar 150 caracteres")
        String nombre,

        @NotBlank(message = "El tipo de deporte es obligatorio")
        @Size(max = 60, message = "El tipo de deporte no puede superar 60 caracteres")
        String tipoDeporte,

        @NotNull(message = "El precio por hora es obligatorio")
        @DecimalMin(value = "0.0", message = "El precio por hora no puede ser negativo")
        BigDecimal precioHora,

        @Min(value = 1, message = "La capacidad minima es 1 persona")
        @Max(value = 100, message = "La capacidad maxima es 100 personas")
        Integer capacidad,

        @Size(max = 500, message = "La descripcion no puede superar 500 caracteres")
        String descripcion,

        @NotNull(message = "El negocio es obligatorio")
        Long negocioId,

        // Solo de salida: el frontend lo puede mostrar, pero no hace falta enviarlo.
        String negocioNombre) {

    /** Entidad -> DTO: lo que se responde como JSON. */
    public static EspacioDTO desde(Espacio espacio) {
        if (espacio == null) {
            return null;
        }
        Long negocioId = espacio.getNegocio() == null ? null : espacio.getNegocio().getId();
        String negocioNombre = espacio.getNegocio() == null ? null : espacio.getNegocio().getNombre();
        return new EspacioDTO(
                espacio.getId(),
                espacio.getNombre(),
                espacio.getTipoDeporte(),
                espacio.getPrecioHora(),
                espacio.getCapacidad(),
                espacio.getDescripcion(),
                negocioId,
                negocioNombre);
    }

    /**
     * DTO -> entidad: lo que llega en el POST/PUT se pasa al servicio.
     * El negocio no se arma aqui: el servicio lo busca por negocioId.
     */
    public Espacio aEntidad() {
        Espacio espacio = new Espacio();
        espacio.setId(id);
        espacio.setNombre(nombre == null ? null : nombre.trim());
        espacio.setTipoDeporte(tipoDeporte);
        espacio.setPrecioHora(precioHora);
        espacio.setCapacidad(capacidad);
        espacio.setDescripcion(descripcion);
        return espacio;
    }
}
