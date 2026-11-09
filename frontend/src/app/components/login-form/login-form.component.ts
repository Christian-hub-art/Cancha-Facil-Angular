import { Component, input, output } from '@angular/core'; // input() y output() permiten la comunicación padre-hijo.
import { FormsModule } from '@angular/forms'; // Habilita [(ngModel)] y (ngSubmit) en el formulario.

// Datos que la persona escribe para intentar iniciar sesión.
// Se usan los mismos nombres del login del proyecto (SeguridadConfig: "email" y "password").
export interface Credenciales {
  email: string; // Correo electrónico con el que se inicia sesión.
  password: string; // Contraseña escrita en el formulario (no se guarda ni se muestra).
}

@Component({
  standalone: true,
  imports: [FormsModule],
  selector: 'app-login-form',
  styleUrl: './login-form.component.css',
  templateUrl: './login-form.component.html',
})
export class LoginForm {
  // PADRE -> HIJO: el padre puede enviar el título que muestra el formulario.
  titulo = input<string>('Iniciar sesión');

  // HIJO -> PADRE: evento que envía las credenciales al componente padre.
  loginEnviado = output<Credenciales>();

  email: string = '';
  password: string = '';

  enviarLogin(): void {
    // emit(...) envía la información al componente padre.
    this.loginEnviado.emit({
      email: this.email,
      password: this.password,
    });
  }
}
