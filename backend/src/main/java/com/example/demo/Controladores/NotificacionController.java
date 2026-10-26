package com.example.demo.Controladores;

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

import com.example.demo.Entidades.Notificacion;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.NotificacionService;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/notificaciones")
public class NotificacionController {

    private final NotificacionService notificacionService;
    private final UsuarioService usuarioService;

    public NotificacionController(NotificacionService notificacionService, UsuarioService usuarioService) {
        this.notificacionService = notificacionService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("notificaciones", notificacionService.listar());
        model.addAttribute("titulo", "Notificaciones");
        return "notificaciones/lista";
    }

    /** GET /notificaciones?usuarioId=1 -> notificaciones de un usuario. */
    @GetMapping(params = "usuarioId")
    public String listarPorUsuario(@RequestParam Long usuarioId, Model model) {
        model.addAttribute("notificaciones", notificacionService.listarPorUsuario(usuarioId));
        model.addAttribute("titulo", "Notificaciones del usuario " + usuarioId);
        return "notificaciones/lista";
    }

    /** GET /notificaciones/no-leidas/1 -> solo las pendientes de leer (consulta @Query). */
    @GetMapping("/no-leidas/{usuarioId}")
    public String listarNoLeidas(@PathVariable("usuarioId") Long usuarioId, Model model) {
        model.addAttribute("notificaciones", notificacionService.listarNoLeidas(usuarioId));
        model.addAttribute("titulo", "Notificaciones sin leer del usuario " + usuarioId);
        return "notificaciones/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("notificacion", new Notificacion());
        prepararFormulario(model, "Crear notificacion");
        return "notificaciones/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("notificacion", notificacionService.obtenerPorId(id));
        prepararFormulario(model, "Editar notificacion");
        return "notificaciones/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("notificacion") Notificacion notificacion,
                          BindingResult result,
                          @RequestParam(value = "usuarioId", required = false) Long usuarioId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = notificacion.getId() == null ? "Crear notificacion" : "Editar notificacion";
        if (usuarioId == null) {
            result.reject("usuario", "Debe seleccionar un usuario");
        }
        notificacion.setUsuario(usuarioId == null ? null : usuarioService.buscarPorId(usuarioId));
        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "notificaciones/formulario";
        }
        try {
            notificacionService.guardar(notificacion, usuarioId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "notificaciones/formulario";
        }
        redirect.addFlashAttribute("exito", "Notificacion guardada correctamente");
        return "redirect:/notificaciones";
    }

    /** GET /notificaciones/leer/5 -> marca la notificacion como leida. */
    @GetMapping("/leer/{id}")
    public String marcarComoLeida(@PathVariable("id") Long id) {
        notificacionService.marcarComoLeida(id);
        return "redirect:/notificaciones";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        notificacionService.eliminar(id);
        redirect.addFlashAttribute("exito", "Notificacion eliminada");
        return "redirect:/notificaciones";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("notificacion", notificacionService.obtenerPorId(id));
        return "notificaciones/detalle";
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("accion", accion);
    }
}
