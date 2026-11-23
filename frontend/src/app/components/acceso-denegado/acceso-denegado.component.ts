import { Component, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../services/auth.service';

// Destino acordado cuando un usuario con sesión abre una ruta sin el rol necesario
// (lo envía rolGuard). Es el equivalente visual del 403 que daría el backend.
@Component({
  selector: 'app-acceso-denegado',
  standalone: true,
  imports: [RouterLink],
  template: `
    <div class="pagina-cabecera">
      <div>
        <h1>Acceso denegado</h1>
        <p>Esta sección es solo para administradores.</p>
      </div>
    </div>

    <div class="tarjeta denegado">
      <div class="alerta alerta-error" role="alert">
        @if (auth.sesion(); as sesion) {
          La cuenta {{ sesion.email }} no tiene permiso para ver esta página.
        } @else {
          No tienes permiso para ver esta página.
        }
      </div>
      <a class="btn btn-neutro btn-sm" routerLink="/espacios">Volver a las canchas</a>
    </div>
  `,
  styles: `
    .denegado { padding: 1.5rem; display: grid; gap: 1rem; max-width: 560px; }
    .btn { justify-self: start; }
  `,
})
export class AccesoDenegado {
  readonly auth = inject(AuthService);
}
