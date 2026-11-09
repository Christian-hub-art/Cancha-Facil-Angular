import { CurrencyPipe, registerLocaleData } from '@angular/common';
import localeEsCo from '@angular/common/locales/es-CO';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { switchMap, take, takeWhile, timer } from 'rxjs';
import { CLAVE_ULTIMA_ORDEN } from './boton-pagar.component';
import { Orden, PagoService } from './pago.service';

registerLocaleData(localeEsCo);

// Cada cuánto se pregunta al backend y cuántas veces como máximo (3 s x 40 = 2 min).
const INTERVALO_MS = 3000;
const MAX_CONSULTAS = 40;

// Página a la que Wompi devuelve al usuario: /pago/resultado?ref=ORD-...&id=<transacción>
// Que el usuario vuelva NO confirma el pago: la confirmación la pone el webhook
// en el backend. Por eso esta página consulta la orden hasta que deja de estar PENDING.
@Component({
  selector: 'app-pago-resultado',
  standalone: true,
  imports: [CurrencyPipe, RouterLink],
  template: `
    <div class="pagina-cabecera">
      <div>
        <h1>Resultado del pago</h1>
        @if (referencia) {
          <p>Orden {{ referencia }}</p>
        }
      </div>
    </div>

    <div class="tarjeta resultado">
      @switch (estado()) {
        @case ('PENDING') {
          <div class="alerta alerta-info"><span class="spinner"></span> Estamos confirmando tu pago…</div>
        }
        @case ('APPROVED') {
          <div class="alerta alerta-exito">¡Pago aprobado!</div>
        }
        @case ('DECLINED') {
          <div class="alerta alerta-error">El pago fue rechazado. No se pudo completar.</div>
        }
        @case ('VOIDED') {
          <div class="alerta alerta-error">El pago fue anulado.</div>
        }
        @case ('ERROR') {
          <div class="alerta alerta-error">El pago no se pudo completar.</div>
        }
        @case ('SIN_CONFIRMAR') {
          <div class="alerta alerta-error">
            Wompi todavía no confirma el pago. Revisa más tarde: la orden se actualiza sola cuando llegue el aviso.
          </div>
        }
        @default {
          <div class="alerta alerta-error">{{ mensajeError() }}</div>
        }
      }

      @if (orden(); as o) {
        <dl class="detalle">
          <dt>Monto</dt>
          <dd>{{ o.montoEnCentavos / 100 | currency: 'COP' : 'symbol-narrow' : '1.0-0' : 'es-CO' }}</dd>
          @if (o.descripcion) {
            <dt>Concepto</dt>
            <dd>{{ o.descripcion }}</dd>
          }
          @if (o.transaccionId || transaccionWompi) {
            <dt>Transacción Wompi</dt>
            <dd>{{ o.transaccionId || transaccionWompi }}</dd>
          }
        </dl>
      }

      <a class="btn btn-neutro btn-sm" routerLink="/espacios">Volver a las canchas</a>
    </div>
  `,
  styles: `
    .resultado { padding: 1.5rem; display: grid; gap: 1rem; max-width: 560px; }
    .detalle { display: grid; grid-template-columns: max-content 1fr; gap: 0.4rem 1rem; margin: 0; }
    .detalle dt { font-weight: 600; color: #475569; }
    .detalle dd { margin: 0; overflow-wrap: anywhere; }
    .btn { justify-self: start; }
  `,
})
export class PagoResultadoComponent implements OnInit {
  private ruta = inject(ActivatedRoute);
  private pagoService = inject(PagoService);
  private destroyRef = inject(DestroyRef);

  referencia: string | null = null;
  transaccionWompi: string | null = null;

  // PENDING mientras se espera; luego el estado final, SIN_CONFIRMAR o FALLO.
  estado = signal<string>('PENDING');
  orden = signal<Orden | null>(null);
  mensajeError = signal('No se encontró la orden de pago.');

  ngOnInit(): void {
    const params = this.ruta.snapshot.queryParamMap;
    // Wompi agrega ?id=<transacción>; si lo pega con "?" en lugar de "&", se corta aquí.
    this.referencia = params.get('ref')?.split('?')[0] || this.leerUltimaOrden();
    this.transaccionWompi = params.get('id');

    if (!this.referencia) {
      this.estado.set('FALLO');
      return;
    }
    this.consultarHastaTenerResultado(this.referencia);
  }

  // Pregunta cada 3 s hasta que la orden deje de estar PENDING (o se acaben los intentos).
  private consultarHastaTenerResultado(referencia: string): void {
    let consultas = 0;
    timer(0, INTERVALO_MS)
      .pipe(
        take(MAX_CONSULTAS),
        switchMap(() => this.pagoService.consultarOrden(referencia)),
        takeWhile((orden) => orden.estado === 'PENDING', true),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: (orden) => {
          consultas++;
          this.orden.set(orden);
          this.estado.set(orden.estado);
          if (orden.estado === 'PENDING' && consultas >= MAX_CONSULTAS) {
            this.estado.set('SIN_CONFIRMAR');
          }
          if (orden.estado !== 'PENDING') {
            this.olvidarUltimaOrden();
          }
        },
        error: (err: HttpErrorResponse) => {
          this.mensajeError.set(
            err.status === 0 ? 'No se pudo conectar con el servidor.' : 'No se encontró la orden de pago.',
          );
          this.estado.set('FALLO');
        },
      });
  }

  private leerUltimaOrden(): string | null {
    try {
      return sessionStorage.getItem(CLAVE_ULTIMA_ORDEN);
    } catch {
      return null;
    }
  }

  private olvidarUltimaOrden(): void {
    try {
      sessionStorage.removeItem(CLAVE_ULTIMA_ORDEN);
    } catch {
      // Nada que limpiar.
    }
  }
}
