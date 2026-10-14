import { Injectable } from '@angular/core';
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
      fechaRegistro: '2026-09-07',
    },
    {
      id: 2,
      nombre: 'Laura Gómez',
      email: 'laura@correo.com',
      telefono: '3001234567',
      rol: 'CLIENTE',
      fechaRegistro: '2026-09-15',
    },
    {
      id: 3,
      nombre: 'Mateo Ruiz',
      email: 'mateo@canchafacil.com',
      telefono: '3207654321',
      rol: 'ADMINISTRADOR',
      fechaRegistro: '2026-08-20',
    },
    {
      id: 4,
      nombre: 'Valentina López',
      email: 'valentina@correo.com',
      telefono: '3119876543',
      rol: 'CLIENTE',
      fechaRegistro: '2026-09-24',
    },
  ];

  agregarUsuario(usuario: Usuario): void {
    // El nuevo usuario todavía no tiene id: se asigna el siguiente disponible.
    const siguienteId = Math.max(0, ...this.usuarios.map((u) => u.id ?? 0)) + 1;
    this.usuarios.push({ ...usuario, id: siguienteId });
  }

  obtenerUsuarios(): Usuario[] {
    return this.usuarios;
  }
}
