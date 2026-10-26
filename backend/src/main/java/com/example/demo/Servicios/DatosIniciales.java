package com.example.demo.Servicios;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.example.demo.Entidades.Calificacion;
import com.example.demo.Entidades.Espacio;
import com.example.demo.Entidades.Negocio;
import com.example.demo.Entidades.Notificacion;
import com.example.demo.Entidades.Pago;
import com.example.demo.Entidades.Reserva;
import com.example.demo.Entidades.Rol;
import com.example.demo.Entidades.Usuario;
import com.example.demo.Repositorios.CalificacionRepository;
import com.example.demo.Repositorios.EspacioRepository;
import com.example.demo.Repositorios.NegocioRepository;
import com.example.demo.Repositorios.NotificacionRepository;
import com.example.demo.Repositorios.PagoRepository;
import com.example.demo.Repositorios.ReservaRepository;
import com.example.demo.Repositorios.UsuarioRepository;

/**
 * Datos iniciales para demostrar la aplicacion.
 *
 * Se ejecuta una vez al arrancar (CommandLineRunner):
 * 1. Si hay usuarios con contrasena en texto plano (creados antes de agregar el
 *    login), se cifran con BCrypt para que puedan iniciar sesion con la misma clave.
 * 2. Se asegura que exista el administrador admin@canchafacil.com / admin123.
 * 3. Si la base esta vacia, carga negocios, espacios, reservas, pagos,
 *    calificaciones y notificaciones de ejemplo.
 *
 * Usuarios de prueba:
 *   admin@canchafacil.com / admin123   (ADMINISTRADOR)
 *   laura@correo.com      / cliente123 (CLIENTE)
 *   carlos@correo.com     / cliente123 (CLIENTE)
 */
