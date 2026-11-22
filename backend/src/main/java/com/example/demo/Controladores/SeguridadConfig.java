package com.example.demo.Controladores;

import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
public class SeguridadConfig {

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
            .cors(Customizer.withDefaults())
            .authorizeHttpRequests(auth -> auth
                // Recursos que se pueden visitar sin iniciar sesion.
                .requestMatchers("/", "/login", "/registro", "/error", "/error/**",
                    "/css/**", "/js/**", "/img/**", "/favicon.ico",
                    "/swagger-ui.html", "/swagger-ui/**", "/v3/api-docs", "/v3/api-docs/**", "/v3/api-docs.yaml",
                    "/api/auth/csrf").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/espacios", "/api/espacios/**").permitAll()
                // Primero se permite consultar canchas; modificarlas exige ADMIN.
                .requestMatchers("/api/usuarios", "/api/usuarios/**",
                    "/api/espacios", "/api/espacios/**", "/usuarios", "/usuarios/**",
                    "/espacios/add", "/espacios/update/**", "/espacios/delete/**").hasRole("ADMIN")
                .anyRequest().authenticated()
            )
            .formLogin(form -> form
                .loginPage("/login")
                .usernameParameter("email")
                .passwordParameter("password")
                // Angular pide JSON; las paginas reciben una redireccion.
                .successHandler((request, response, auth) -> {
                    if (pideJson(request)) responderJson(response, 200, "Sesion iniciada");
                    else response.sendRedirect("/perfil");
                })
                .failureHandler((request, response, error) -> {
                    if (pideJson(request)) responderJson(response, 401, "Credenciales incorrectas");
                    else response.sendRedirect("/login?error=true");
                })
            )
            .logout(logout -> logout
                .logoutUrl("/logout")
                .deleteCookies("JSESSIONID")
                .logoutSuccessHandler((request, response, auth) -> {
                    if (pideJson(request)) response.setStatus(204);
                    else response.sendRedirect("/login?logout=true");
                })
            )
            .exceptionHandling(errors -> errors
                .authenticationEntryPoint((request, response, error) -> {
                    if (pideJson(request)) responderJson(response, 401, "Debes iniciar sesion");
                    else response.sendRedirect("/login");
                })
                .accessDeniedHandler((request, response, error) -> {
                    if (pideJson(request)) responderJson(response, 403, "Acceso denegado");
                    else response.sendError(403);
                })
            );
        // CSRF permanece activo, como en la guia de clase.
        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public InMemoryUserDetailsManager usuariosEnMemoria(PasswordEncoder encoder) {
        UserDetails laura = User.withUsername("laura@correo.com")
            .password(encoder.encode("cliente123"))
            .roles("USER")
            .build();

        UserDetails carlos = User.withUsername("carlos@correo.com")
            .password(encoder.encode("cliente123"))
            .roles("USER")
            .build();

        UserDetails admin = User.withUsername("admin@canchafacil.com")
            .password(encoder.encode("admin123"))
            .roles("ADMIN")
            .build();

        return new InMemoryUserDetailsManager(laura, carlos, admin);
    }

    // Necesario cuando Angular y Spring se ejecutan en origenes diferentes.
    @Bean
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendUrl.replaceAll("/+$", ""), "http://localhost:4200", "http://localhost"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Content-Type", "Accept", "X-CSRF-TOKEN"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/api/**", config);
        source.registerCorsConfiguration("/login", config);
        source.registerCorsConfiguration("/logout", config);
        return source;
    }

    // Las rutas API siempre responden JSON; en login/logout Angular envia Accept.
    private boolean pideJson(HttpServletRequest request) {
        String accept = request.getHeader("Accept");
        return request.getServletPath().startsWith("/api/")
            || (accept != null && accept.contains("application/json"));
    }

    private void responderJson(HttpServletResponse response, int status, String mensaje)
            throws java.io.IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"mensaje\":\"" + mensaje + "\"}");
    }
}
