import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Usuario } from '../models/usuario.model';
// Servicio con la lógica de los usuarios. Mientras Angular no se conecte al
// backend (/api/usuarios), guarda los usuarios temporalmente en un arreglo.
@Injectable({
  providedIn: 'root',
})
export class UsuarioService {
  // Datos de ejemplo que antes estaban en usuarios.component.ts.
  private usuarios: Usuario[] = [
    {
      id: 1,
      nombre: 'Christian Medina',
      email: 'christian@javeriana.edu.co',
      telefono: '3132178001',
      rol: 'CLIENTE',
      direccion: 'Calle 123 #45-67, Bogotá, Colombia',
      fechaRegistro: '2026-08-07',
    },
    {
      id: 2,
      nombre: 'Laura Gómez',
      email: 'laura@correo.com',
      telefono: '3001234567',
      rol: 'CLIENTE',
      direccion: 'Calle 123 #45-67, Bogotá, Colombia',
      fechaRegistro: '2026-08-07',
    },
    {
      id: 3,
      nombre: 'Mateo Ruiz',
      email: 'mateo@canchafacil.com',
      telefono: '3207654321',
      rol: 'ADMINISTRADOR',
      direccion: 'Calle 123 #45-67, Bogotá, Colombia',
      fechaRegistro: '2026-08-07',
    },
    {
      id: 4,
      nombre: 'Valentina López',
      email: 'valentina@correo.com',
      telefono: '3119876543',
      rol: 'CLIENTE',
      direccion: 'Calle 123 #45-67, Bogotá, Colombia',
      fechaRegistro: '2026-08-07',
    },
  ];

  agregarUsuario(usuario: Usuario): void {
    // El nuevo usuario todavía no tiene id: se asigna el siguiente disponible.
    const siguienteId = Math.max(0, ...this.usuarios.map((u) => u.id ?? 0)) + 1;
    this.usuarios.push({ ...usuario, id: siguienteId });
  }

  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/usuarios';

  obtenerUsuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.url);
  }

  //punto 3 del taller
  consultarPorId(id: number): Observable<Usuario>{
    return this.http.get<Usuario>(`${this.url}/${id}`);
  }  
}
