package com.example.demo.Controladores;

import com.example.demo.Servicios.SesionActual;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.WebDataBinder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.InitBinder;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entidades.Calificacion;
import com.example.demo.Entidades.Reserva;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.CalificacionService;
import com.example.demo.Servicios.ReservaService;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

/**
 * "Mi perfil": datos del usuario en sesion, estadisticas de juego calculadas a
 * partir de sus reservas y calificaciones, y edicion de sus propios datos.
 */
@Controller
@RequestMapping("/perfil")
public class PerfilController {

    private final SesionActual sesion;
    private final UsuarioService usuarioService;
    private final ReservaService reservaService;
    private final CalificacionService calificacionService;
    private final Validator validator;

    public PerfilController(SesionActual sesion,
                            UsuarioService usuarioService,
                            ReservaService reservaService,
                            CalificacionService calificacionService,
                            Validator validator) {
        this.sesion = sesion;
        this.usuarioService = usuarioService;
        this.reservaService = reservaService;
        this.calificacionService = calificacionService;
        this.validator = validator;
    }

    /**
     * Solo se aceptan del formulario los campos que el usuario puede cambiar.
     * Asi nadie puede enviar "rol=ADMINISTRADOR" a mano.
     */
    @InitBinder("usuario")
    public void camposPermitidos(WebDataBinder binder) {
        binder.setAllowedFields("nombre", "telefono", "direccion", "password");
    }

    @GetMapping
    public String perfil(Model model) {
        Usuario usuario = sesion.getUsuario();
        List<Reserva> reservas = reservaService.listarPorEmailDeUsuario(usuario.getEmail());
        List<Calificacion> calificaciones = calificacionService.listarPorUsuario(usuario.getId());

        long canchasDiferentes = reservas.stream()
                .map(r -> r.getEspacio().getId())
                .distinct()
                .count();

        long minutosJugados = reservas.stream()
                .filter(r -> !ReservaService.CANCELADA.equalsIgnoreCase(r.getEstado()))
                .filter(r -> r.getHoraInicio() != null && r.getHoraFin() != null)
                .mapToLong(r -> Duration.between(r.getHoraInicio(), r.getHoraFin()).toMinutes())
                .sum();

        double promedio = calificaciones.stream()
                .map(Calificacion::getPuntuacion)
                .filter(Objects::nonNull)
                .mapToInt(Integer::intValue)
                .average()
                .orElse(0.0);

        LocalDate hoy = LocalDate.now();
        List<Reserva> proximas = reservas.stream()
                .filter(r -> !ReservaService.CANCELADA.equalsIgnoreCase(r.getEstado()))
                .filter(r -> r.getFecha() != null && !r.getFecha().isBefore(hoy))
                .sorted(Comparator.comparing(Reserva::getFecha).thenComparing(Reserva::getHoraInicio))
                .limit(3)
                .toList();

        model.addAttribute("usuario", usuario);
        model.addAttribute("totalReservas", reservas.size());
        model.addAttribute("canchasDiferentes", canchasDiferentes);
        model.addAttribute("horasJugadas", minutosJugados / 60);
        model.addAttribute("promedioCalificacion", promedio);
        model.addAttribute("totalCalificaciones", calificaciones.size());
        model.addAttribute("proximas", proximas);
        return "perfil/index";
    }

    @GetMapping("/editar")
    public String editar(Model model) {
        model.addAttribute("usuario", sesion.getUsuario());
        return "perfil/editar";
    }

    @PostMapping("/editar")
    public String guardar(@ModelAttribute("usuario") Usuario formulario,
                          BindingResult result,
                          Model model,
                          RedirectAttributes redirect) {
        // Id, email y rol salen siempre de la sesion, nunca del formulario.
        Usuario actual = sesion.getUsuario();
        formulario.setId(actual.getId());
        formulario.setEmail(actual.getEmail());
        formulario.setRol(actual.getRol());

        // Validacion manual (despues de fijar los campos anteriores).
        for (ConstraintViolation<Usuario> v : validator.validate(formulario)) {
            result.rejectValue(v.getPropertyPath().toString(), "invalido", v.getMessage());
        }
        if (result.hasErrors()) {
            return "perfil/editar";
        }
        try {
            usuarioService.guardar(formulario);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            return "perfil/editar";
        }
        redirect.addFlashAttribute("exito", "Tu perfil se actualizo correctamente");
        return "redirect:/perfil";
    }
}
