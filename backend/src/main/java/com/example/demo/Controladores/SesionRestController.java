package com.example.demo.Controladores;

import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class SesionRestController {
    public record Sesion(String email, List<String> roles) {}
    public record Token(String headerName, String token) {}

    @GetMapping("/me")
    public Sesion actual(Authentication auth) {
        return new Sesion(auth.getName(), auth.getAuthorities().stream()
            .map(a -> a.getAuthority().replaceFirst("^ROLE_", "")).toList());
    } 

    @GetMapping("/csrf")
    public Token csrf(CsrfToken csrf) {
        return new Token(csrf.getHeaderName(), csrf.getToken());
    }
}
