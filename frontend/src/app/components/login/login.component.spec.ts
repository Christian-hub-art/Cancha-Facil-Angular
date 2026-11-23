import { TestBed } from '@angular/core/testing';
import { HttpErrorResponse } from '@angular/common/http';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { AuthService, SesionNoConfirmadaError } from '../../services/auth.service';
import { Login } from './login.component';

describe('Login', () => {
  let respuestaLogin: () => Promise<unknown>;
  let llamadas: Array<[string, string]>;
  let navegaciones: string[];

  function crear(returnUrl: string | null = null) {
    llamadas = [];
    navegaciones = [];
    TestBed.configureTestingModule({
      imports: [Login],
      providers: [
        provideRouter([]),
        {
          provide: AuthService,
          useValue: {
            login: (email: string, password: string) => {
              llamadas.push([email, password]);
              return respuestaLogin();
            },
          },
        },
        {
          provide: ActivatedRoute,
          useValue: { snapshot: { queryParamMap: convertToParamMap(returnUrl ? { returnUrl } : {}) } },
        },
      ],
    });
    const router = TestBed.inject(Router);
    router.navigateByUrl = (url) => {
      navegaciones.push(String(url));
      return Promise.resolve(true);
    };
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    return fixture;
  }

  it('inicia sesión y vuelve a la ruta que pidió el guard', async () => {
    respuestaLogin = () => Promise.resolve({ email: 'admin@canchafacil.com', roles: ['ADMIN'] });
    const { componentInstance: login } = crear('/usuarios');
    expect(login.aviso).toBe('Inicia sesión para continuar.');

    await login.procesarLogin({ email: '  admin@canchafacil.com ', password: 'clave' });

    expect(llamadas).toEqual([['admin@canchafacil.com', 'clave']]);
    expect(navegaciones).toEqual(['/usuarios']);
    expect(login.enviando()).toBe(false);
  });

  it('sin returnUrl navega al catálogo', async () => {
    respuestaLogin = () => Promise.resolve({ email: 'laura@correo.com', roles: ['USER'] });
    const { componentInstance: login } = crear();
    await login.procesarLogin({ email: 'laura@correo.com', password: 'clave' });
    expect(navegaciones).toEqual(['/espacios']);
  });

  it('muestra el error de credenciales y no navega', async () => {
    respuestaLogin = () => Promise.reject(new HttpErrorResponse({ status: 401 }));
    const fixture = crear();
    await fixture.componentInstance.procesarLogin({ email: 'laura@correo.com', password: 'mala' });
    fixture.detectChanges();

    expect(navegaciones).toEqual([]);
    const alerta = (fixture.nativeElement as HTMLElement).querySelector('[role="alert"]');
    expect(alerta?.textContent).toContain('Correo o contraseña incorrectos.');
  });

  it('distingue servidor apagado, CSRF y cookie no conservada', async () => {
    const { componentInstance: login } = crear();
    const casos: Array<[unknown, string]> = [
      [new HttpErrorResponse({ status: 0 }), 'No se pudo conectar'],
      [new HttpErrorResponse({ status: 403 }), 'verificación de seguridad'],
      [new SesionNoConfirmadaError(), 'no conservó la sesión'],
    ];
    for (const [error, texto] of casos) {
      respuestaLogin = () => Promise.reject(error);
      await login.procesarLogin({ email: 'laura@correo.com', password: 'clave' });
      expect(login.mensajeError()).toContain(texto);
    }
  });

  it('no envía si faltan datos ni permite envíos dobles', async () => {
    let terminar!: (valor: unknown) => void;
    respuestaLogin = () => new Promise((resolver) => (terminar = resolver));
    const { componentInstance: login } = crear();

    await login.procesarLogin({ email: ' ', password: '' });
    expect(login.mensajeError()).toBe('Escribe tu correo y tu contraseña.');

    const primero = login.procesarLogin({ email: 'laura@correo.com', password: 'clave' });
    await login.procesarLogin({ email: 'laura@correo.com', password: 'clave' });
    expect(llamadas.length).toBe(1);

    terminar({ email: 'laura@correo.com', roles: ['USER'] });
    await primero;
  });
});
