package com.example.demo.Controladores;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

import com.example.demo.Entidades.Espacio;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.CalificacionService;
import com.example.demo.Servicios.EspacioService;
import com.example.demo.Servicios.NegocioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/espacios")
public class EspacioController {

    private final EspacioService espacioService;
    private final NegocioService negocioService;
    private final CalificacionService calificacionService;

    public EspacioController(EspacioService espacioService,
                             NegocioService negocioService,
                             CalificacionService calificacionService) {
        this.espacioService = espacioService;
        this.negocioService = negocioService;
        this.calificacionService = calificacionService;
    }

    @GetMapping
    public String listar(Model model) {
        agregarEspacios(model, espacioService.listar());
        model.addAttribute("titulo", "Canchas");
        return "espacios/lista";
    }

    /** GET /espacios?deporte=futbol -> filtra por tipo de deporte. */
    @GetMapping(params = "deporte")
    public String buscarPorDeporte(@RequestParam String deporte, Model model) {
        agregarEspacios(model, espacioService.buscarPorDeporte(deporte));
        model.addAttribute("titulo", "Canchas de " + deporte);
        return "espacios/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("espacio", new Espacio());
        prepararFormulario(model, "Crear espacio");
        return "espacios/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("espacio", espacioService.obtenerPorId(id));
        prepararFormulario(model, "Editar espacio");
        return "espacios/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("espacio") Espacio espacio,
                          BindingResult result,
                          @RequestParam(value = "negocioId", required = false) Long negocioId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = espacio.getId() == null ? "Crear espacio" : "Editar espacio";
        if (negocioId == null) {
            result.reject("negocio", "Debe seleccionar un negocio");
        }
        espacio.setNegocio(negocioId == null ? null : negocioService.buscarPorId(negocioId));
        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "espacios/formulario";
        }
        try {
            espacioService.guardar(espacio, negocioId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "espacios/formulario";
        }
        redirect.addFlashAttribute("exito", "Espacio guardado correctamente");
        return "redirect:/espacios";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        espacioService.eliminar(id);
        redirect.addFlashAttribute("exito", "Espacio eliminado");
        return "redirect:/espacios";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("espacio", espacioService.obtenerPorId(id));
        model.addAttribute("promedio", calificacionService.promedioPorEspacio(id));
        model.addAttribute("calificaciones", calificacionService.listarPorEspacio(id));
        return "espacios/detalle";
    }

    /** Lista de canchas + su promedio de calificacion para pintar las tarjetas. */
    private void agregarEspacios(Model model, List<Espacio> espacios) {
        Map<Long, Double> promedios = new HashMap<>();
        for (Espacio e : espacios) {
            promedios.put(e.getId(), calificacionService.promedioPorEspacio(e.getId()));
        }
        model.addAttribute("espacios", espacios);
        model.addAttribute("promedios", promedios);
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("negocios", negocioService.listar());
        model.addAttribute("accion", accion);
    }
}
