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

import com.example.demo.Entidades.Pago;
import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.PagoService;
import com.example.demo.Servicios.ReservaService;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/pagos")
public class PagoController {

    private final PagoService pagoService;
    private final ReservaService reservaService;

    public PagoController(PagoService pagoService, ReservaService reservaService) {
        this.pagoService = pagoService;
        this.reservaService = reservaService;
    }

    @GetMapping
    public String listar(Model model) {
        model.addAttribute("pagos", pagoService.listar());
        model.addAttribute("titulo", "Pagos");
        return "pagos/lista";
    }

    /** GET /pagos?estado=APROBADO -> filtra por estado. */
    @GetMapping(params = "estado")
    public String listarPorEstado(@RequestParam String estado, Model model) {
        model.addAttribute("pagos", pagoService.listarPorEstado(estado));
        model.addAttribute("titulo", "Pagos en estado " + estado);
        return "pagos/lista";
    }

    @GetMapping("/add")
    public String mostrarFormularioCrear(Model model) {
        model.addAttribute("pago", new Pago());
        prepararFormulario(model, "Registrar pago");
        return "pagos/formulario";
    }

    @GetMapping("/update/{id}")
    public String mostrarFormularioEditar(@PathVariable("id") Long id, Model model) {
        model.addAttribute("pago", pagoService.obtenerPorId(id));
        prepararFormulario(model, "Editar pago");
        return "pagos/formulario";
    }

    @PostMapping("/add")
    public String guardar(@Valid @ModelAttribute("pago") Pago pago,
                          BindingResult result,
                          @RequestParam(value = "reservaId", required = false) Long reservaId,
                          Model model,
                          RedirectAttributes redirect) {
        String accion = pago.getId() == null ? "Registrar pago" : "Editar pago";
        if (reservaId == null) {
            result.reject("reserva", "Debe seleccionar una reserva");
        }
        pago.setReserva(reservaId == null ? null : reservaService.buscarPorId(reservaId));
        if (result.hasErrors()) {
            prepararFormulario(model, accion);
            return "pagos/formulario";
        }
        try {
            pagoService.guardar(pago, reservaId);
        } catch (ReglaNegocioException ex) {
            model.addAttribute("error", ex.getMessage());
            prepararFormulario(model, accion);
            return "pagos/formulario";
        }
        redirect.addFlashAttribute("exito", "Pago guardado correctamente");
        return "redirect:/pagos";
    }

    /** GET /pagos/estado/5?valor=APROBADO -> aprueba, rechaza o reembolsa el pago. */
    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Long id,
                                @RequestParam("valor") String valor,
                                RedirectAttributes redirect) {
        pagoService.cambiarEstado(id, valor);
        redirect.addFlashAttribute("exito", "Pago #" + id + " actualizado a " + valor.toUpperCase());
        return "redirect:/pagos";
    }

    @GetMapping("/delete/{id}")
    public String eliminar(@PathVariable("id") Long id, RedirectAttributes redirect) {
        pagoService.eliminar(id);
        redirect.addFlashAttribute("exito", "Pago eliminado");
        return "redirect:/pagos";
    }

    @GetMapping("/{id}")
    public String detalle(@PathVariable("id") Long id, Model model) {
        model.addAttribute("pago", pagoService.obtenerPorId(id));
        return "pagos/detalle";
    }

    private void prepararFormulario(Model model, String accion) {
        model.addAttribute("reservas", reservaService.listar());
        model.addAttribute("accion", accion);
    }
}
