package com.example.demo.Controladores;

import com.example.demo.Servicios.SesionActual;
import java.util.List;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.demo.Entidades.Reserva;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.EspacioService;
import com.example.demo.Servicios.ReservaService;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/reservas")
public class ReservaController {

    private final ReservaService reservaService;
    private final UsuarioService usuarioService;
    private final EspacioService espacioService;
    private final SesionActual sesion;

    public ReservaController(ReservaService reservaService,
                             UsuarioService usuarioService,
                             EspacioService espacioService,
                             SesionActual sesion) {
        this.reservaService = reservaService;
        this.usuarioService = usuarioService;
        this.espacioService = espacioService;
        this.sesion = sesion;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("reservas", reservaService.listar());
        model.addAttribute("titulo", "Reservas");
        return "reservas/lista";
    }

    /** GET /reservas?estado=CONFIRMADA -> filtra por estado. */
    @GetMapping(params = "estado")
    public String listarPorEstado(@RequestParam String estado, Model model) {
        model.addAttribute("reservas", reservaService.listarPorEstado(estado));
        model.addAttribute("titulo", "Reservas en estado " + estado);
        return "reservas/lista";
    }

    /** GET /reservas?usuarioId=1 -> reservas de un usuario. */
    @GetMapping(params = "usuarioId")
    public String listarPorUsuario(@RequestParam Long usuarioId, Model model) {
        model.addAttribute("reservas", reservaService.listarPorUsuario(usuarioId));
        model.addAttribute("titulo", "Reservas del usuario " + usuarioId);
        return "reservas/lista";
    }

    /**
     * GET /reservas/mis-reservas -> reservas del usuario que inicio sesion.
     * Usa la relacion Reserva -> Usuario con una consulta JPQL por email.
     */
    @GetMapping("/mis-reservas")
    public String misReservas(Model model) {
        model.addAttribute("reservas", reservaService.listarPorEmailDeUsuario(sesion.getEmail()));
        model.addAttribute("titulo", "Mis reservas");
        return "reservas/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        Reserva reserva = new Reserva();
        // Por defecto la reserva queda a nombre de quien inicio sesion.
        reserva.setUsuario(sesion.getUsuario());
        model.addAttribute("reserva", reserva);
        prepararFormulario(model, "Crear reserva");
        return "reservas/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("reserva", reservaService.obtenerPorId(id));
        prepararFormulario(model, "Editar reserva");
        return "reservas/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("reserva") Reserva reserva,
                          BindingResult result,
                          @RequestParam(value = "usuarioId", required = false) Long usuarioId,
                          @RequestParam(value = "espacioId", required = false) Long espacioId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = reserva.getId() == null ? "Crear reserva" : "Editar reserva";
        if (usuarioId == null) {
            result.reject("usuario", "Debe seleccionar un usuario");
        }
        if (espacioId == null) {
            result.reject("espacio", "Debe seleccionar un espacio");
        }
        if (reserva.getHoraInicio() != null && reserva.getHoraFin() != null
                && !reserva.getHoraInicio().isBefore(reserva.getHoraFin())) {
            result.rejectValue("horaFin", "rango", "La hora de fin debe ser posterior a la hora de inicio");
        }
        // Se conservan las selecciones para volver a pintar el formulario.
        reserva.setUsuario(usuarioId == null ? null : usuarioService.buscarPorId(usuarioId));
        reserva.setEspacio(espacioId == null ? null : espacioService.buscarPorId(espacioId));

        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "reservas/formulario";
        }
        try {
            reservaService.guardar(reserva, usuarioId, espacioId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "reservas/formulario";
        }
        redirect.addFlashAttribute("exito", "Reserva guardada correctamente");
        return "redirect:/reservas";
    }

    /** GET /reservas/cancelar/5 -> cambia el estado a CANCELADA. */
    @GetMapping("/cancelar/{id}")
    public String cancelar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        reservaService.cancelar(id);
        redirect.addFlashAttribute("exito", "Reserva #" + id + " cancelada");
        return "redirect:/reservas";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        reservaService.eliminar(id);
        redirect.addFlashAttribute("exito", "Reserva eliminada");
        return "redirect:/reservas";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("reserva", reservaService.obtenerPorId(id));
        return "reservas/detalle";
    }

    /**
     * El administrador puede reservar a nombre de cualquier usuario; un cliente
     * solo se ve a si mismo en la lista.
     */
    private void prepararFormulario(Model model, String accion) {
        List<Usuario> usuarios;
        if (sesion.isAdmin()) {
            usuarios = usuarioService.listar();
        } else {
            Usuario actual = sesion.getUsuario();
            usuarios = actual == null ? List.of() : List.of(actual);
        }
        model.addAttribute("usuarios", usuarios);
        model.addAttribute("espacios", espacioService.listar());
        model.addAttribute("accion", accion);
    }
}