@Component
public class DatosIniciales implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DatosIniciales.class);

    private final UsuarioRepository usuarioRepository;
    private final NegocioRepository negocioRepository;
    private final EspacioRepository espacioRepository;
    private final ReservaRepository reservaRepository;
    private final PagoRepository pagoRepository;
    private final CalificacionRepository calificacionRepository;
    private final NotificacionRepository notificacionRepository;
    private final PasswordEncoder passwordEncoder;

    public DatosIniciales(UsuarioRepository usuarioRepository,
                          NegocioRepository negocioRepository,
                          EspacioRepository espacioRepository,
                          ReservaRepository reservaRepository,
                          PagoRepository pagoRepository,
                          CalificacionRepository calificacionRepository,
                          NotificacionRepository notificacionRepository,
                          PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.negocioRepository = negocioRepository;
        this.espacioRepository = espacioRepository;
        this.reservaRepository = reservaRepository;
        this.pagoRepository = pagoRepository;
        this.calificacionRepository = calificacionRepository;
        this.notificacionRepository = notificacionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        cifrarContrasenasAntiguas();

        boolean baseVacia = usuarioRepository.count() == 0;

        Usuario admin = usuarioRepository.findByEmailIgnoreCase("admin@canchafacil.com");
        if (admin == null) {
            admin = usuarioRepository.save(usuario("Administrador CanchaFacil", "admin@canchafacil.com",
                    "admin123", "3001112233", "Calle 10 # 20-30", Rol.ADMINISTRADOR));
            log.info("Usuario administrador creado: admin@canchafacil.com / admin123");
        }

        if (!baseVacia) {
            return;
        }

        // ---------- Usuarios ----------
        Usuario laura = usuarioRepository.save(usuario("Laura Gomez", "laura@correo.com",
                "cliente123", "3104445566", "Carrera 45 # 12-08", Rol.CLIENTE));
        Usuario carlos = usuarioRepository.save(usuario("Carlos Ruiz", "carlos@correo.com",
                "cliente123", "3207778899", "Avenida 68 # 30-15", Rol.CLIENTE));

        // ---------- Negocios ----------
        Negocio bosque = negocioRepository.save(negocio(admin, "Complejo Deportivo El Bosque",
                "900123456-1", "Calle 134 # 7-20", "Canchas sinteticas con iluminacion y parqueadero."));
        Negocio la70 = negocioRepository.save(negocio(admin, "Canchas La 70",
                "900654321-7", "Carrera 70 # 53-10", "Futbol 5 y futbol 8 en el occidente de la ciudad."));

        // ---------- Espacios ----------
        Espacio bosque1 = espacioRepository.save(espacio(bosque, "Cancha Bosque 1", "futbol 5", "80000", 10,
                "Grama sintetica, techada."));
        Espacio bosque2 = espacioRepository.save(espacio(bosque, "Cancha Bosque 2", "futbol 8", "120000", 16,
                "Grama sintetica al aire libre."));
        Espacio la70a = espacioRepository.save(espacio(la70, "La 70 - Cancha A", "futbol 5", "70000", 10,
                "Incluye petos y balon."));
        espacioRepository.save(espacio(la70, "La 70 - Estadio", "futbol 11", "250000", 22,
                "Cancha profesional con graderia."));

        // ---------- Reservas ----------
        LocalDate hoy = LocalDate.now();
        Reserva r1 = reservaRepository.save(reserva(laura, bosque1, hoy.minusDays(7), 18, 19, "COMPLETADA"));
        Reserva r2 = reservaRepository.save(reserva(carlos, bosque2, hoy.minusDays(3), 20, 22, "COMPLETADA"));
        Reserva r3 = reservaRepository.save(reserva(laura, la70a, hoy.plusDays(2), 19, 20, "CONFIRMADA"));
        reservaRepository.save(reserva(carlos, bosque1, hoy.plusDays(5), 17, 18, "PENDIENTE"));
        reservaRepository.save(reserva(laura, bosque2, hoy.plusDays(1), 21, 22, "CANCELADA"));

        // ---------- Pagos ----------
        pagoRepository.save(pago(r1, "80000", "Tarjeta", "APROBADO", "TRX-0001"));
        pagoRepository.save(pago(r2, "240000", "PSE", "APROBADO", "TRX-0002"));
        pagoRepository.save(pago(r3, "70000", "Nequi", "APROBADO", "TRX-0003"));

        // ---------- Calificaciones ----------
        calificacionRepository.save(calificacion(r1, 5, "Excelente cancha, muy bien iluminada."));
        calificacionRepository.save(calificacion(r2, 4, "Buena cancha, el parqueadero se llena rapido."));

        // ---------- Notificaciones ----------
        notificacionRepository.save(notificacion(laura, "RESERVA",
                "Tu reserva en La 70 - Cancha A fue confirmada."));
        notificacionRepository.save(notificacion(carlos, "PAGO",
                "Tienes una reserva pendiente de pago en Cancha Bosque 1."));
        notificacionRepository.save(notificacion(admin, "SISTEMA",
                "Bienvenido a CanchaFacil. Se cargaron datos de ejemplo."));

        log.info("Datos iniciales cargados: 3 usuarios, 2 negocios, 4 espacios, 5 reservas.");
    }

    /** Usuarios creados antes del login tenian la clave en texto plano. */
    private void cifrarContrasenasAntiguas() {
        for (Usuario u : usuarioRepository.findAll()) {
            String password = u.getPassword();
            if (password != null && !password.startsWith("$2")) {
                u.setPassword(passwordEncoder.encode(password));
                if (u.getDireccion() == null || u.getDireccion().isBlank()) {
                    u.setDireccion("Sin direccion");
                }
                usuarioRepository.save(u);
                log.info("Contrasena cifrada con BCrypt para {}", u.getEmail());
            }
        }
    }

    private Usuario usuario(String nombre, String email, String password, String telefono,
                            String direccion, Rol rol) {
        Usuario u = new Usuario();
        u.setNombre(nombre);
        u.setEmail(email);
        u.setPassword(passwordEncoder.encode(password));
        u.setTelefono(telefono);
        u.setDireccion(direccion);
        u.setRol(rol);
        u.setFechaRegistro(LocalDateTime.now());
        return u;
    }

    private Negocio negocio(Usuario admin, String nombre, String nit, String direccion, String descripcion) {
        Negocio n = new Negocio();
        n.setAdministrador(admin);
        n.setNombre(nombre);
        n.setNit(nit);
        n.setDireccion(direccion);
        n.setDescripcion(descripcion);
        return n;
    }

    private Espacio espacio(Negocio negocio, String nombre, String deporte, String precio,
                            int capacidad, String descripcion) {
        Espacio e = new Espacio();
        e.setNegocio(negocio);
        e.setNombre(nombre);
        e.setTipoDeporte(deporte);
        e.setPrecioHora(new BigDecimal(precio));
        e.setCapacidad(capacidad);
        e.setDescripcion(descripcion);
        return e;
    }

    private Reserva reserva(Usuario usuario, Espacio espacio, LocalDate fecha,
                            int horaInicio, int horaFin, String estado) {
        Reserva r = new Reserva();
        r.setUsuario(usuario);
        r.setEspacio(espacio);
        r.setFecha(fecha);
        r.setHoraInicio(LocalTime.of(horaInicio, 0));
        r.setHoraFin(LocalTime.of(horaFin, 0));
        r.setEstado(estado);
        r.setFechaCreacion(LocalDateTime.now());
        return r;
    }

    private Pago pago(Reserva reserva, String monto, String metodo, String estado, String referencia) {
        Pago p = new Pago();
        p.setReserva(reserva);
        p.setMonto(new BigDecimal(monto));
        p.setMetodoPago(metodo);
        p.setEstado(estado);
        p.setReferencia(referencia);
        p.setFechaPago(LocalDateTime.now());
        return p;
    }

    private Calificacion calificacion(Reserva reserva, int puntuacion, String comentario) {
        Calificacion c = new Calificacion();
        c.setReserva(reserva);
        c.setUsuario(reserva.getUsuario());
        c.setEspacio(reserva.getEspacio());
        c.setPuntuacion(puntuacion);
        c.setComentario(comentario);
        c.setFecha(LocalDateTime.now());
        return c;
    }

    private Notificacion notificacion(Usuario usuario, String tipo, String mensaje) {
        Notificacion n = new Notificacion();
        n.setUsuario(usuario);
        n.setTipo(tipo);
        n.setMensaje(mensaje);
        n.setFecha(LocalDateTime.now());
        n.setLeido(false);
        return n;
    }
}
