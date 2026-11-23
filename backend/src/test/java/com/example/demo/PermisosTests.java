package com.example.demo;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.CookieManager;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import com.example.demo.Entidades.Espacio;
import com.example.demo.Repositorios.EspacioRepository;
import com.example.demo.Repositorios.NegocioRepository;

/**
 * Pruebas de la Persona 3: permisos reales por rol.
 *
 * Demuestran que el servidor bloquea las operaciones aunque la peticion se
 * envie a mano (sin pasar por Angular) y que el rechazo es por rol y no por
 * CSRF: el USER envia un token CSRF valido y aun asi recibe 403.
 *
 * Se trabaja con una cancha temporal creada por el propio test, para no borrar
 * datos de demostracion.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PermisosTests {

    private static final String LAURA = "laura@correo.com";
    private static final String ADMIN = "admin@canchafacil.com";

    @Value("${local.server.port}")
    int port;

    @Autowired
    EspacioRepository espacios;

    @Autowired
    NegocioRepository negocios;

    // ---------------------------------------------------------------------
    // Casos
    // ---------------------------------------------------------------------

    /** Caso: visitante consulta el catalogo publico -> 200 sin sesion. */
    @Test
    void visitanteConsultaCatalogo() throws Exception {
        HttpClient visitante = navegador();
        assertEquals(200, pedir(visitante, "GET", "/api/espacios", null, null).statusCode());
        assertEquals(200, pedir(visitante, "GET", "/api/espacios/precio?min=0&max=1000000", null, null).statusCode());
    }

    /** Caso: visitante intenta rutas protegidas -> 401 y no recibe datos privados. */
    @Test
    void visitanteNoEntraARutasProtegidas() throws Exception {
        HttpClient visitante = navegador();
        String token = token(visitante);

        HttpResponse<String> usuarios = pedir(visitante, "GET", "/api/usuarios", null, null);
        assertEquals(401, usuarios.statusCode());
        assertTrue(!usuarios.body().contains("@"), "Un visitante no debe recibir correos de usuarios");

        long antes = espacios.count();
        assertEquals(401, pedir(visitante, "POST", "/api/espacios", canchaJson("Visitante"), token).statusCode());
        assertEquals(antes, espacios.count());
    }

    /**
     * Caso central de la sustentacion: la misma operacion (eliminar una cancha)
     * se le niega a USER con 403 y se le permite a ADMIN con 204.
     */
    @Test
    void userRecibe403YAdminPuedeEliminar() throws Exception {
        HttpClient admin = iniciarSesion(ADMIN, "admin123");
        HttpClient user = iniciarSesion(LAURA, "cliente123");

        // ADMIN crea una cancha temporal -> 201.
        HttpResponse<String> creada = pedir(admin, "POST", "/api/espacios",
                canchaJson("Cancha temporal permisos"), token(admin));
        assertEquals(201, creada.statusCode(), creada.body());
        Long id = Long.valueOf(creada.body().replaceAll("(?s).*\"id\":(\\d+).*", "$1"));

        // USER, con sesion y CSRF valido, intenta editarla y eliminarla -> 403.
        assertEquals(403, pedir(user, "PUT", "/api/espacios/" + id,
                canchaJson("Cambiada por USER"), token(user)).statusCode());
        assertEquals(403, pedir(user, "DELETE", "/api/espacios/" + id, null, token(user)).statusCode());

        // Los datos quedan intactos.
        Espacio intacta = espacios.findById(id).orElseThrow();
        assertEquals("Cancha temporal permisos", intacta.getNombre());

        // USER tampoco puede crear canchas ni administrar usuarios.
        long antes = espacios.count();
        assertEquals(403, pedir(user, "POST", "/api/espacios", canchaJson("Creada por USER"), token(user)).statusCode());
        assertEquals(antes, espacios.count());
        assertEquals(403, pedir(user, "GET", "/api/usuarios", null, null).statusCode());

        // ADMIN si puede eliminarla -> 204, y ya no existe.
        assertEquals(204, pedir(admin, "DELETE", "/api/espacios/" + id, null, token(admin)).statusCode());
        assertTrue(espacios.findById(id).isEmpty());
    }

    /** Las rutas Thymeleaf equivalentes no sirven para saltarse la restriccion. */
    @Test
    void rutasThymeleafAdministrativasRechazanUser() throws Exception {
        HttpClient user = iniciarSesion(LAURA, "cliente123");
        long canchas = espacios.count();
        long negociosAntes = negocios.count();

        for (String ruta : new String[] {
                "/usuarios", "/espacios/add", "/espacios/delete/1", "/negocios/delete/1",
                "/pagos/delete/1", "/pagos/estado/1", "/notificaciones/add",
                "/notificaciones/delete/1", "/calificaciones/delete/1", "/reservas/delete/1"}) {
            assertEquals(403, pedir(user, "GET", ruta, null, null).statusCode(), ruta);
        }
        assertEquals(canchas, espacios.count());
        assertEquals(negociosAntes, negocios.count());

        // Lo que si es de un cliente sigue funcionando.
        assertEquals(200, pedir(user, "GET", "/perfil", null, null).statusCode());
        assertEquals(200, pedir(user, "GET", "/espacios", null, null).statusCode());
    }

    // ---------------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------------

    private HttpClient navegador() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }

    /** Peticion con la cookie de sesion del cliente; cuerpo JSON si se envia. */
    private HttpResponse<String> pedir(HttpClient cliente, String metodo, String ruta,
                                       String json, String tokenCsrf) throws Exception {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta))
                // Acepta HTML (paginas Thymeleaf) y JSON; con JSON los rechazos de
                // seguridad llegan como {"mensaje": ...} en lugar de redirecciones.
                .header("Accept", "text/html,application/json;q=0.9");
        if (tokenCsrf != null) {
            builder.header("X-CSRF-TOKEN", tokenCsrf);
        }
        if (json != null) {
            builder.header("Content-Type", "application/json")
                    .method(metodo, HttpRequest.BodyPublishers.ofString(json));
        } else {
            builder.method(metodo, HttpRequest.BodyPublishers.noBody());
        }
        return cliente.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    /** Token CSRF vigente para la sesion de este cliente (Spring lo renueva al hacer login). */
    private String token(HttpClient cliente) throws Exception {
        HttpResponse<String> respuesta = pedir(cliente, "GET", "/api/auth/csrf", null, null);
        assertEquals(200, respuesta.statusCode());
        return respuesta.body().split("\"token\":\"")[1].split("\"")[0];
    }

    private HttpClient iniciarSesion(String email, String clave) throws Exception {
        HttpClient cliente = navegador();
        String formulario = "email=" + URLEncoder.encode(email, StandardCharsets.UTF_8)
                + "&password=" + URLEncoder.encode(clave, StandardCharsets.UTF_8);
        HttpResponse<String> respuesta = cliente.send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
                        .header("Accept", "application/json")
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .header("X-CSRF-TOKEN", token(cliente))
                        .POST(HttpRequest.BodyPublishers.ofString(formulario))
                        .build(),
                HttpResponse.BodyHandlers.ofString());
        assertEquals(200, respuesta.statusCode(), "Login de " + email);
        return cliente;
    }

    private String canchaJson(String nombre) {
        Long negocioId = negocios.findAll().get(0).getId();
        return """
                {"nombre":"%s","tipoDeporte":"futbol 5","precioHora":50000,
                 "capacidad":10,"descripcion":"Creada por PermisosTests","negocioId":%d}
                """.formatted(nombre, negocioId);
    }
}
