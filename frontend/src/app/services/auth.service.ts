import { Injectable, inject } from '@angular/core';
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
