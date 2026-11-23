import { TestBed } from '@angular/core/testing';
import { signal } from '@angular/core';
import { ActivatedRouteSnapshot, CanActivateFn, Router, RouterStateSnapshot, UrlTree, provideRouter } from '@angular/router';
import { AuthService, Rol } from '../services/auth.service';
import { autenticadoGuard, invitadoGuard, rolGuard, rutaDeRegresoSegura } from './auth.guard';

// AuthService falso: permite fijar la sesión sin hacer peticiones HTTP.
function crearAuthFalso(roles: Rol[] | null) {
  let consultas = 0;
  return {
    autenticado: signal(roles !== null),
    tieneRol: (rol: Rol) => roles?.includes(rol) ?? false,
    asegurarSesion: () => {
      consultas++;
      return Promise.resolve(null);
    },
    consultas: () => consultas,
  };
}

async function ejecutar(guard: CanActivateFn, url: string, roles: Rol[] | null) {
  const authFalso = crearAuthFalso(roles);
  TestBed.configureTestingModule({
    providers: [provideRouter([]), { provide: AuthService, useValue: authFalso }],
  });
  const resultado = await TestBed.runInInjectionContext(() =>
    guard({} as ActivatedRouteSnapshot, { url } as RouterStateSnapshot),
  );
  const router = TestBed.inject(Router);
  const destino = resultado instanceof UrlTree ? router.serializeUrl(resultado) : resultado;
  return { destino, authFalso };
}

describe('Guards de autenticación', () => {
  it('autenticadoGuard espera la consulta de sesión y deja pasar con sesión', async () => {
    const { destino, authFalso } = await ejecutar(autenticadoGuard, '/pago/resultado', ['USER']);
    expect(destino).toBe(true);
    expect(authFalso.consultas()).toBe(1);
  });

  it('autenticadoGuard envía al visitante al login recordando la URL', async () => {
    const { destino } = await ejecutar(autenticadoGuard, '/pago/resultado?ref=ORD-1', null);
    expect(destino).toBe('/login?returnUrl=%2Fpago%2Fresultado%3Fref%3DORD-1');
  });

  it('rolGuard(ADMIN) deja pasar al administrador', async () => {
    const { destino } = await ejecutar(rolGuard('ADMIN'), '/usuarios', ['ADMIN']);
    expect(destino).toBe(true);
  });

  it('rolGuard(ADMIN) muestra acceso denegado a un USER', async () => {
    const { destino } = await ejecutar(rolGuard('ADMIN'), '/usuarios', ['USER']);
    expect(destino).toBe('/acceso-denegado');
  });

  it('rolGuard(ADMIN) envía al visitante al login', async () => {
    const { destino } = await ejecutar(rolGuard('ADMIN'), '/usuarios', null);
    expect(destino).toBe('/login?returnUrl=%2Fusuarios');
  });

  it('invitadoGuard saca del login a quien ya tiene sesión', async () => {
    expect((await ejecutar(invitadoGuard, '/login', ['USER'])).destino).toBe('/espacios');
  });

  it('invitadoGuard deja ver el login al visitante', async () => {
    expect((await ejecutar(invitadoGuard, '/login', null)).destino).toBe(true);
  });
});

describe('rutaDeRegresoSegura', () => {
  it('acepta rutas internas', () => {
    expect(rutaDeRegresoSegura('/usuarios')).toBe('/usuarios');
  });

  it('rechaza URLs externas, vacías o el propio login', () => {
    for (const valor of [null, '', 'https://otro.com', '//otro.com', '/\\otro.com', '/login?x=1']) {
      expect(rutaDeRegresoSegura(valor)).toBe('/espacios');
    }
  });
});
