import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { AuthService } from './auth.service';
import { environment } from '../../environments/environment';

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(), provideHttpClientTesting()]
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía formulario, CSRF y cookie antes de consultar la sesión', async () => {
    const resultado = auth.login('laura@correo.com', 'clave-de-prueba');
    const csrf = http.expectOne(`${environment.apiUrl}/api/auth/csrf`);
    expect(csrf.request.withCredentials).toBe(true);
    csrf.flush({ headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' });
    await Promise.resolve();

    const login = http.expectOne(`${environment.apiUrl}/login`);
    expect(login.request.method).toBe('POST');
    expect(login.request.withCredentials).toBe(true);
    expect(login.request.headers.get('Accept')).toBe('application/json');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('token-de-prueba');
    expect(login.request.body.get('email')).toBe('laura@correo.com');
    expect(login.request.body.get('password')).toBe('clave-de-prueba');
    login.flush({ mensaje: 'Sesion iniciada' });
    await Promise.resolve();

    const sesion = http.expectOne(`${environment.apiUrl}/api/auth/me`);
    expect(sesion.request.withCredentials).toBe(true);
    sesion.flush({ email: 'laura@correo.com', roles: ['USER'] });
    expect(await resultado).toEqual({ email: 'laura@correo.com', roles: ['USER'] });
  });

  it('propaga el 401 y no consulta la sesión si falla el login', async () => {
    const resultado = auth.login('laura@correo.com', 'incorrecta');
    const rechazo = expect(resultado).rejects.toMatchObject({ status: 401 });
    http.expectOne(`${environment.apiUrl}/api/auth/csrf`)
      .flush({ headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' });
    await Promise.resolve();
    http.expectOne(`${environment.apiUrl}/login`)
      .flush({ mensaje: 'Credenciales incorrectas' }, { status: 401, statusText: 'Unauthorized' });
    await rechazo;
    http.expectNone(`${environment.apiUrl}/api/auth/me`);
  });
});
