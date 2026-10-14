import { CommonModule, DatePipe } from '@angular/common'; // Importa utilidades comunes y el pipe para fechas.
import { Component, inject } from '@angular/core'; // Permite definir el componente Angular y obtener servicios con inject().
import { FormsModule } from '@angular/forms'; // Habilita el enlace de datos de los formularios.
import { Usuario } from '../../models/usuario.model'; // Estructura de cada usuario (antes estaba definida aquí).
import { UsuarioService } from '../../services/usuario.service'; // Servicio con la lógica de usuarios.
import { UsuarioForm } from '../usuario-form/usuario-form.component'; // Componente hijo: formulario de usuario.

@Component({// decorador: patron de diseño de software que permite agregar funcionalidades a una clase 
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, UsuarioForm],
  selector: 'app-usuarios',
  styleUrl: './usuarios.component.css',
  templateUrl: './usuarios.component.html',
})
export class Usuarios {
  filtro = '';

  // Angular entrega la instancia de UsuarioService (no se usa new UsuarioService()).
  usuarioService = inject(UsuarioService);

  // Los datos de ejemplo ahora viven en UsuarioService.
  usuarios: Usuario[] = this.usuarioService.obtenerUsuarios();

  // Controla si se muestra el formulario de nuevo usuario.
  mostrarFormulario = false;

  get usuariosFiltrados(): Usuario[] {
    const texto = this.filtro.trim().toLowerCase();

    if (!texto) {
      return this.usuarios;
    }

    return this.usuarios.filter((usuario) =>
      usuario.nombre.toLowerCase().includes(texto) ||
      usuario.email.toLowerCase().includes(texto) ||
      usuario.rol.toLowerCase().includes(texto)
    );
  }

  limpiarFiltro(): void {
    this.filtro = '';
  }

  // $event trae el usuario que el formulario hijo envió con emit().
  guardarUsuario(usuario: Usuario): void {
    this.usuarioService.agregarUsuario(usuario);
    this.mostrarFormulario = false;
  }
}
