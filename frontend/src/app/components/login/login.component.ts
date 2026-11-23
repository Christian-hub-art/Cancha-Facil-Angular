import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, signal } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { rutaDeRegresoSegura } from '../../guards/auth.guard';
import { AuthService, SesionNoConfirmadaError } from '../../services/auth.service';
import { Credenciales, LoginForm } from '../login-form/login-form.component'; // Componente hijo y estructura de las credenciales.

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [LoginForm],
  styleUrl: './login.component.css',
  templateUrl: './login.component.html',
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  private readonly ruta = inject(ActivatedRoute);

  // Ruta que el guard intentó abrir antes de pedir el login (?returnUrl=/usuarios).
  private readonly returnUrl = this.ruta.snapshot.queryParamMap.get('returnUrl');

  // Aviso cuando un guard trajo aquí al usuario.
  readonly aviso = this.returnUrl ? 'Inicia sesión para continuar.' : '';
  readonly mensajeError = signal('');
  readonly enviando = signal(false);

  // $event trae las credenciales que el hijo envió con emit().
  // La contraseña solo se reenvía al backend: no se guarda ni se escribe en consola.
  async procesarLogin(credenciales: Credenciales): Promise<void> {
    if (this.enviando()) return; // Evita envíos dobles.

    const email = credenciales.email.trim();
    if (!email || !credenciales.password) {
      this.mensajeError.set('Escribe tu correo y tu contraseña.');
      return;
    }

    this.enviando.set(true);
    this.mensajeError.set('');
    try {
      await this.auth.login(email, credenciales.password);
      await this.router.navigateByUrl(rutaDeRegresoSegura(this.returnUrl));
    } catch (error) {
      this.mensajeError.set(this.describirError(error));
    } finally {
      this.enviando.set(false);
    }
  }

  private describirError(error: unknown): string {
    if (error instanceof SesionNoConfirmadaError) {
      return 'El servidor aceptó el ingreso, pero el navegador no conservó la sesión. Revisa la configuración de cookies y CORS.';
    }
    if (error instanceof HttpErrorResponse) {
      switch (error.status) {
        case 401:
          return 'Correo o contraseña incorrectos.';
        case 0:
          return 'No se pudo conectar con el servidor. Revisa que Spring Boot esté encendido.';
        case 403:
          return 'La verificación de seguridad expiró. Recarga la página e inténtalo de nuevo.';
      }
    }
    return 'No se pudo iniciar sesión. Inténtalo de nuevo en unos minutos.';
  }
}
