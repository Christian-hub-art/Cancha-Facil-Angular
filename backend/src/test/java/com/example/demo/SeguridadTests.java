package com.example.demo;

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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import com.example.demo.Repositorios.UsuarioRepository;
import com.example.demo.Servicios.DatosIniciales;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SeguridadTests {
    @Value("${local.server.port}") int port;
    @Autowired InMemoryUserDetailsManager usuarios;
    @Autowired PasswordEncoder encoder;
    @Autowired UsuarioRepository repositorio;
    @Autowired DatosIniciales datos;

    HttpClient navegador() {
        return HttpClient.newBuilder().cookieHandler(new CookieManager()).build();
    }
    HttpResponse<String> pedir(HttpClient cliente, String metodo, String ruta, String cuerpo, String token)
            throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://localhost:" + port + ruta));
        if (ruta.equals("/logout") || (ruta.equals("/login") && !metodo.equals("GET"))) {
            builder.header("Accept", "application/json");
        }
        if (token != null) builder.header("X-CSRF-TOKEN", token);
        if (metodo.equals("GET")) builder.GET();
        else builder.header("Content-Type", "application/x-www-form-urlencoded")
            .method(metodo, HttpRequest.BodyPublishers.ofString(cuerpo));
        return cliente.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }
    String token(HttpClient cliente) throws Exception {
        var respuesta = pedir(cliente, "GET", "/api/auth/csrf", "", null);
        assertEquals(200, respuesta.statusCode());
        return respuesta.body().split("\"token\":\"")[1].split("\"")[0];
    }
    HttpResponse<String> login(HttpClient cliente, String email, String clave) throws Exception {
        return pedir(cliente, "POST", "/login", "email="
            + URLEncoder.encode(email, StandardCharsets.UTF_8) + "&password=" + clave, token(cliente));
    }

    @Test void cuentasRolesYSesionCompleta() throws Exception {
        for (String email : new String[]{"laura@correo.com", "carlos@correo.com", "admin@canchafacil.com"}) {
            boolean admin = email.startsWith("admin");
            String clave = admin ? "admin123" : "cliente123";
            assertTrue(usuarios.loadUserByUsername(email).getPassword().startsWith("$2"));
            assertTrue(encoder.matches(clave, usuarios.loadUserByUsername(email).getPassword()));
            assertNotNull(repositorio.findByEmailIgnoreCase(email));
            var cliente = navegador();
            assertEquals(200, login(cliente, email, clave).statusCode());
            var sesion = pedir(cliente, "GET", "/api/auth/me", "", null);
            assertEquals(200, sesion.statusCode());
            assertTrue(sesion.body().contains(email));
            assertTrue(sesion.body().contains(admin ? "ADMIN" : "USER"));
            assertFalse(sesion.body().contains("password"));
            assertEquals(admin ? 200 : 403, pedir(cliente, "GET", "/api/usuarios", "", null).statusCode());
            // CSRF valido: el rechazo es por rol, no por ausencia del token.
            if (!admin) assertEquals(403, pedir(cliente, "DELETE", "/api/espacios/999999", "", token(cliente)).statusCode());
            assertEquals(204, pedir(cliente, "POST", "/logout", "", token(cliente)).statusCode());
            assertEquals(401, pedir(cliente, "GET", "/api/auth/me", "", null).statusCode());
        }
    }
    @Test void publicoErroresCsrfYCors() throws Exception {
        var cliente = navegador();
        assertEquals(200, pedir(cliente, "GET", "/api/espacios", "", null).statusCode());
        assertEquals(401, pedir(cliente, "GET", "/api/usuarios", "", null).statusCode());
        var fallo = login(cliente, "laura@correo.com", "incorrecta");
        assertEquals(401, fallo.statusCode());
        assertTrue(fallo.headers().firstValue("content-type").orElse("").contains("application/json"));
        assertEquals(403, pedir(cliente, "POST", "/login", "email=x&password=x", null).statusCode());
        var cors = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
            .header("Origin", "http://localhost:4200").header("Access-Control-Request-Method", "POST")
            .method("OPTIONS", HttpRequest.BodyPublishers.noBody()).build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(200, cors.statusCode());
        assertEquals("true", cors.headers().firstValue("access-control-allow-credentials").orElse(""));
    }
    @Test void paginasConservanLoginYPermisos() throws Exception {
        var cliente = navegador();
        assertEquals(200, pedir(cliente, "GET", "/login", "", null).statusCode());
        String html = pedir(cliente, "GET", "/login", "", null).body();
        var matcher = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        assertTrue(matcher.find());
        var respuesta = cliente.send(HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/login"))
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("X-CSRF-TOKEN", matcher.group(1))
            .POST(HttpRequest.BodyPublishers.ofString("email=laura%40correo.com&password=cliente123"))
            .build(), HttpResponse.BodyHandlers.ofString());
        assertEquals(302, respuesta.statusCode());
        assertEquals(403, pedir(cliente, "GET", "/usuarios", "", null).statusCode());
        assertEquals(403, pedir(cliente, "GET", "/espacios/add", "", null).statusCode());
        assertEquals(200, pedir(cliente, "GET", "/perfil", "", null).statusCode());
    }
    @Test void datosRepetiblesConBasePoblada() {
        long cantidad = repositorio.count();
        datos.run();
        assertEquals(cantidad, repositorio.count());
    }
}
