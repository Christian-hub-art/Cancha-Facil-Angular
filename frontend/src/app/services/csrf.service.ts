import { HttpBackend, HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// Lo que devuelve GET /api/auth/csrf (ver backend/SEGURIDAD.md).
export interface TokenCsrf {
  headerName: string; // Nombre de la cabecera que espera Spring (X-CSRF-TOKEN).
  token: string; // Valor del token. Solo vive en memoria: nunca se guarda en localStorage.
}

// Pide el token CSRF vigente al backend.
// Usa HttpBackend en lugar del HttpClient normal para que esta petición NO pase
// por los interceptores; así el interceptor de credenciales puede llamarla sin
// entrar en un ciclo (interceptor -> token -> interceptor -> ...).
@Injectable({ providedIn: 'root' })
export class CsrfService {
  private readonly http = new HttpClient(inject(HttpBackend));
  private readonly url = `${environment.apiUrl}/api/auth/csrf`;

  // Spring renueva el token al iniciar y cerrar sesión, por eso se consulta
  // justo antes de cada petición que modifica datos en lugar de reutilizar uno viejo.
  obtenerToken(): Observable<TokenCsrf> {
    return this.http.get<TokenCsrf>(this.url, { withCredentials: true });
  }
}
