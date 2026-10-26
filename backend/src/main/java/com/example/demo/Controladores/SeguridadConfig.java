package com.example.demo.Controladores;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Configuracion de Spring Security.
 *
 * - La portada (/), /login y /registro son publicos; el resto de paginas Thymeleaf pide sesion.
 * - /usuarios/** (CRUD de usuarios) solo lo ve el rol ADMINISTRADOR.
 * - /api/** queda abierto y sin CSRF para no romper el frontend Angular,
 *   que se ajustara mas adelante.
 * - Las contrasenas se guardan con BCrypt.
 *
 * El UserDetailsService (UsuarioDetallesService) y el PasswordEncoder de abajo
 * los detecta Spring Boot solo y arma con ellos el proceso de autenticacion.
 */
@Configuration
@EnableWebSecurity
public class SeguridadConfig {

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
            // El API REST no usa sesion de formulario, por eso se excluye.
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/**"));

        return http.build();
    }
}
