import { Component, output } from '@angular/core'; // output() permite enviar datos al componente padre.
import { FormsModule } from '@angular/forms'; // Habilita [(ngModel)] y (ngSubmit) en el formulario.
import { Usuario } from '../../models/usuario.model'; // Estructura de cada usuario.

@Component({
  standalone: true,
  imports: [FormsModule],
  selector: 'app-usuario-form',
  styleUrl: './usuario-form.component.css',
  templateUrl: './usuario-form.component.html',
})
export class UsuarioForm {
  // HIJO -> PADRE: evento que envía el usuario capturado.
  usuarioGuardado = output<Usuario>();

  // Campos reales de la interface Usuario del proyecto.
  nombre: string = '';
  email: string = '';
  telefono: string = '';
  direccion: string = '';
  rol: 'ADMINISTRADOR' | 'CLIENTE' = 'CLIENTE';
  fechaRegistro: string = '';

  guardar(): void {
    const usuario: Usuario = {
      nombre: this.nombre,
      email: this.email,
      telefono: this.telefono,
      rol: this.rol,
      direccion: this.direccion,
      fechaRegistro: this.fechaRegistro,
    };

    // emit(...) envía el usuario al componente padre.
    this.usuarioGuardado.emit(usuario);
  }
}
