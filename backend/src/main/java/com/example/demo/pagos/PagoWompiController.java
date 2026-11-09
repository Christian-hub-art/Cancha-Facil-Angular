package com.example.demo.pagos;

import java.math.BigDecimal;
import java.net.URI;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.DTO.ErrorRespuestaDTO;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Endpoints que usa Angular para pagar con Wompi.
 *
 *   POST /api/pagos/checkout             -> crea la orden y devuelve el link de pago (201)
 *   GET  /api/pagos/ordenes/{referencia} -> estado de la orden (200 o 404)
 *
 * Se llama PagoWompiController (y no PagoController como en el laboratorio)
 * porque el proyecto ya tiene un PagoController para las paginas Thymeleaf de
 * pagos, y dos clases con el mismo nombre chocan como beans de Spring.
 *
 * El CORS para Angular lo configura SeguridadConfig (app.frontend-url).
 */
@Tag(name = "Pagos con Wompi", description = "Crear el link de pago y consultar el estado de una orden")
@RestController
@RequestMapping("/api/pagos")
public class PagoWompiController {

    /** Lo que envia el boton: monto en pesos y, opcional, que se paga. */
    public record CheckoutSolicitud(BigDecimal monto, String descripcion) {
    }

    /** Lo que recibe el boton: la referencia y el link al que debe ir el usuario. */
    public record CheckoutRespuesta(String referencia, long montoEnCentavos, String urlPago) {
    }

    /** Estado de una orden, para la pagina de resultado. */
    public record OrdenRespuesta(String referencia, String estado, long montoEnCentavos,
                                 String moneda, String descripcion, String transaccionId) {
        static OrdenRespuesta desde(Orden orden) {
            return new OrdenRespuesta(orden.getReferencia(), orden.getEstado(),
                    orden.getMontoEnCentavos(), orden.getMoneda(),
                    orden.getDescripcion(), orden.getTransaccionId());
        }
    }

    private final WompiService wompiService;
    private final OrdenRepository ordenRepository;

    public PagoWompiController(WompiService wompiService, OrdenRepository ordenRepository) {
        this.wompiService = wompiService;
        this.ordenRepository = ordenRepository;
    }

    @Operation(summary = "Crear pago",
            description = "Crea la orden en estado PENDING y devuelve el link firmado del Web Checkout de Wompi. "
                    + "201 si sale bien, 400 si el monto no es valido, 503 si faltan las llaves de Wompi.")
    @PostMapping("/checkout")
    public ResponseEntity<?> checkout(@RequestBody CheckoutSolicitud solicitud) {
        if (!wompiService.checkoutConfigurado()) {
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body(new ErrorRespuestaDTO(503,
                            "Los pagos con Wompi no estan configurados en el servidor (faltan las llaves)"));
        }
        try {
            Orden orden = wompiService.crearOrden(solicitud.monto(), solicitud.descripcion());
            return ResponseEntity
                    .created(URI.create("/api/pagos/ordenes/" + orden.getReferencia()))
                    .body(new CheckoutRespuesta(orden.getReferencia(), orden.getMontoEnCentavos(),
                            wompiService.urlDePago(orden)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(new ErrorRespuestaDTO(400, e.getMessage()));
        }
    }

    @Operation(summary = "Consultar estado de una orden",
            description = "PENDING mientras Wompi no avisa; luego APPROVED, DECLINED, VOIDED o ERROR. 404 si no existe.")
    @GetMapping("/ordenes/{referencia}")
    public ResponseEntity<?> estado(@PathVariable String referencia) {
        return ordenRepository.findByReferencia(referencia)
                .<ResponseEntity<?>>map(orden -> ResponseEntity.ok(OrdenRespuesta.desde(orden)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ErrorRespuestaDTO(404, "No existe la orden " + referencia)));
    }
}
