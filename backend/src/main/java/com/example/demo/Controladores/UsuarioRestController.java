package com.example.demo.Controladores;

import com.example.demo.Entidades.Usuario;
import com.example.demo.Entidades.Rol;
import com.example.demo.DTO.ErrorRespuestaDTO;
import com.example.demo.DTO.UsuarioDTO;
import com.example.demo.Servicios.UsuarioService;

import java.net.URI;
import java.util.List;

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
import org.springframework.web.bind.annotation.RestController;

/**
 * Controlador REST de la entidad Usuario.
 *
 * Nunca devuelve la entidad Usuario directamente: siempre la convierte a
 * UsuarioDTO, asi el JSON no incluye el password ni las relaciones.
 *
 * Cada endpoint devuelve ResponseEntity para poder elegir el codigo HTTP:
 *   200 OK         -> consulta o actualizacion correcta
 *   201 Created    -> usuario creado
 *   204 No Content -> usuario eliminado
 *   404 Not Found  -> el id no existe
 *   409 Conflict   -> el correo ya esta registrado
 *   400 Bad Request-> regla de negocio (ej: falta la contrasena), la responde ManejadorErroresApi
 */
// Marca esta clase como controlador que devuelve datos, normalmente JSON.
@RestController
// Agrega esta ruta base a todos los endpoints de esta clase.
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioRestController {

    // Servicio que contiene las operaciones de negocio de usuarios.
    private final UsuarioService usuarioService;

    // Spring entrega el servicio al crear el controlador.
    public UsuarioRestController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    // GET /api/usuarios -> 200 con todos los usuarios.
    @GetMapping
    public ResponseEntity<List<UsuarioDTO>> listar() {
        List<UsuarioDTO> usuarios = usuarioService.listar().stream()
                .map(UsuarioDTO::desde)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    // GET /api/usuarios/{id} -> 200 con el usuario o 404 si no existe.
    @GetMapping("/{id}")
    public ResponseEntity<?> buscarPorId(@PathVariable Long id) {
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return noEncontrado(id);
        }
        return ResponseEntity.ok(UsuarioDTO.desde(usuario));
    }

    // POST /api/usuarios -> 201 si se crea, 409 si el correo ya existe.
    @PostMapping
    public ResponseEntity<?> crear(@RequestBody Usuario usuario) {
        if (correoOcupado(usuario.getEmail(), null)) {
            return correoRepetido(usuario.getEmail());
        }
        // Al crear, el id siempre lo genera la base de datos.
        usuario.setId(null);
        Usuario guardado = usuarioService.guardar(usuario);
        return ResponseEntity
                .created(URI.create("/api/usuarios/" + guardado.getId()))
                .body(UsuarioDTO.desde(guardado));
    }

    // PUT /api/usuarios/{id} -> 200 si se actualiza, 404 si no existe, 409 si el correo es de otro.
    @PutMapping("/{id}")
    public ResponseEntity<?> actualizar(
            @PathVariable Long id,
            @RequestBody Usuario usuario) {
        // Confirma que el ID exista; de lo contrario, no debe crear otro usuario.
        if (usuarioService.buscarPorId(id) == null) {
            return noEncontrado(id);
        }
        if (correoOcupado(usuario.getEmail(), id)) {
            return correoRepetido(usuario.getEmail());
        }
        // Usa el ID de la URL y no uno que pudiera venir en el JSON.
        usuario.setId(id);
        // Guarda los cambios y devuelve el DTO sin exponer el password.
        return ResponseEntity.ok(UsuarioDTO.desde(usuarioService.guardar(usuario)));
    }

    // DELETE /api/usuarios/{id} -> 204 si se elimina, 404 si no existe.
    @DeleteMapping("/{id}")
    public ResponseEntity<?> eliminar(@PathVariable Long id) {
        if (usuarioService.buscarPorId(id) == null) {
            return noEncontrado(id);
        }
        usuarioService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    // GET /api/usuarios/rol/{rol} -> filtra por CLIENTE o ADMINISTRADOR.
    @GetMapping("/rol/{rol}")
    public ResponseEntity<List<UsuarioDTO>> listarPorRol(@PathVariable Rol rol) {
        List<UsuarioDTO> usuarios = usuarioService.listarPorRol(rol).stream()
                .map(UsuarioDTO::desde)
                .toList();
        return ResponseEntity.ok(usuarios);
    }

    // ---------------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------------

    /** true si el correo ya lo tiene otro usuario distinto de idActual. */
    private boolean correoOcupado(String email, Long idActual) {
        if (email == null || email.isBlank()) {
            return false;
        }
        Usuario conEseCorreo = usuarioService.buscarPorEmail(email.trim());
        return conEseCorreo != null && !conEseCorreo.getId().equals(idActual);
    }

    private ResponseEntity<ErrorRespuestaDTO> noEncontrado(Long id) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorRespuestaDTO(404, "El usuario con id " + id + " no existe"));
    }

    private ResponseEntity<ErrorRespuestaDTO> correoRepetido(String email) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorRespuestaDTO(409, "Ya existe un usuario con el correo " + email));
    }
}
