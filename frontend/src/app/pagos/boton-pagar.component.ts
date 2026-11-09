import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeEsCo from '@angular/common/locales/es-CO';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, inject, input, signal } from '@angular/core';
import { PagoService } from './pago.service';

registerLocaleData(localeEsCo);

// Clave con la que se recuerda la última orden mientras el usuario está en Wompi.
export const CLAVE_ULTIMA_ORDEN = 'canchafacil.ultimaOrden';

// Botón "Pagar con Wompi". Uso:
//   <app-boton-pagar [monto]="15000" descripcion="1 hora en Cancha Bosque 1" />
// El monto va en pesos; el backend lo convierte a centavos.
@Component({
  selector: 'app-boton-pagar',
  standalone: true,
  imports: [CurrencyPipe],
  template: `
    <button class="btn btn-primario btn-sm btn-pagar" type="button" (click)="pagar()" [disabled]="cargando()">
      @if (cargando()) {
        <span class="spinner"></span> Abriendo Wompi…
      } @else {
        Pagar {{ monto() | currency: 'COP' : 'symbol-narrow' : '1.0-0' : 'es-CO' }} con Wompi
      }
    </button>
    @if (error()) {
      <p class="error-pago" role="alert">{{ error() }}</p>
    }
  `,
  styles: `
    :host { display: block; }
    .btn-pagar { width: 100%; justify-content: center; }
    .error-pago { margin: 0.4rem 0 0; font-size: 0.78rem; color: #b42318; }
  `,
})
export class BotonPagarComponent {
  private pagoService = inject(PagoService);

  // input(): el padre envía el monto en pesos y, opcional, qué se está pagando.
  monto = input.required<number>();
  descripcion = input<string>();

  cargando = signal(false);
  error = signal<string | null>(null);

  pagar(): void {
    this.cargando.set(true);
    this.error.set(null);

    this.pagoService.iniciarPago(this.monto(), this.descripcion()).subscribe({
      next: (respuesta) => {
        // Se guarda la referencia por si Wompi no la devuelve en la URL de regreso.
        try {
          sessionStorage.setItem(CLAVE_ULTIMA_ORDEN, respuesta.referencia);
        } catch {
          // Sin sessionStorage igual funciona: la referencia viaja en ?ref=.
        }
        // Se sale de Angular hacia la página de pago de Wompi.
        window.location.href = respuesta.urlPago;
      },
      error: (err: HttpErrorResponse) => {
        this.cargando.set(false);
        if (err.status === 0) {
          this.error.set('No se pudo conectar con el servidor.');
        } else if (err.status === 503) {
          this.error.set('Los pagos no están configurados en el servidor.');
        } else {
          this.error.set(err.error?.mensaje ?? 'No se pudo iniciar el pago.');
        }
      },
    });
  }
}
