package com.example.demo.Controladores;

import java.math.BigDecimal;
import java.net.URI;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.web.PagedModel;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.EspacioDTO;
import com.example.demo.DTO.ErrorRespuestaDTO;
import com.example.demo.Entidades.Espacio;
import com.example.demo.Servicios.EspacioService;

import jakarta.validation.Valid;

/**
 * API REST de espacios (canchas) que consume Angular (EspacioService).
 *
 *   GET    /api/espacios?page=0&size=5   -> pagina de espacios (paginacion)
 *   GET    /api/espacios/{id}            -> un espacio (200) o 404
 *   GET    /api/espacios/precio?min&max  -> consulta JPQL por rango de precio
 *   POST   /api/espacios                 -> crea (201) o 400 si los datos no sirven
 *   PUT    /api/espacios/{id}            -> actualiza (200) o 404
 *   DELETE /api/espacios/{id}            -> elimina (204) o 404
 *
 * Todos los endpoints devuelven ResponseEntity para controlar el codigo HTTP.
 * Las excepciones que no se atrapan aqui las convierte ManejadorErroresApi.
 */
@RestController
@RequestMapping("/api/espacios")
@CrossOrigin(origins = "http://localhost:4200")
public class EspacioRestController {

    private final EspacioService espacioService;

    public EspacioRestController(EspacioService espacioService) {
        this.espacioService = espacioService;
    }

    /**
     * GET paginado. Spring arma el Pageable con los parametros ?page=&size=
     * (por defecto pagina 0, 5 por pagina, ordenado por id).
     *
     * PagedModel le da al JSON la forma acordada con Angular:
     * { content: [...], page: { size, number, totalElements, totalPages } }
     */
    @GetMapping
    public ResponseEntity<PagedModel<EspacioDTO>> listar(
            @PageableDefault(size = 5, sort = "id", direction = Sort.Direction.ASC) Pageable pageable) {
        Page<EspacioDTO> pagina = espacioService.listarPaginado(pageable).map(EspacioDTO::desde);
        return ResponseEntity.ok(new PagedModel<>(pagina));
    }

    /** GET por id: 200 con el espacio, o 404 si no existe. */
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Espacio espacio = espacioService.buscarPorId(id);
        if (espacio == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuestaDTO(404, "El espacio con id " + id + " no existe"));
        }
        return ResponseEntity.ok(EspacioDTO.desde(espacio));
    }

    /**
     * GET con consulta JPQL (EspacioRepository.buscarPorRangoDePrecio):
     * /api/espacios/precio?min=30000&max=90000
     */
    @GetMapping("/precio")
    public ResponseEntity<List<EspacioDTO>> buscarPorRangoDePrecio(
            @RequestParam(required = false) BigDecimal min,
            @RequestParam(required = false) BigDecimal max) {
        List<EspacioDTO> espacios = espacioService.buscarPorRangoDePrecio(min, max).stream()
                .map(EspacioDTO::desde)
                .toList();
        return ResponseEntity.ok(espacios);
    }

    /**
     * POST: crea un espacio. @Valid revisa las anotaciones del DTO; si falla,
     * ManejadorErroresApi responde 400. Si sale bien responde 201 Created con
     * la cabecera Location apuntando al nuevo recurso.
     */
    @PostMapping
    public ResponseEntity<EspacioDTO> crear(@Valid @RequestBody EspacioDTO dto) {
        Espacio espacio = dto.aEntidad();
        espacio.setId(null); // al crear, el id lo genera PostgreSQL
        Espacio guardado = espacioService.guardar(espacio, dto.negocioId());
        return ResponseEntity
                .created(URI.create("/api/espacios/" + guardado.getId()))
                .body(EspacioDTO.desde(guardado));
    }

    /** PUT: 404 si el id no existe (para no crear uno nuevo por error); 200 si se actualiza. */
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(@PathVariable Long id, @Valid @RequestBody EspacioDTO dto) {
        if (espacioService.buscarPorId(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuestaDTO(404, "El espacio con id " + id + " no existe"));
        }
        Espacio espacio = dto.aEntidad();
        espacio.setId(id); // se usa el id de la URL, no el que venga en el JSON
        Espacio guardado = espacioService.guardar(espacio, dto.negocioId());
        return ResponseEntity.ok(EspacioDTO.desde(guardado));
    }

    /** DELETE: 204 No Content si se elimina, 404 si no existe. */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (espacioService.buscarPorId(id) == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(new ErrorRespuestaDTO(404, "El espacio con id " + id + " no existe"));
        }
        espacioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
