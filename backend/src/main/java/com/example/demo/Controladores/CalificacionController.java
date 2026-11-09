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

import com.example.demo.Entidades.Calificacion;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.CalificacionService;
import com.example.demo.Servicios.ReservaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/calificaciones")
public class CalificacionController {

    private final CalificacionService calificacionService;
    private final ReservaService reservaService;

    public CalificacionController(CalificacionService calificacionService, ReservaService reservaService) {
        this.calificacionService = calificacionService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("calificaciones", calificacionService.listar());
        model.addAttribute("titulo", "Calificaciones");
        return "calificaciones/lista";
    }

    /** GET /calificaciones?espacioId=1 -> calificaciones de un espacio. */
    @GetMapping(params = "espacioId")
    public String listarPorEspacio(@RequestParam Long espacioId, Model model) {
        model.addAttribute("calificaciones", calificacionService.listarPorEspacio(espacioId));
        model.addAttribute("titulo", "Calificaciones del espacio " + espacioId);
        return "calificaciones/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("calificacion", new Calificacion());
        prepararFormulario(model, "Crear calificacion");
        return "calificaciones/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("calificacion", calificacionService.obtenerPorId(id));
        prepararFormulario(model, "Editar calificacion");
        return "calificaciones/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("calificacion") Calificacion calificacion,
                          BindingResult result,
                          @RequestParam(value = "reservaId", required = false) Long reservaId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = calificacion.getId() == null ? "Crear calificacion" : "Editar calificacion";
        if (reservaId == null) {
            result.reject("reserva", "Debe seleccionar una reserva");
        }
        calificacion.setReserva(reservaId == null ? null : reservaService.buscarPorId(reservaId));
        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "calificaciones/formulario";
        }
        try {
            calificacionService.guardar(calificacion, reservaId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "calificaciones/formulario";
        }
        redirect.addFlashAttribute("exito", "Calificacion guardada correctamente");
        return "redirect:/calificaciones";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        calificacionService.eliminar(id);
        redirect.addFlashAttribute("exito", "Calificacion eliminada");
        return "redirect:/calificaciones";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("calificacion", calificacionService.obtenerPorId(id));
        return "calificaciones/detalle";
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("reservas", reservaService.listar());
        model.addAttribute("accion", accion);
    }
}
