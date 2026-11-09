import { Component, output, input, OnChanges } from '@angular/core'; // output() permite enviar datos al componente padre.
import { FormsModule } from '@angular/forms'; // Habilita [(ngModel)] y (ngSubmit) en el formulario.
import { Usuario } from '../../models/usuario.model'; // Estructura de cada usuario.

@Component({
  standalone: true,
  imports: [FormsModule],
  selector: 'app-usuario-form',
  styleUrl: './usuario-form.component.css',
  templateUrl: './usuario-form.component.html',
})
export class UsuarioForm implements OnChanges {
  // HIJO -> PADRE: evento que envía el usuario capturado.
  usuarioGuardado = output<Usuario>();
  //padre -> hijo : evento que recibe el usuario a editar
  usuarioEditar = input<Usuario | null> (null)
  //padre -> hijo desactivar el boton cuando le da click
  cargando = input(false)

  // Campos reales de la interface Usuario del proyecto.
  
  nombre: string = '';
  email: string = '';
  telefono: string = '';
  direccion: string = '';
  rol: 'ADMINISTRADOR' | 'CLIENTE' = 'CLIENTE';
  password: string = '';

  ngOnChanges(): void {
  const usuario = this.usuarioEditar();

  if (usuario) {
    this.nombre = usuario.nombre;
    this.email = usuario.email;
    this.telefono = usuario.telefono;
    this.direccion = usuario.direccion;
    this.rol = usuario.rol;
    this.password = '';
  } else {
    this.nombre = '';
    this.email = '';
    this.telefono = '';
    this.direccion = '';
    this.rol = 'CLIENTE';
    this.password = '';
  }
}

  guardar(): void {
    const usuario: Usuario = {
      nombre: this.nombre,
      email: this.email,
      telefono: this.telefono,
      rol: this.rol,
      direccion: this.direccion,
      password: this.password
    };

    // emit(...) envía el usuario al componente padre.
    this.usuarioGuardado.emit(usuario);
  }
}
