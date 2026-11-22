import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../../services/auth.service';
import { Credenciales, LoginForm } from '../login-form/login-form.component'; // Componente hijo y estructura de las credenciales.

@Component({
  standalone: true,
  imports: [LoginForm],
  selector: 'app-login',
  styleUrl: './login.component.css',
  templateUrl: './login.component.html',
})
export class Login {
  private auth = inject(AuthService);
  private router = inject(Router);

  mensaje = signal('');
  enviando = signal(false);

  async procesarLogin(credenciales: Credenciales): Promise<void> {
    if (this.enviando()) return;

    this.enviando.set(true);
    this.mensaje.set('');

    try {
      await this.auth.login(credenciales.email, credenciales.password);
      await this.router.navigate(['/espacios']);
    } catch (error) {
      this.mensaje.set(
        error instanceof HttpErrorResponse && error.status === 401
          ? 'Correo o contraseña incorrectos.'
          : 'No se pudo iniciar sesión. Revisa la conexión con el backend.'
      );
    } finally {
      this.enviando.set(false);
    }
  }
}
