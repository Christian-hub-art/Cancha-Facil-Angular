import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { CsrfService } from '../services/csrf.service';

// Métodos que cambian datos: Spring exige el token CSRF en todos ellos (también en login y logout).
const METODOS_CON_CSRF = new Set(['POST', 'PUT', 'PATCH', 'DELETE']);

// Interceptor de credenciales. Solo actúa sobre peticiones al backend propio
// (environment.apiUrl); cualquier otra URL sale sin cookie ni token.
//  1. withCredentials: el navegador envía la cookie de sesión (JSESSIONID).
//  2. En POST/PUT/PATCH/DELETE consulta el token CSRF vigente y lo pone en la
//     cabecera que indica el backend (X-CSRF-TOKEN).
//  3. Si el backend responde 401, avisa a AuthService para que la interfaz
//     deje de mostrar una sesión que ya no existe.
export const credencialesInterceptor: HttpInterceptorFn = (peticion, siguiente) => {
  if (!esDelBackend(peticion)) {
    return siguiente(peticion);
  }

  const auth = inject(AuthService);
  const csrf = inject(CsrfService);
  const conCookie = peticion.clone({ withCredentials: true });

  const respuesta = METODOS_CON_CSRF.has(peticion.method)
    ? csrf
        .obtenerToken()
        .pipe(switchMap(({ headerName, token }) => siguiente(conCookie.clone({ setHeaders: { [headerName]: token } }))))
    : siguiente(conCookie);

  return respuesta.pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse && error.status === 401) {
        auth.marcarSesionCerrada();
      }
      return throwError(() => error);
    }),
  );
};

// Compara orígenes (protocolo + host + puerto) en lugar de prefijos de texto,
// para que una URL como http://localhost:8080.otro.com no se confunda con el backend.
function esDelBackend(peticion: HttpRequest<unknown>): boolean {
  try {
    const base = globalThis.location?.origin ?? 'http://localhost';
    return new URL(peticion.url, base).origin === new URL(environment.apiUrl, base).origin;
  } catch {
    return false;
  }
}
