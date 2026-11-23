import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, computed, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

// Roles que expone el backend en GET /api/auth/me (sin el prefijo ROLE_).
export type Rol = 'USER' | 'ADMIN';

// El servidor aceptó las credenciales, pero la consulta posterior de /api/auth/me
// no encontró la sesión: el navegador no guardó o no envió la cookie (CORS o SameSite).
export class SesionNoConfirmadaError extends Error {
  constructor() {
    super('El inicio de sesión no se pudo confirmar con la cookie de sesión.');
    this.name = 'SesionNoConfirmadaError';
  }
}

// Identidad de la sesión actual. El backend nunca envía la contraseña ni su hash.
export interface Sesion {
  email: string;
  roles: string[];
}

// Único punto de Angular que sabe iniciar, consultar y cerrar la sesión.
// Las pantallas solo leen sus signals (sesion, esAdmin, ...) y no duplican lógica.
//
// La sesión vive en el servidor (cookie JSESSIONID). Angular no guarda la cookie,
// la contraseña ni el token CSRF: el navegador envía la cookie solo y el
// credencialesInterceptor añade withCredentials y el token CSRF a cada petición.
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly backend = environment.apiUrl;

  // Estado interno; hacia afuera solo se expone en modo lectura.
  private readonly sesionActual = signal<Sesion | null>(null);
  private readonly consultada = signal(false);

  // Sesión actual: null = visitante (o todavía sin consultar, ver sesionConsultada).
  readonly sesion = this.sesionActual.asReadonly();
  // true cuando ya se sabe si hay sesión. Sirve para distinguir "visitante" de
  // "consultando todavía" y no redirigir ni pintar menús antes de tiempo.
  readonly sesionConsultada = this.consultada.asReadonly();
  readonly cargando = computed(() => !this.consultada());
  readonly autenticado = computed(() => this.sesionActual() !== null);
  readonly roles = computed(() => this.sesionActual()?.roles ?? []);
  // ADMIN puede administrar usuarios y crear, editar o eliminar canchas.
  readonly esAdmin = computed(() => this.roles().includes('ADMIN'));

  // Consulta a /api/auth/me en curso; se comparte para no repetir la petición
  // cuando varias partes (inicio de la app, guards, navegación) la piden a la vez.
  private consultaEnCurso: Promise<Sesion | null> | null = null;
  // Cambia en cada login/logout. Si llega una respuesta de /me pedida antes de
  // ese cambio, se descarta para que no "reviva" una sesión ya cerrada.
  private version = 0;

  tieneRol(rol: Rol): boolean {
    return this.roles().includes(rol);
  }

  // GET /api/auth/me: 200 con correo y roles, 401 si no hay sesión.
  // Se llama al cargar la app (app.config.ts) para recuperar la identidad al recargar.
  cargarSesion(): Promise<Sesion | null> {
    this.consultaEnCurso ??= this.consultarSesion().finally(() => (this.consultaEnCurso = null));
    return this.consultaEnCurso;
  }

  // Para los guards: espera la primera consulta; después responde de inmediato.
  asegurarSesion(): Promise<Sesion | null> {
    return this.consultada() ? Promise.resolve(this.sesionActual()) : this.cargarSesion();
  }

  // POST /login como formulario (Spring Security no recibe JSON aquí) y luego
  // GET /api/auth/me para confirmar la identidad con la cookie recién creada.
  // Si las credenciales son incorrectas, el backend responde 401 y ese
  // HttpErrorResponse se propaga al componente para mostrar el mensaje.
  async login(email: string, password: string): Promise<Sesion> {
    const formulario = new HttpParams().set('email', email).set('password', password);
    await firstValueFrom(
      this.http.post(`${this.backend}/login`, formulario, {
        headers: { Accept: 'application/json' }, // Pide JSON 200/401 en lugar de redirecciones.
      }),
    );

    this.version++;
    let sesion: Sesion;
    try {
      sesion = await firstValueFrom(this.http.get<Sesion>(`${this.backend}/api/auth/me`));
    } catch (error) {
      throw error instanceof HttpErrorResponse && error.status === 401 ? new SesionNoConfirmadaError() : error;
    }
    this.sesionActual.set(sesion);
    this.consultada.set(true);
    return sesion;
  }

  // POST /logout: el servidor invalida la sesión y responde 204.
  // El estado visible se limpia siempre. Devuelve false si no se pudo confirmar
  // el cierre en el servidor (por ejemplo, backend apagado).
  async logout(): Promise<boolean> {
    this.version++;
    try {
      await firstValueFrom(
        this.http.post(`${this.backend}/logout`, null, { headers: { Accept: 'application/json' } }),
      );
      return true;
    } catch {
      return false;
    } finally {
      this.limpiarEstado();
    }
  }

  // La usa el interceptor cuando el backend responde 401: la sesión expiró
  // o se cerró en el servidor, así que la interfaz deja de mostrarla.
  marcarSesionCerrada(): void {
    this.version++;
    this.limpiarEstado();
  }

  private limpiarEstado(): void {
    this.sesionActual.set(null);
    this.consultada.set(true);
  }

  private async consultarSesion(): Promise<Sesion | null> {
    const version = this.version;
    try {
      const sesion = await firstValueFrom(this.http.get<Sesion>(`${this.backend}/api/auth/me`));
      if (version === this.version) {
        this.sesionActual.set(sesion);
      }
    } catch (error) {
      // 401 = visitante. Otros errores (servidor caído) no cambian lo que ya se sabía.
      if (version === this.version && error instanceof HttpErrorResponse && error.status === 401) {
        this.sesionActual.set(null);
      }
    }
    this.consultada.set(true);
    return this.sesionActual();
  }
}
