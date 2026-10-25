import { CurrencyPipe, UpperCasePipe, registerLocaleData } from '@angular/common'; // Pipes para mostrar moneda y mayúsculas.
import localeEsCo from '@angular/common/locales/es-CO';
import { HttpErrorResponse } from '@angular/common/http';
import { Component, OnInit, inject, signal } from '@angular/core';
import { Espacio } from '../../models/espacio.model';
import { EspacioService } from '../../services/espacio.service';
import { EspacioForm } from '../espacio-form/espacio-form.component'; // Componente hijo.

// Formato colombiano para el pipe currency: $ 80.000 en lugar de $80,000.
registerLocaleData(localeEsCo);

// Mensaje que se muestra arriba de la tabla después de cada operación.
interface Mensaje {
  tipo: 'exito' | 'error';
  texto: string;
}

@Component({
  standalone: true,
  imports: [CurrencyPipe, UpperCasePipe, EspacioForm],
  selector: 'app-espacios',
  styleUrl: './espacios.component.css',
  templateUrl: './espacios.component.html',
})
export class Espacios implements OnInit {
  // inject(): Angular entrega el servicio que habla con el backend.
  private espacioService = inject(EspacioService);

  // Cuántos espacios se muestran por página.
  readonly tamanoPagina = 5;

  // Estado de la pantalla. Se usan signals porque los datos llegan después
  // (cuando responde el backend) y así Angular actualiza la vista sola.
  espacios = signal<Espacio[]>([]);
  paginaActual = signal(0); // Empieza en 0, como en Spring Boot.
  totalPaginas = signal(0);
  totalElementos = signal(0);
  cargando = signal(false);
  mensaje = signal<Mensaje | null>(null);
  mostrarFormulario = signal(false);
  espacioSeleccionado = signal<Espacio | null>(null); // Se envía al hijo con input().

  ngOnInit(): void {
    this.cargarPagina(0);
  }

  // GET paginado: pide al backend una página de espacios.
  cargarPagina(numero: number): void {
    this.cargando.set(true);
    this.espacioService.listar(numero, this.tamanoPagina).subscribe({
      next: (respuesta) => {
        this.espacios.set(respuesta.content);
        this.paginaActual.set(respuesta.page.number);
        this.totalPaginas.set(respuesta.page.totalPages);
        this.totalElementos.set(respuesta.page.totalElements);
        this.cargando.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.mostrarError(error, 'No se pudieron cargar los espacios.');
        this.cargando.set(false);
      },
    });
  }

  paginaAnterior(): void {
    if (this.paginaActual() > 0) {
      this.cargarPagina(this.paginaActual() - 1);
    }
  }

  paginaSiguiente(): void {
    if (this.paginaActual() < this.totalPaginas() - 1) {
      this.cargarPagina(this.paginaActual() + 1);
    }
  }

  nuevoEspacio(): void {
    this.espacioSeleccionado.set(null); // null = el formulario queda vacío para crear.
    this.mostrarFormulario.set(true);
    this.subirAlFormulario();
  }

  // GET por id: trae el espacio actualizado del backend y lo envía al hijo para editarlo.
  editarEspacio(espacio: Espacio): void {
    this.cargando.set(true);
    this.espacioService.buscarPorId(espacio.id!).subscribe({
      next: (encontrado) => {
        this.espacioSeleccionado.set(encontrado);
        this.mostrarFormulario.set(true);
        this.cargando.set(false);
        this.subirAlFormulario();
      },
      error: (error: HttpErrorResponse) => {
        this.mostrarError(error, 'No se pudo abrir el espacio.');
        this.cargando.set(false);
      },
    });
  }

  cerrarFormulario(): void {
    this.mostrarFormulario.set(false);
    this.espacioSeleccionado.set(null);
  }

  // $event trae el espacio que el hijo envió con emit().
  // Si tiene id es una edición (PUT); si no, es uno nuevo (POST).
  guardarEspacio(espacio: Espacio): void {
    const esEdicion = espacio.id !== undefined;
    const peticion = esEdicion
      ? this.espacioService.actualizar(espacio.id!, espacio)
      : this.espacioService.crear(espacio);

    this.cargando.set(true);
    peticion.subscribe({
      next: (guardado) => {
        this.mensaje.set({
          tipo: 'exito',
          texto: esEdicion
            ? `Se actualizó "${guardado.nombre}".`
            : `Se creó "${guardado.nombre}".`,
        });
        this.cerrarFormulario();
        // Al crear se va a la última página, que es donde queda el espacio nuevo.
        const pagina = esEdicion
          ? this.paginaActual()
          : Math.floor(this.totalElementos() / this.tamanoPagina);
        this.cargarPagina(pagina);
      },
      error: (error: HttpErrorResponse) => {
        this.mostrarError(error, 'No se pudo guardar el espacio.');
        this.cargando.set(false);
      },
    });
  }

  // DELETE: elimina el espacio después de confirmar.
  eliminarEspacio(espacio: Espacio): void {
    if (!confirm(`¿Eliminar "${espacio.nombre}"?`)) {
      return;
    }
    this.cargando.set(true);
    this.espacioService.eliminar(espacio.id!).subscribe({
      next: () => {
        this.mensaje.set({ tipo: 'exito', texto: `Se eliminó "${espacio.nombre}".` });
        // Si era el último de la página, se vuelve a la página anterior.
        const quedaVacia = this.espacios().length === 1 && this.paginaActual() > 0;
        this.cargarPagina(quedaVacia ? this.paginaActual() - 1 : this.paginaActual());
      },
      error: (error: HttpErrorResponse) => {
        this.mostrarError(error, 'No se pudo eliminar el espacio.');
        this.cargando.set(false);
      },
    });
  }


  // El formulario está arriba de las tarjetas: sube la página para que se vea.
  private subirAlFormulario(): void {
    window.scrollTo({ top: 0, behavior: 'smooth' });
  }

  // Elige el mensaje según el código HTTP que devolvió el backend.
  private mostrarError(error: HttpErrorResponse, textoPorDefecto: string): void {
    let texto = textoPorDefecto;
    if (error.status === 0) {
      texto = 'No se pudo conectar con el servidor. Revisa que Spring Boot esté encendido.';
    } else if (error.status === 404) {
      texto = 'El espacio no existe o ya fue eliminado.';
    } else if (error.status === 400) {
      texto = 'Los datos enviados no son válidos.';
    }
    this.mensaje.set({ tipo: 'error', texto });
  }
}
