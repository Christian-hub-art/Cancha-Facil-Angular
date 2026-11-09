package com.example.demo.Controladores;

import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

/**
 * Configuracion de Spring Security.
 *
 * - La portada (/), /login y /registro son publicos; el resto de paginas Thymeleaf pide sesion.
 * - /usuarios/** (CRUD de usuarios) solo lo ve el rol ADMINISTRADOR.
 * - /api/** queda abierto y sin CSRF para el frontend Angular. Incluye
 *   /api/pagos/webhook, al que Wompi llama sin sesion ni token CSRF.
 * - Swagger UI (/swagger-ui/**) y OpenAPI (/v3/api-docs/**) son publicos para
 *   poder documentar y probar el API REST.
 * - CORS: el API acepta peticiones del frontend configurado en app.frontend-url
 *   (variable FRONTEND_URL), ademas de Angular en local (4200) y en Docker (puerto 80).
 * - Las contrasenas se guardan con BCrypt.
 *
 * El UserDetailsService (UsuarioDetallesService) y el PasswordEncoder de abajo
 * los detecta Spring Boot solo y arma con ellos el proceso de autenticacion.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

    // URL del frontend Angular; en Render se toma de la variable FRONTEND_URL.
    @Value("${app.frontend-url:http://localhost:4200}")
    private String frontendUrl;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/login", "/registro", "/error", "/error/**",
                        "/css/**", "/js/**", "/img/**", "/favicon.ico").permitAll()
                .requestMatchers("/api/**").permitAll()
                // Swagger UI y la definicion OpenAPI (JSON/YAML) de springdoc.
                // Sin esto, anyRequest().authenticated() redirige al login.
                .requestMatchers("/swagger-ui.html", "/swagger-ui/**",
                        "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml").permitAll()
                .requestMatchers("/usuarios/**").hasRole("ADMINISTRADOR")
                .anyRequest().authenticated())
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                .defaultSuccessUrl("/perfil")
                .failureUrl("/login?error=true")
                .permitAll())
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout=true")
                .permitAll())
            // Thymeleaf agrega el token CSRF solo en los formularios con th:action.
            // El API REST (y el webhook de Wompi) no usan sesion de formulario, por eso se excluye.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }

    /**
     * Origenes que pueden llamar al API desde el navegador. .cors(...) de arriba
     * usa este bean. Wompi no pasa por aqui: llama al webhook desde su servidor.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
                quitarBarraFinal(frontendUrl),
                "http://localhost:4200",
                "http://localhost"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        return source;
    }

    private String quitarBarraFinal(String url) {
        return url.endsWith("/") ? url.substring(0, url.length() - 1) : url;
    }
}
