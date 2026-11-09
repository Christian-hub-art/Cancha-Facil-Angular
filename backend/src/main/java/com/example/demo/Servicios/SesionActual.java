package com.example.demo.Servicios;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.demo.Entidades.Usuario;
import com.example.demo.Repositorios.NotificacionRepository;
import com.example.demo.Repositorios.UsuarioRepository;

/**
 * Da acceso al usuario que inicio sesion desde los controladores y desde las
 * plantillas. En Thymeleaf se usa como bean: ${@sesion.autenticado},
 * ${@sesion.admin}, ${@sesion.nombre}.
 */
@Component("sesion")
public class SesionActual {

    private final UsuarioRepository usuarioRepository;
    private final NotificacionRepository notificacionRepository;

    public SesionActual(UsuarioRepository usuarioRepository, NotificacionRepository notificacionRepository) {
        this.usuarioRepository = usuarioRepository;
        this.notificacionRepository = notificacionRepository;
    }

    public boolean isAutenticado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && !(auth instanceof AnonymousAuthenticationToken);
    }

    public boolean isAdmin() {
        if (!isAutenticado()) {
            return false;
        }
        return SecurityContextHolder.getContext().getAuthentication().getAuthorities().stream()
                .anyMatch(a -> "ROLE_ADMINISTRADOR".equals(a.getAuthority()));
    }

    public String getEmail() {
        return isAutenticado() ? SecurityContextHolder.getContext().getAuthentication().getName() : null;
    }

    /** Entidad Usuario de la sesion, o null si nadie ha iniciado sesion. */
    public Usuario getUsuario() {
        String email = getEmail();
        return email == null ? null : usuarioRepository.findByEmailIgnoreCase(email);
    }

    public String getNombre() {
        Usuario usuario = getUsuario();
        return usuario == null ? "" : usuario.getNombre();
    }

    /** Iniciales para el avatar: "David Morales" -> "DM". */
    public String getIniciales() {
        String nombre = getNombre().trim();
        if (nombre.isEmpty()) {
            return "?";
        }
        String[] partes = nombre.split("\\s+");
        String iniciales = partes[0].substring(0, 1);
        if (partes.length > 1) {
            iniciales += partes[partes.length - 1].substring(0, 1);
        }
        return iniciales.toUpperCase();
    }

    /** Cantidad de notificaciones sin leer del usuario en sesion (contador de la campana). */
    public int getNotificacionesSinLeer() {
        Usuario usuario = getUsuario();
        return usuario == null ? 0 : notificacionRepository.findByUsuarioIdYNoLeidas(usuario.getId()).size();
    }
}