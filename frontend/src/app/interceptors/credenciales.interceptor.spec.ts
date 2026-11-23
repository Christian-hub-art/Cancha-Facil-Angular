import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { firstValueFrom } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from '../services/auth.service';
import { credencialesInterceptor } from './credenciales.interceptor';

const API = environment.apiUrl;
const TOKEN = { headerName: 'X-CSRF-TOKEN', token: 'token-de-prueba' };

describe('credencialesInterceptor', () => {
  let cliente: HttpClient;
  let http: HttpTestingController;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideHttpClient(withInterceptors([credencialesInterceptor])), provideHttpClientTesting()],
    });
    cliente = TestBed.inject(HttpClient);
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('envía la cookie en un GET al backend sin pedir token CSRF', () => {
    cliente.get(`${API}/api/espacios`).subscribe();
    const peticion = http.expectOne(`${API}/api/espacios`);
    expect(peticion.request.withCredentials).toBe(true);
    expect(peticion.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    http.expectNone(`${API}/api/auth/csrf`);
    peticion.flush({});
  });

  it('añade el token CSRF vigente en POST, PUT y DELETE al backend', () => {
    for (const metodo of ['POST', 'PUT', 'DELETE']) {
      cliente.request(metodo, `${API}/api/espacios/1`, { body: {} }).subscribe();
      http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
      const peticion = http.expectOne((r) => r.method === metodo && r.url === `${API}/api/espacios/1`);
      expect(peticion.request.withCredentials).toBe(true);
      expect(peticion.request.headers.get('X-CSRF-TOKEN')).toBe('token-de-prueba');
      peticion.flush({});
    }
  });

  it('no envía cookie ni token a otros orígenes', () => {
    cliente.post('https://api.wompi.co/v1/algo', {}).subscribe();
    const peticion = http.expectOne('https://api.wompi.co/v1/algo');
    expect(peticion.request.withCredentials).toBe(false);
    expect(peticion.request.headers.has('X-CSRF-TOKEN')).toBe(false);
    http.expectNone(`${API}/api/auth/csrf`);
    peticion.flush({});
  });

  it('un 401 del backend cierra la sesión visible y el error llega al componente', async () => {
    const auth = TestBed.inject(AuthService);
    const carga = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush({ email: 'admin@canchafacil.com', roles: ['ADMIN'] });
    await carga;

    const resultado = firstValueFrom(cliente.get(`${API}/api/usuarios`));
    const rechazo = expect(resultado).rejects.toMatchObject({ status: 401 });
    http.expectOne(`${API}/api/usuarios`).flush({ mensaje: 'Debes iniciar sesion' }, { status: 401, statusText: 'Unauthorized' });

    await rechazo;
    expect(auth.sesion()).toBeNull();
  });

  it('un 403 (rol insuficiente) no cierra la sesión', async () => {
    const auth = TestBed.inject(AuthService);
    const carga = auth.cargarSesion();
    http.expectOne(`${API}/api/auth/me`).flush({ email: 'laura@correo.com', roles: ['USER'] });
    await carga;

    const resultado = firstValueFrom(cliente.delete(`${API}/api/espacios/1`));
    const rechazo = expect(resultado).rejects.toMatchObject({ status: 403 });
    http.expectOne(`${API}/api/auth/csrf`).flush(TOKEN);
    http.expectOne(`${API}/api/espacios/1`).flush({ mensaje: 'Acceso denegado' }, { status: 403, statusText: 'Forbidden' });

    await rechazo;
    expect(auth.sesion()?.email).toBe('laura@correo.com');
  });
});
