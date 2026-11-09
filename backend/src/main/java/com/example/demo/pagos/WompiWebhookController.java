package com.example.demo.pagos;

import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Webhook: la direccion a la que Wompi "le toca la puerta" a nuestro backend
 * cuando una transaccion cambia de estado. Se registra en el panel de Wompi
 * (Sandbox) como https://<backend>/api/pagos/webhook.
 *
 * Esto es lo que de verdad confirma el pago: que el usuario vuelva a la pagina
 * de resultado no prueba nada, el aviso firmado de Wompi si.
 *
 * Respuestas:
 *   200 -> aviso recibido (Wompi deja de reintentar)
 *   401 -> la firma no cuadra: el aviso no viene de Wompi o el secreto esta mal
 *   503 -> falta WOMPI_EVENTS_SECRET en el servidor
 */
@Tag(name = "Pagos con Wompi")
@RestController
@RequestMapping("/api/pagos")
public class WompiWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WompiWebhookController.class);

    /** Estados finales que Wompi puede enviar para una transaccion. */
    private static final Set<String> ESTADOS_WOMPI = Set.of("APPROVED", "DECLINED", "VOIDED", "ERROR", "PENDING");

    private final WompiService wompiService;
    private final OrdenRepository ordenRepository;

    public WompiWebhookController(WompiService wompiService, OrdenRepository ordenRepository) {
        this.wompiService = wompiService;
        this.ordenRepository = ordenRepository;
    }

    @Operation(summary = "Webhook de Wompi",
            description = "Lo llama Wompi, no Angular. Valida la firma del evento y actualiza el estado de la orden.")
    @PostMapping("/webhook")
    @Transactional
    public ResponseEntity<Void> recibir(@RequestBody Map<String, Object> evento) {
        if (!wompiService.webhookConfigurado()) {
            log.error("Webhook de Wompi recibido, pero falta WOMPI_EVENTS_SECRET");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).build();
        }
        if (!wompiService.eventoValido(evento)) {
            log.warn("Webhook de Wompi con firma invalida; se descarta");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        // Solo interesan los cambios de transaccion; otros eventos se aceptan y se ignoran.
        if (!"transaction.updated".equals(evento.get("event"))) {
            return ResponseEntity.ok().build();
        }

        Object transaccion = WompiService.leerRuta(castMapa(evento.get("data")), "transaction");
        if (!(transaccion instanceof Map<?, ?> datos)) {
            return ResponseEntity.ok().build();
        }
        String referencia = String.valueOf(datos.get("reference"));
        String estado = String.valueOf(datos.get("status"));
        String transaccionId = String.valueOf(datos.get("id"));
        String montoRecibido = WompiService.comoTexto(datos.get("amount_in_cents"));

        ordenRepository.findByReferencia(referencia).ifPresentOrElse(orden -> {
            if (!montoRecibido.equals(String.valueOf(orden.getMontoEnCentavos()))) {
                // El monto pagado no es el de la orden: no se aprueba.
                log.warn("Orden {}: Wompi reporta {} centavos y la orden es de {}",
                        referencia, montoRecibido, orden.getMontoEnCentavos());
                orden.setEstado("ERROR");
            } else {
                orden.setEstado(ESTADOS_WOMPI.contains(estado) ? estado : "ERROR");
            }
            orden.setTransaccionId(transaccionId);
            ordenRepository.save(orden);
            log.info("Orden {} actualizada a {} (transaccion {})", referencia, orden.getEstado(), transaccionId);
        }, () -> log.warn("Webhook de Wompi para una referencia que no existe: {}", referencia));

        // 200 siempre que la firma sea valida, para que Wompi no reintente.
        return ResponseEntity.ok().build();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMapa(Object valor) {
        return valor instanceof Map<?, ?> mapa ? (Map<String, Object>) mapa : Map.of();
    }
}
