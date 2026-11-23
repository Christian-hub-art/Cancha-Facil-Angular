package com.example.demo.Servicios;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Usuario;
import com.example.demo.Repositorios.UsuarioRepository;

/**
 * Alternativa de autenticacion con BD, sin registrar como bean.
 * Esta entrega usa exclusivamente usuariosEnMemoria de SeguridadConfig.
 * Traduce CLIENTE a USER y ADMINISTRADOR a ADMIN.
 */
public class UsuarioDetallesService implements UserDetailsService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioDetallesService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email);
        if (usuario == null) {
            throw new UsernameNotFoundException("No existe un usuario con el email " + email);
        }
        return User.withUsername(usuario.getEmail())
                .password(usuario.getPassword())
                .roles(usuario.getRol() == com.example.demo.Entidades.Rol.ADMINISTRADOR ? "ADMIN" : "USER")
                .build();
    }
}
