package com.example.demo.Controladores;

import java.math.BigDecimal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.demo.Servicios.ReglaNegocioException;
import com.example.demo.Servicios.CalificacionService;
import com.example.demo.Servicios.EspacioService;
import com.example.demo.Servicios.PagoService;
import com.example.demo.Servicios.ReservaService;

/**
 * Pagina que muestra los resultados de las consultas personalizadas
 * (@Query / JPQL) de los repositorios:
 * - ranking de espacios por calificacion (CalificacionRepository.rankingDeEspacios)
 * - reservas por estado (ReservaRepository.contarPorEstado)
 * - ingresos por negocio (PagoRepository.ingresosPorNegocio)
 * - espacios por rango de precio (EspacioRepository.buscarPorRangoDePrecio)
 */
@Controller
@RequestMapping("/consultas")
public class ConsultaController {

    private final CalificacionService calificacionService;
    private final ReservaService reservaService;
    private final PagoService pagoService;
    private final EspacioService espacioService;

    public ConsultaController(CalificacionService calificacionService,
                              ReservaService reservaService,
                              PagoService pagoService,
                              EspacioService espacioService) {
        this.calificacionService = calificacionService;
        this.reservaService = reservaService;
        this.pagoService = pagoService;
        this.espacioService = espacioService;
    }

    @GetMapping
    public String consultas(@RequestParam(value = "min", required = false) BigDecimal min,
                            @RequestParam(value = "max", required = false) BigDecimal max,
                            Model model) {
        model.addAttribute("ranking", calificacionService.rankingDeEspacios());
        model.addAttribute("reservasPorEstado", reservaService.contarPorEstado());
        model.addAttribute("ingresos", pagoService.ingresosPorNegocio());

        model.addAttribute("min", min);
        model.addAttribute("max", max);
        if (min != null || max != null) {
            try {
                model.addAttribute("espaciosEnRango", espacioService.buscarPorRangoDePrecio(min, max));
            } catch (ReglaNegocioException ex) {
                model.addAttribute("errorRango", ex.getMessage());
            }
        }
        return "consultas/index";
    }
}
