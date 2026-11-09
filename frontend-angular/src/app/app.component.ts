import { Component, DestroyRef, inject, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs';

// Componente raíz: tiene la barra de navegación y el <router-outlet>,
// donde el router pinta el componente de la ruta actual.
@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.component.css',
  templateUrl: './app.component.html',
})
export class App {
  // inject(): Angular entrega el Router para escuchar los cambios de página.
  private readonly router = inject(Router);
  private readonly destroyRef = inject(DestroyRef);

  // Signals: contador de visitas a las páginas de la aplicación.
  // signal(0) crea el valor inicial; la vista lo lee con visitas().
  readonly visitas = signal(0);

  // Ruta actual, solo para mostrarla en la barra.
  readonly rutaActual = signal('');

  constructor() {
    // Cada vez que termina una navegación se suma una visita con update().
    this.router.events
      .pipe(
        filter((evento) => evento instanceof NavigationEnd),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((evento) => {
        this.visitas.update((valor) => valor + 1); // update(): calcula el nuevo valor a partir del anterior
        this.rutaActual.set((evento as NavigationEnd).urlAfterRedirects); // set(): reemplaza el valor
      });
  }

  // (click) del botón «Reiniciar»: set() deja el contador en 0.
  reiniciarVisitas(): void {
    this.visitas.set(0);
  }
}
