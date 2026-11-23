import { TestBed } from '@angular/core/testing';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { environment } from '../../environments/environment';
import { credencialesInterceptor } from '../interceptors/credenciales.interceptor';
import { AuthService, Sesion, SesionNoConfirmadaError } from './auth.service';

const API = environment.apiUrl;
const LAURA: Sesion = { email: 'laura@correo.com', roles: ['USER'] };
const ADMIN: Sesion = { email: 'admin@canchafacil.com', roles: ['ADMIN'] };
const TOKEN = { headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' };

// Deja correr las promesas pendientes (los await de AuthService).
const esperar = () => new Promise((resolver) => setTimeout(resolver));

describe('AuthService', () => {
  let auth: AuthService;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([credencialesInterceptor])), provideHttpClientTesting()],
    });
    auth = TestBed.inject(AuthService);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('empieza sin sesión y sin haberla consultado', () => {
    expect(auth.sesion()).toBeNull();
    expect(auth.sesionConsultada()).toBe(false);
    expect(auth.cargando()).toBe(true);
  });

  it('login envía formulario con CSRF y cookie, y luego confirma la identidad', async () => {
    const resultado = auth.login('laura@correo.com', 'clave-de-prueba');

    const csrf = http.expectOne(`${API}/api/auth/csrf`);
    expect(csrf.request.withCredentials).toBe(true);
    csrf.flush(TOKEN);

    const login = http.expectOne(`${API}/login`);
    expect(login.request.method).toBe('POST');
    expect(login.request.withCredentials).toBe(true);
    expect(login.request.headers.get('Accept')).toBe('application/json');
    expect(login.request.headers.get('X-CSRF-TOKEN')).toBe('token-de-prueba');
    expect(login.request.body.get('email')).toBe('laura@correo.com');
    expect(login.request.body.get('password')).toBe('clave-de-prueba');
    login.flush({ mensaje: 'Sesion iniciada' });
    await esperar();

    const me = http.expectOne(`${API}/api/auth/me`);
    expect(me.request.withCredentials).toBe(true);
    me.flush(LAURA);

    expect(await resultado).toEqual(LAURA);
    expect(auth.sesion()).toEqual(LAURA);
    expect(auth.autenticado()).toBe(true);
    expect(auth.esAdmin()).toBe(false);
    expect(auth.tieneRol('USER')).toBe(true);
  });

  it('login con credenciales incorrectas propaga el 401 y no consulta la sesión', async () => {
    const resultado = auth.login('laura@correo.com', 'incorrecta');
    const rechazo = expect(resultado).rejects.toMatchObject({ status: 401 });

    http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
    http.expectOne(`${API}/login`).flush({ mensaje: 'Credenciales incorrectas' }, { status: 401, statusText: 'Unauthorized' });

    await rechazo;
    http.expectNone(`${API}/api/auth/me`);
    expect(auth.sesion()).toBeNull();
  });

  it('login aceptado pero sin cookie en /me lanza SesionNoConfirmadaError', async () => {
    const resultado = auth.login('laura@correo.com', 'clave-de-prueba');
    const rechazo = expect(resultado).rejects.toBeInstanceOf(SesionNoConfirmadaError);

    http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
    http.expectOne(`${API}/login`).flush({ mensaje: 'Sesion iniciada' });
    await esperar();
    http.expectOne(`${API}/api/auth/me`).flush({ mensaje: 'Debes iniciar sesion' }, { status: 401, statusText: 'Unauthorized' });

    await rechazo;
    expect(auth.sesion()).toBeNull();
  });

  it('cargarSesion recupera la identidad al recargar (200 de /me)', async () => {
    const resultado = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush(ADMIN);

    expect(await resultado).toEqual(ADMIN);
    expect(auth.esAdmin()).toBe(true);
    expect(auth.sesionConsultada()).toBe(true);
  });

  it('cargarSesion con 401 deja al usuario como visitante ya consultado', async () => {
    const resultado = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush({}, { status: 401, statusText: 'Unauthorized' });

    expect(await resultado).toBeNull();
    expect(auth.sesionConsultada()).toBe(true);
    expect(auth.cargando()).toBe(false);
  });

  it('un error de conexión no borra una sesión ya conocida', async () => {
    const primera = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush(LAURA);
    await primera;

    const segunda = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).error(new ProgressEvent('error'), { status: 0 });

    expect(await segunda).toEqual(LAURA);
  });

  it('comparte una sola petición a /me cuando se pide varias veces a la vez', async () => {
    const a = auth.cargarSesion();
    const b = auth.asegurarSesion();
    http.expectOne(`${API}/api/auth/me`).flush(LAURA);

    expect(await a).toEqual(LAURA);
    expect(await b).toEqual(LAURA);
    // Ya consultada: asegurarSesion responde sin otra petición.
    expect(await auth.asegurarSesion()).toEqual(LAURA);
  });

  it('logout envía CSRF, cierra la sesión en el servidor y limpia el estado', async () => {
    const carga = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush(LAURA);
    await carga;

    const resultado = auth.logout();
    http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
    const logout = http.expectOne(`${API}/logout`);
    expect(logout.request.method).toBe('POST');
    expect(logout.request.withCredentials).toBe(true);
    expect(logout.request.headers.get('X-CSRF-TOKEN')).toBe('token-de-prueba');
    expect(logout.request.headers.get('Accept')).toBe('application/json');
    logout.flush(null, { status: 204, statusText: 'No Content' });

    expect(await resultado).toBe(true);
    expect(auth.sesion()).toBeNull();
  });

  it('logout sin servidor limpia el estado visible y devuelve false', async () => {
    const resultado = auth.logout();
    http.expectOne(`${API}/api/auth/csrf`).error(new ProgressEvent('error'), { status: 0 });

    expect(await resultado).toBe(false);
    expect(auth.sesion()).toBeNull();
  });

  it('una respuesta de /me pedida antes del logout no revive la sesión', async () => {
    const consultaVieja = auth.cargarSesion();
    const salida = auth.logout();
    http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
    http.expectOne(`${API}/logout`).flush(null, { status: 204, statusText: 'No Content' });
    await salida;

    http.expectOne(`${API}/api/auth/me`).flush(LAURA);
    await consultaVieja;
    expect(auth.sesion()).toBeNull();
  });
});
