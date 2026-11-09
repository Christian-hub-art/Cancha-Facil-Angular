import { HttpClient, HttpErrorResponse, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, delay, of, throwError } from 'rxjs';
import { Espacio, Pagina } from '../models/espacio.model';

// Servicio que centraliza la comunicación con el backend para los espacios.
// Los componentes nunca llaman a HttpClient directamente: siempre pasan por aquí.
@Injectable({
  providedIn: 'root',
})
export class EspacioService {
  // inject(): Angular entrega la instancia de HttpClient (no se usa new).
  private http = inject(HttpClient);

  // Dirección del EspacioRestController de Spring Boot.
  private readonly apiUrl = 'http://localhost:8080/api/espacios';

  // true  = usa datos de prueba en memoria (mientras el backend de espacios no está listo).
  // false = hace las peticiones HTTP reales a Spring Boot.
  // Los componentes funcionan igual en los dos casos porque siempre reciben un Observable.
  private readonly usarDatosDePrueba = true;

  // GET /api/espacios?page=0&size=5 -> una página de espacios.
  listar(pagina: number, tamano: number): Observable<Pagina<Espacio>> {
    if (this.usarDatosDePrueba) {
      return this.simular(this.paginarDatosDePrueba(pagina, tamano));
    }
    const params = new HttpParams().set('page', pagina).set('size', tamano);
    return this.http.get<Pagina<Espacio>>(this.apiUrl, { params });
  }

  // GET /api/espacios/{id} -> un solo espacio (se usa antes de editar).
  buscarPorId(id: number): Observable<Espacio> {
    if (this.usarDatosDePrueba) {
      const espacio = this.datosDePrueba.find((e) => e.id === id);
      return espacio ? this.simular({ ...espacio }) : this.errorDePrueba(404);
    }
    return this.http.get<Espacio>(`${this.apiUrl}/${id}`);
  }

  // POST /api/espacios -> crea un espacio y devuelve el espacio guardado (con id).
  crear(espacio: Espacio): Observable<Espacio> {
    if (this.usarDatosDePrueba) {
      const nuevo = { ...espacio, id: this.siguienteId++ };
      this.datosDePrueba.push(nuevo);
      return this.simular({ ...nuevo });
    }
    return this.http.post<Espacio>(this.apiUrl, espacio);
  }

  // PUT /api/espacios/{id} -> actualiza un espacio existente.
  actualizar(id: number, espacio: Espacio): Observable<Espacio> {
    if (this.usarDatosDePrueba) {
      const indice = this.datosDePrueba.findIndex((e) => e.id === id);
      if (indice === -1) {
        return this.errorDePrueba(404);
      }
      this.datosDePrueba[indice] = { ...espacio, id };
      return this.simular({ ...this.datosDePrueba[indice] });
    }
    return this.http.put<Espacio>(`${this.apiUrl}/${id}`, espacio);
  }

  // DELETE /api/espacios/{id} -> elimina un espacio (el backend responde 204 sin cuerpo).
  eliminar(id: number): Observable<void> {
    if (this.usarDatosDePrueba) {
      const indice = this.datosDePrueba.findIndex((e) => e.id === id);
      if (indice === -1) {
        return this.errorDePrueba(404);
      }
      this.datosDePrueba.splice(indice, 1);
      return this.simular(undefined);
    }
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  // ---------------------------------------------------------------------------
  // Datos de prueba (solo mientras usarDatosDePrueba = true)
  // ---------------------------------------------------------------------------

  // Devuelve el valor con medio segundo de espera, como si viniera del servidor.
  // Así también se alcanza a ver el indicador de "Cargando...".
  private simular<T>(valor: T): Observable<T> {
    return of(valor).pipe(delay(500));
  }

  // Simula un error HTTP (por ejemplo 404 cuando el espacio no existe).
  private errorDePrueba(status: number): Observable<never> {
    return throwError(() => new HttpErrorResponse({ status, statusText: 'Error de prueba' })).pipe(
      delay(500),
    );
  }

  // Arma la misma estructura que devuelve Spring Boot con Pageable.
  private paginarDatosDePrueba(pagina: number, tamano: number): Pagina<Espacio> {
    const inicio = pagina * tamano;
    return {
      content: this.datosDePrueba.slice(inicio, inicio + tamano).map((e) => ({ ...e })),
      page: {
        size: tamano,
        number: pagina,
        totalElements: this.datosDePrueba.length,
        totalPages: Math.ceil(this.datosDePrueba.length / tamano),
      },
    };
  }

  private siguienteId = 13;

  private datosDePrueba: Espacio[] = [
    { id: 1, nombre: 'Complejo El Bosque - Cancha 1', tipoDeporte: 'futbol 5', precioHora: 80000, capacidad: 10, descripcion: 'Negocio privado. Grama sintética techada', negocioId: 1 },
    { id: 2, nombre: 'Complejo El Bosque - Cancha 2', tipoDeporte: 'futbol 8', precioHora: 120000, capacidad: 16, descripcion: 'Negocio privado. Al aire libre', negocioId: 1 },
    { id: 3, nombre: 'Canchas La 70 - Cancha A', tipoDeporte: 'futbol 5', precioHora: 70000, capacidad: 10, descripcion: 'Negocio privado. Incluye petos y balón', negocioId: 2 },
    { id: 4, nombre: 'Canchas La 70 - Estadio', tipoDeporte: 'futbol 11', precioHora: 250000, capacidad: 22, descripcion: 'Negocio privado. Cancha con gradería', negocioId: 2 },
    { id: 5, nombre: 'Polideportivo Kennedy - Micro 1', tipoDeporte: 'microfutbol', precioHora: 30000, capacidad: 10, descripcion: 'Cancha pública. Piso en concreto', negocioId: 1 },
    { id: 6, nombre: 'Parque El Tunal - Micro', tipoDeporte: 'microfutbol', precioHora: 25000, capacidad: 10, descripcion: 'Cancha pública con iluminación', negocioId: 2 },
    { id: 7, nombre: 'Colegio San Bartolomé - Micro', tipoDeporte: 'microfutbol', precioHora: 40000, capacidad: 10, descripcion: 'Colegio. Disponible fines de semana', negocioId: 1 },
    { id: 8, nombre: 'Colegio Champagnat - Cancha', tipoDeporte: 'futbol 5', precioHora: 50000, capacidad: 10, descripcion: 'Colegio. Grama sintética', negocioId: 2 },
    { id: 9, nombre: 'Universidad Javeriana - Micro', tipoDeporte: 'microfutbol', precioHora: 45000, capacidad: 10, descripcion: 'Universidad. Coliseo cubierto', negocioId: 1 },
    { id: 10, nombre: 'Universidad Nacional - Cancha 2', tipoDeporte: 'futbol 8', precioHora: 90000, capacidad: 16, descripcion: 'Universidad. Grama natural', negocioId: 2 },
    { id: 11, nombre: 'Parque Simón Bolívar - Micro 3', tipoDeporte: 'microfutbol', precioHora: 20000, capacidad: 10, descripcion: 'Cancha pública', negocioId: 1 },
    { id: 12, nombre: 'Complejo El Bosque - Micro', tipoDeporte: 'microfutbol', precioHora: 60000, capacidad: 10, descripcion: 'Negocio privado. Bajo techo', negocioId: 1 },
  ];
}
