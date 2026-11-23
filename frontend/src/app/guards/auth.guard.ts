import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { AuthService, Rol } from '../services/auth.service';

// Guards de navegación. Mejoran la experiencia (no muestran pantallas que el
// usuario no puede usar), pero NO reemplazan la seguridad: el backend rechaza
// igual con 401/403 cualquier petición que llegue sin sesión o sin el rol.
//
// Todos esperan a que AuthService termine de consultar /api/auth/me. Así, al
// recargar la página o abrir una URL directamente, no se redirige al login
// mientras la sesión todavía se está recuperando.

// Exige una sesión iniciada; si no la hay, envía al login y recuerda a dónde quería ir.
export const autenticadoGuard: CanActivateFn = async (_ruta, estado) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.asegurarSesion();
  return auth.autenticado() ? true : irAlLogin(router, estado.url);
};

// Exige sesión y al menos uno de los roles indicados. Uso: canActivate: [rolGuard('ADMIN')].
// Con sesión pero sin el rol, muestra /acceso-denegado (equivale al 403 del backend).
export function rolGuard(...rolesPermitidos: Rol[]): CanActivateFn {
  return async (_ruta, estado) => {
    const auth = inject(AuthService);
    const router = inject(Router);
    await auth.asegurarSesion();
    if (!auth.autenticado()) {
      return irAlLogin(router, estado.url);
    }
    return rolesPermitidos.some((rol) => auth.tieneRol(rol)) ? true : router.createUrlTree(['/acceso-denegado']);
  };
}

// Para /login: quien ya inició sesión no necesita volver a ver el formulario.
export const invitadoGuard: CanActivateFn = async () => {
  const auth = inject(AuthService);
  const router = inject(Router);
  await auth.asegurarSesion();
  return auth.autenticado() ? router.createUrlTree(['/espacios']) : true;
};

// Ruta interna a la que volver después del login. Solo se aceptan rutas que
// empiezan por "/" (y no "//") para que nadie pueda redirigir a otro sitio.
export function rutaDeRegresoSegura(valor: string | null | undefined): string {
  return valor && valor.startsWith('/') && !valor.startsWith('//') && !valor.startsWith('/\\') && !valor.startsWith('/login')
    ? valor
    : '/espacios';
}

function irAlLogin(router: Router, urlSolicitada: string): UrlTree {
  return router.createUrlTree(['/login'], { queryParams: { returnUrl: urlSolicitada } });
}
