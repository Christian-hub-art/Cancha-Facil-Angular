package com.example.demo.pagos;

import java.math.BigDecimal;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Logica del pago con Wompi:
 *  1. crearOrden: guarda la orden en estado PENDING.
 *  2. urlDePago: arma el link del Web Checkout de Wompi, firmado con el
 *     secreto de integridad para que nadie pueda cambiar el monto.
 *  3. eventoValido: comprueba que el aviso del webhook de verdad viene de Wompi
 *     (firma con el secreto de eventos).
 *
 * Las llaves se leen de application.properties, que a su vez las toma de
 * variables de entorno. Nunca quedan escritas en el codigo.
 */
@Service
public class WompiService {

    private static final String URL_CHECKOUT = "https://checkout.wompi.co/p/";
    private static final String MONEDA = "COP";

    private final OrdenRepository ordenRepository;
    private final String llavePublica;
    private final String secretoIntegridad;
    private final String secretoEventos;
    private final String frontendUrl;

    public WompiService(OrdenRepository ordenRepository,
                        @Value("${wompi.public-key:}") String llavePublica,
                        @Value("${wompi.integrity-key:}") String secretoIntegridad,
                        @Value("${wompi.events-secret:}") String secretoEventos,
                        @Value("${app.frontend-url:http://localhost:4200}") String frontendUrl) {
        this.ordenRepository = ordenRepository;
        this.llavePublica = llavePublica;
        this.secretoIntegridad = secretoIntegridad;
        this.secretoEventos = secretoEventos;
        this.frontendUrl = frontendUrl.endsWith("/")
                ? frontendUrl.substring(0, frontendUrl.length() - 1)
                : frontendUrl;
    }

    /** true si estan las llaves necesarias para crear pagos. */
    public boolean checkoutConfigurado() {
        return !llavePublica.isBlank() && !secretoIntegridad.isBlank();
    }

    /** true si esta el secreto para validar los avisos del webhook. */
    public boolean webhookConfigurado() {
        return !secretoEventos.isBlank();
    }

    /**
     * Crea la orden en estado PENDING. El monto llega en pesos y se guarda en
     * centavos, que es como lo pide Wompi ($15.000 -> 1500000).
     */
    @Transactional
    public Orden crearOrden(BigDecimal montoEnPesos, String descripcion) {
        if (montoEnPesos == null || montoEnPesos.signum() <= 0) {
            throw new IllegalArgumentException("El monto debe ser mayor que 0");
        }
        long centavos;
        try {
            centavos = montoEnPesos.movePointRight(2).longValueExact();
        } catch (ArithmeticException e) {
            throw new IllegalArgumentException("El monto admite maximo 2 decimales");
        }

        Orden orden = new Orden();
        orden.setReferencia("ORD-" + UUID.randomUUID());
        orden.setMontoEnCentavos(centavos);
        orden.setMoneda(MONEDA);
        orden.setDescripcion(recortar(descripcion, 200));
        return ordenRepository.save(orden);
    }

    /**
     * Link del Web Checkout. Al terminar, Wompi devuelve al usuario a
     * redirect-url y le agrega ?id=<transaccion>. Se envia tambien la
     * referencia (?ref=) para que Angular sepa que orden consultar.
     */
    public String urlDePago(Orden orden) {
        String monto = String.valueOf(orden.getMontoEnCentavos());
        String firma = firmaIntegridad(orden.getReferencia(), monto, orden.getMoneda(), secretoIntegridad);
        String redirect = frontendUrl + "/pago/resultado?ref=" + orden.getReferencia();

        return URL_CHECKOUT
                + "?public-key=" + codificar(llavePublica)
                + "&currency=" + codificar(orden.getMoneda())
                + "&amount-in-cents=" + monto
                + "&reference=" + codificar(orden.getReferencia())
                + "&signature:integrity=" + firma
                + "&redirect-url=" + codificar(redirect);
    }

    /**
     * Valida la firma de un evento del webhook. Wompi indica en
     * signature.properties que campos de "data" firmo (ej. transaction.id,
     * transaction.status, transaction.amount_in_cents). Se concatenan sus
     * valores, luego el timestamp y por ultimo el secreto de eventos, y se
     * calcula SHA-256. Si coincide con signature.checksum, el aviso es real.
     */
    @SuppressWarnings("unchecked")
    public boolean eventoValido(Map<String, Object> evento) {
        if (!(evento.get("signature") instanceof Map<?, ?> firma)
                || !(firma.get("properties") instanceof List<?> propiedades)
                || !(firma.get("checksum") instanceof String checksumRecibido)
                || evento.get("timestamp") == null
                || !(evento.get("data") instanceof Map<?, ?> data)) {
            return false;
        }

        StringBuilder cadena = new StringBuilder();
        for (Object propiedad : propiedades) {
            Object valor = leerRuta((Map<String, Object>) data, String.valueOf(propiedad));
            cadena.append(comoTexto(valor));
        }
        cadena.append(comoTexto(evento.get("timestamp")));
        cadena.append(secretoEventos);

        String calculado = sha256(cadena.toString());
        // Comparacion en tiempo constante, sin importar mayusculas/minusculas.
        return MessageDigest.isEqual(
                calculado.getBytes(StandardCharsets.UTF_8),
                checksumRecibido.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    // ---------------------------------------------------------------------
    // Ayudas
    // ---------------------------------------------------------------------

    /** SHA-256 de referencia + monto en centavos + moneda + secreto de integridad. */
    static String firmaIntegridad(String referencia, String montoEnCentavos, String moneda, String secreto) {
        return sha256(referencia + montoEnCentavos + moneda + secreto);
    }

    static String sha256(String texto) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(texto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no esta disponible", e);
        }
    }

    /** Lee "transaction.amount_in_cents" dentro de data. */
    @SuppressWarnings("unchecked")
    static Object leerRuta(Map<String, Object> data, String ruta) {
        Object actual = data;
        for (String parte : ruta.split("\\.")) {
            if (!(actual instanceof Map<?, ?> mapa)) {
                return null;
            }
            actual = ((Map<String, Object>) mapa).get(parte);
        }
        return actual;
    }

    /** Numeros enteros sin ".0" (4490000 y no 4490000.0); null como texto vacio. */
    static String comoTexto(Object valor) {
        if (valor == null) {
            return "";
        }
        if (valor instanceof Double || valor instanceof Float || valor instanceof BigDecimal) {
            BigDecimal numero = new BigDecimal(valor.toString());
            return numero.stripTrailingZeros().scale() <= 0
                    ? numero.toBigInteger().toString()
                    : numero.toPlainString();
        }
        return valor.toString();
    }

    private static String codificar(String valor) {
        return URLEncoder.encode(valor, StandardCharsets.UTF_8);
    }

    private static String recortar(String texto, int maximo) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.length() <= maximo ? limpio : limpio.substring(0, maximo);
    }
}
