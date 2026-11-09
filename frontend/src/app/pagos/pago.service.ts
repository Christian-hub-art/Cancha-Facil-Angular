import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

// Lo que devuelve POST /api/pagos/checkout.
export interface CheckoutRespuesta {
  referencia: string; // Ej: ORD-3f2a...
  montoEnCentavos: number; // $15.000 -> 1500000
  urlPago: string; // Link firmado del Web Checkout de Wompi.
}

// Lo que devuelve GET /api/pagos/ordenes/{referencia}.
export interface Orden {
  referencia: string;
  estado: 'PENDING' | 'APPROVED' | 'DECLINED' | 'VOIDED' | 'ERROR';
  montoEnCentavos: number;
  moneda: string;
  descripcion: string | null;
  transaccionId: string | null;
}

// Habla con el backend: pide el link de pago y consulta el estado de la orden.
// Las llaves de Wompi nunca llegan a Angular; las usa solo el backend.
@Injectable({ providedIn: 'root' })
export class PagoService {
  private http = inject(HttpClient);
  private url = `${environment.apiUrl}/api/pagos`;

  // POST: crea la orden (PENDING) y recibe el link de pago. El monto va en pesos.
  iniciarPago(monto: number, descripcion?: string): Observable<CheckoutRespuesta> {
    return this.http.post<CheckoutRespuesta>(`${this.url}/checkout`, { monto, descripcion });
  }

  // GET: estado actual de la orden. Lo cambia el webhook cuando Wompi avisa.
  consultarOrden(referencia: string): Observable<Orden> {
    return this.http.get<Orden>(`${this.url}/ordenes/${encodeURIComponent(referencia)}`);
  }
}
