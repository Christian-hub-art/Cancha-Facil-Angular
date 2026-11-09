import { Component } from '@angular/core'; // Permite definir el componente Angular.
import { Credenciales, LoginForm } from '../login-form/login-form.component'; // Componente hijo y estructura de las credenciales.

@Component({
  standalone: true,
  imports: [LoginForm],
  selector: 'app-login',
  styleUrl: './login.component.css',
  templateUrl: './login.component.html',
})
export class Login {
  // Correo recibido desde el hijo; solo se usa para mostrar que llegó la información.
  correoRecibido = '';

  // $event trae las credenciales que el hijo envió con emit().
  procesarLogin(credenciales: Credenciales): void {
    // El inicio de sesión real del proyecto lo procesa Spring Security en POST /login
    // (ver SeguridadConfig); Angular todavía no tiene un servicio de login que llamar,
    // por eso aquí solo se recibe la información. La contraseña no se guarda ni se muestra.
    this.correoRecibido = credenciales.email;
  }
}
