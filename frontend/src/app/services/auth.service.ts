import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';

interface TokenCsrf {
  headerName: string;
  token: string;
}

export interface Sesion {
  email: string;
  roles: string[];
}

@Injectable({ providedIn: 'root' })
export class AuthService {
  private http = inject(HttpClient);
  private backend = environment.apiUrl;

  // --- Agregado por la Persona 3 (minimo, para la interfaz por rol). ---
  // La Persona 2 puede ampliarlo; login() no se modifico.

  // Sesion actual: null = visitante. La lee la interfaz para mostrar u ocultar opciones.
  readonly sesion = signal<Sesion | null>(null);
  // true cuando ya se consulto /api/auth/me al menos una vez.
  readonly sesionConsultada = signal(false);
  // ADMIN puede administrar usuarios y crear, editar o eliminar canchas.
  readonly esAdmin = computed(() => this.sesion()?.roles.includes('ADMIN') ?? false);

  // GET /api/auth/me: 200 con correo y roles, 401 si no hay sesion.
  async cargarSesion(): Promise<void> {
    try {
      this.sesion.set(
        await firstValueFrom(
          this.http.get<Sesion>(`${this.backend}/api/auth/me`, { withCredentials: true })
        )
      );
    } catch {
      this.sesion.set(null);
    } finally {
      this.sesionConsultada.set(true);
    }
  }

  // POST /logout con el token CSRF vigente; el servidor invalida la sesion (204).
  async logout(): Promise<void> {
    try {
      const csrf = await firstValueFrom(
        this.http.get<TokenCsrf>(`${this.backend}/api/auth/csrf`, { withCredentials: true })
      );
      await firstValueFrom(
        this.http.post(`${this.backend}/logout`, null, {
          withCredentials: true,
          headers: { Accept: 'application/json', [csrf.headerName]: csrf.token }
        })
      );
    } finally {
      this.sesion.set(null);
    }
  }
  // --- Fin de lo agregado por la Persona 3. ---

  async login(email: string, password: string): Promise<Sesion> {
    // 1. Obtener el token que protege el formulario de login.
    const csrf = await firstValueFrom(
      this.http.get<TokenCsrf>(`${this.backend}/api/auth/csrf`, {
        withCredentials: true
      })
    );

    // 2. Spring Security espera un formulario, no un cuerpo JSON.
    const formulario = new HttpParams()
      .set('email', email)
      .set('password', password);

    await firstValueFrom(
      this.http.post(`${this.backend}/login`, formulario, {
        withCredentials: true,
        headers: {
          Accept: 'application/json',
          [csrf.headerName]: csrf.token
        }
      })
    );

    // 3. Confirmar la identidad usando la cookie de sesion.
    return firstValueFrom(
      this.http.get<Sesion>(`${this.backend}/api/auth/me`, {
        withCredentials: true
      })
    );
  }
}
