package com.example.demo.Controladores;

import com.example.demo.Entidades.Usuario;
import com.example.demo.Entidades.Rol;
import com.example.demo.DTO.UsuarioDTO;
import com.example.demo.Servicios.UsuarioService;
import java.util.List;

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

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Controlador REST de la entidad Usuario. Expone los servicios de la API para
 * consultar usuarios.
 *
 * Nunca devuelve la entidad Usuario directamente: siempre la convierte a
 * UsuarioDTO, asi el JSON no incluye el password ni las relaciones .
 *
 * Las anotaciones @Tag, @Operation, @ApiResponse y @Parameter (springdoc /
 * Swagger) no cambian el comportamiento: solo describen cada endpoint en
 * Swagger UI (http://localhost:8080/swagger-ui/index.html).
 */
// Agrupa todos los endpoints de esta clase bajo "Usuarios" en Swagger UI.
@Tag(name = "Usuarios", description = "Operaciones de consulta y administracion de usuarios")
// Marca esta clase como controlador que devuelve datos, normalmente JSON.
@RestController
// Agrega esta ruta base a todos los endpoints de esta clase.
@RequestMapping("/api/usuarios")
@CrossOrigin(origins = "http://localhost:4200")
public class UsuarioRestController {

    // JSON de ejemplo que Swagger UI precarga en "Try it out" para POST y PUT.
    // Solo incluye los campos que el backend espera; las listas (reservas, etc.) se omiten.
    private static final String EJEMPLO_USUARIO = """
            {
              "nombre": "Usuario de prueba",
              "email": "prueba@example.com",
              "password": "prueba123",
              "telefono": "3001234567",
              "rol": "CLIENTE",
              "direccion": "Calle 40 # 5-50"
            }
            """;

    // Servicio que contiene las operaciones de negocio de usuarios.
    private final UsuarioService usuarioService;

    // Spring entrega el servicio al crear el controlador.
    public UsuarioRestController(UsuarioService usuarioService) {
        // Guarda el servicio para usarlo en los endpoints.
        this.usuarioService = usuarioService;
    }

    // Atiende GET /api/usuarios para consultar todos los usuarios.
    @Operation(summary = "Listar usuarios",
            description = "Consulta todos los usuarios registrados. No incluye la contrasena.")
    @ApiResponse(responseCode = "200", description = "Consulta exitosa")
    @GetMapping
    public List<UsuarioDTO> listar() {
        // Obtiene los usuarios, convierte cada uno a DTO y arma la lista de respuesta.
        return usuarioService.listar().stream()
                .map(UsuarioDTO::desde)
                .toList();
    }

    // Atiende GET /api/usuarios/{id}; toma el id desde la URL.
    @Operation(summary = "Consultar usuario por identificador")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario encontrado"),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content)
    })
    @GetMapping("/{id}")
    public ResponseEntity<UsuarioDTO> buscarPorId(
            @Parameter(description = "ID del usuario", example = "1")
            @PathVariable Long id) {
        // Busca el usuario; si no existe responde 404 en vez de un 200 vacio.
        Usuario usuario = usuarioService.buscarPorId(id);
        if (usuario == null) {
            return ResponseEntity.notFound().build();
        }
        // Convierte el resultado a DTO antes de responder.
        return ResponseEntity.ok(UsuarioDTO.desde(usuario));
    }

    // Atiende POST /api/usuarios para crear un usuario.
    @Operation(summary = "Crear usuario",
            description = "Registra un usuario nuevo. El email no se puede repetir y la contrasena es obligatoria.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario creado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o email repetido",
                    content = @Content)
    })
    @PostMapping
    public UsuarioDTO crear(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Datos del usuario a crear",
                    content = @Content(examples = @ExampleObject(value = EJEMPLO_USUARIO)))
            @RequestBody Usuario usuario) {
        // Convierte el JSON recibido en Usuario; el servicio lo guarda y el DTO
        // evita incluir la contrasena cifrada en la respuesta.
        return UsuarioDTO.desde(usuarioService.guardar(usuario));
    }

    // Atiende PUT /api/usuarios/{id} para actualizar un usuario existente.
    @Operation(summary = "Actualizar usuario",
            description = "Modifica un usuario existente. Si la contrasena viene vacia se conserva la actual.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario actualizado"),
            @ApiResponse(responseCode = "400", description = "Datos invalidos o email repetido",
                    content = @Content),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content)
    })
    @PutMapping("/{id}")
    public UsuarioDTO actualizar(
            @Parameter(description = "ID del usuario a actualizar", example = "1")
            @PathVariable Long id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    description = "Nuevos datos del usuario",
                    content = @Content(examples = @ExampleObject(value = EJEMPLO_USUARIO)))
            @RequestBody Usuario usuario) {
        // Confirma que el ID exista; de lo contrario, no debe crear otro usuario.
        usuarioService.obtenerPorId(id);
        // Usa el ID de la URL y no uno que pudiera venir en el JSON.
        usuario.setId(id);
        // Guarda los cambios y devuelve el DTO sin exponer el password.
        return UsuarioDTO.desde(usuarioService.guardar(usuario));
    }

    //Atiende DELETE /api/usuarios/{id} para eliminar un usuario existente.
    @Operation(summary = "Eliminar usuario",
            description = "Elimina el usuario y sus negocios asociados. Usar solo con datos de prueba.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Usuario eliminado"),
            @ApiResponse(responseCode = "404", description = "No existe un usuario con ese id",
                    content = @Content)
    })
    @DeleteMapping("/{id}")
    public void eliminar(
            @Parameter(description = "ID del usuario a eliminar", example = "1")
            @PathVariable Long id) {
        usuarioService.eliminar(id);
    }

    // Atiende GET /api/usuarios/rol/{rol} para filtrar por CLIENTE o ADMINISTRADOR.
    @Operation(summary = "Listar usuarios por rol")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Consulta exitosa"),
            @ApiResponse(responseCode = "400", description = "Rol no valido", content = @Content)
    })
    @GetMapping("/rol/{rol}")
    public List<UsuarioDTO> listarPorRol(
            @Parameter(description = "Rol a filtrar", example = "CLIENTE")
            @PathVariable Rol rol) {
        // Busca por rol y convierte los resultados a DTO antes de responder.
        return usuarioService.listarPorRol(rol).stream()
                .map(UsuarioDTO::desde)
                .toList();
    }

}
