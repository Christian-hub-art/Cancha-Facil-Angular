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

import com.example.demo.Entidades.Negocio;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.NegocioService;
import com.example.demo.Servicios.UsuarioService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/negocios")
public class NegocioController {

    private final NegocioService negocioService;
    private final UsuarioService usuarioService;

    public NegocioController(NegocioService negocioService, UsuarioService usuarioService) {
        this.negocioService = negocioService;
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("negocios", negocioService.listar());
        model.addAttribute("titulo", "Negocios");
        return "negocios/lista";
    }

    @GetMapping(params = "nombre")
    public String buscarPorNombre(@RequestParam String nombre, Model model) {
        model.addAttribute("negocios", negocioService.buscarPorNombre(nombre));
        model.addAttribute("titulo", "Negocios que coinciden con: " + nombre);
        return "negocios/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("negocio", new Negocio());
        prepararFormulario(model, "Crear negocio");
        return "negocios/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("negocio", negocioService.obtenerPorId(id));
        prepararFormulario(model, "Editar negocio");
        return "negocios/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("negocio") Negocio negocio,
                          BindingResult result,
                          @RequestParam(value = "administradorId", required = false) Long administradorId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = negocio.getId() == null ? "Crear negocio" : "Editar negocio";
        if (administradorId == null) {
            result.reject("administrador", "Debe seleccionar un administrador");
        }
        // Se vuelve a colgar el administrador elegido para que el <select> lo conserve.
        negocio.setAdministrador(administradorId == null ? null : usuarioService.buscarPorId(administradorId));
        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "negocios/formulario";
        }
        try {
            negocioService.guardar(negocio, administradorId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "negocios/formulario";
        }
        redirect.addFlashAttribute("exito", "Negocio guardado correctamente");
        return "redirect:/negocios";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        negocioService.eliminar(id);
        redirect.addFlashAttribute("exito", "Negocio eliminado");
        return "redirect:/negocios";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("negocio", negocioService.obtenerPorId(id));
        return "negocios/detalle";
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("accion", accion);
    }
}
