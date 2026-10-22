
import { AsyncPipe, CommonModule, DatePipe } from '@angular/common';
import { Component, inject, signal, OnInit, OnDestroy } from '@angular/core';
import { FormsModule } from '@angular/forms';

import {
  catchError,
  of,
  Subscription,
  range,
  Observable,
  from,
  Subject,
  switchMap,
  defer,
  tap,
  concatMap,
  mergeMap,
  startWith,
  exhaustMap,
  shareReplay,
  timer,
  interval
} from 'rxjs';

import { Usuario } from '../../models/usuario.model';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioForm } from '../usuario-form/usuario-form.component';



import { FormControl, ReactiveFormsModule } from '@angular/forms';

import {
  combineLatest,
  debounceTime,
  distinctUntilChanged,
  map
} from 'rxjs';



import { forkJoin } from 'rxjs';




@Component({
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    DatePipe,
    UsuarioForm,
    AsyncPipe
  ],
  selector: 'app-usuarios',
  styleUrl: './usuarios.component.css',
  templateUrl: './usuarios.component.html',
})

export class Usuarios implements OnInit, OnDestroy {

  filtro = '';

  private readonly usuarioService = inject(UsuarioService);

  usuarios = signal<Usuario[]>([]);
  mensajeError = signal('');

  private readonly suscripciones = new Subscription();

  // PUNTO 1 - CONSULTAR USUARIOS

  cargarUsuarios() {
    this.mensajeError.set('');

    this.suscripciones.add(
      this.usuarioService.obtenerUsuarios()
        .pipe(
          catchError(error => {
            console.error(error);
            this.mensajeError.set('Error al cargar los usuarios');
            return of([] as Usuario[]);
          })
        )
        .subscribe(lista => this.usuarios.set(lista))
    );
  }

  // FUNCIONALIDADES ORIGINALES

  mostrarFormulario = false;

  get usuariosFiltrados(): Usuario[] {
    const texto = this.filtro.trim().toLowerCase();
    const usuarios = this.usuarios();

    if (!texto) {
      return usuarios;
    }

    return usuarios.filter(
      usuario =>
        usuario.nombre.toLowerCase().includes(texto) ||
        usuario.email.toLowerCase().includes(texto) ||
        usuario.rol.toLowerCase().includes(texto)
    );
  }

  limpiarFiltro(): void {
    this.filtro = '';
  }

  guardarUsuario(usuario: Usuario): void {
    this.usuarioService.agregarUsuario(usuario);
    this.mostrarFormulario = false;
  }

  // PUNTO 2 - OBSERVABLES

  examinarUsuario() {
    const datos = this.usuarios();

    of(datos).subscribe(lista => {
      console.log('OF: total', lista.length);
    });

    from(datos).subscribe(usuario => {
      console.log(
        'FROM: id:', usuario.id,
        'nombre:', usuario.nombre
      );
    });

    range(0, Math.min(5, datos.length)).subscribe(indice => {
      console.log('RANGE:', indice, datos[indice].nombre);
    });

    const aviso$ = new Observable<string>(observador => {
      observador.next(`Se recibieron ${datos.length} usuarios`);
      observador.next('Se procesaron todos los usuarios de manera correcta');
      observador.complete();
    });

    aviso$.subscribe(mensaje => {
      console.log('MANUAL:', mensaje);
    });
  }

  // PUNTO 3 - SWITCHMAP

  private readonly seleccionId$ = new Subject<number>();

  usuarioActual = signal<Usuario | null>(null);

  seleccionarUsuario(id: number) {
    this.usuarioActual.set(null);
    this.seleccionId$.next(id);
  }

  // PUNTO 4 - CONCATMAP Y MERGEMAP

  resultadosRevision = signal<Usuario[]>([]);

  private revisionActual?: Subscription;

  private consultarConRegistro(id: number) {
    return defer(() => {
      console.log('INICIA CONSULTA:', id);

      return this.usuarioService.consultarPorId(id).pipe(
        tap(() => console.log('TERMINA CONSULTA:', id)),
        catchError(error => {
          console.error('ERROR:', id, error);
          return of(null);
        })
      );
    });
  }

  // Consulta secuencial con concatMap

  revisarEnOrden() {
    const ids = this.usuarios()
      .slice(0, 3)
      .map(u => u.id!);

    this.revisionActual?.unsubscribe();
    this.resultadosRevision.set([]);

    this.revisionActual = from(ids).pipe(
      concatMap(id => this.consultarConRegistro(id))
    ).subscribe(usuario => {
      if (usuario !== null) {
        this.resultadosRevision.update(
          actual => [...actual, usuario]
        );
      }
    });
  }

  // Consulta concurrente con mergeMap

  revisarSimultaneamente() {
    const ids = this.usuarios()
      .slice(0, 3)
      .map(u => u.id!);

    this.revisionActual?.unsubscribe();
    this.resultadosRevision.set([]);

    this.revisionActual = from(ids).pipe(
      mergeMap(id => this.consultarConRegistro(id), 2)
    ).subscribe(usuario => {
      if (usuario !== null) {
        this.resultadosRevision.update(
          actual => [...actual, usuario]
        );
      }
    });
  }

  // PUNTO 5 - SUBJECT, EXHAUSTMAP Y ASYNC PIPE

  private readonly actualizar$ = new Subject<void>();

  listado$ = this.actualizar$.pipe(
    startWith(undefined),

    exhaustMap(() =>
      this.usuarioService.obtenerUsuarios().pipe(
        tap(lista => {
          this.usuarios.set(lista);
          this.mensajeError.set('');
        }),

        catchError(error => {
          console.error(error);
          this.mensajeError.set(
            'No fue posible actualizar los usuarios'
          );
          return of(this.usuarios());
        })
      )
    ),

    shareReplay({ bufferSize: 1, refCount: true })
  );

  solicitarActualizacion() {
    this.actualizar$.next();
  }

  // PUNTO 7 - BEHAVIORSUBJECT

  usuarioSeleccionado$ =
    this.usuarioService.usuarioSeleccionado$;



// PUNTO 8 - COMBINELATEST

  filtroRxJS = new FormControl('', { nonNullable: true });

  usuariosFiltrados$ = combineLatest([
    this.listado$,
    this.filtroRxJS.valueChanges.pipe(
      startWith(''),
      debounceTime(300),
      distinctUntilChanged()
    )
  ]).pipe(
    map(([usuarios, texto]) => {
      const buscado = texto.toLowerCase().trim();

      return usuarios.filter(usuario =>
        usuario.nombre.toLowerCase().includes(buscado)
      );
    })
  );




  // PUNTO 9 - FORKJOIN

  comparacion = signal<{
    primero: Usuario | null;
    segundo: Usuario | null;
  } | null>(null);

  compararUsuarios(): void {
    this.comparacion.set(null);
    this.mensajeError.set('');

    const ids = this.usuarios()
      .map(usuario => usuario.id)
      .filter((id): id is number => id !== undefined)
      .slice(0, 2);

    if (ids.length < 2) {
      this.mensajeError.set('Se necesitan al menos dos usuarios para comparar');
      return;
    }

    this.suscripciones.add(
      forkJoin({
        primero: this.usuarioService.consultarPorId(ids[0]),
        segundo: this.usuarioService.consultarPorId(ids[1])
      }).pipe(
        catchError(error => {
          console.error(error);
          this.mensajeError.set('No fue posible consultar los usuarios para comparar');
          return of(null);
        })
      ).subscribe(datos => this.comparacion.set(datos))
    );
  }

  // INICIALIZACIÓN DEL COMPONENTE


  ngOnInit() {

    // PUNTOS 3 Y 7 - SELECCIONAR Y COMPARTIR USUARIO

    this.suscripciones.add(
      this.seleccionId$
        .pipe(
          switchMap(id =>
            this.usuarioService.consultarPorId(id).pipe(
              catchError(error => {
                console.error(error);
                return of(null);
              })
            )
          )
        )
        .subscribe(usuario => {
          this.usuarioActual.set(usuario);

          // PUNTO 7
          this.usuarioService.seleccionarUsuario(usuario);
        })
    );

    // PUNTO 6 - TIMER
    // Actualizar después de 5 segundos

    this.suscripciones.add(
      timer(5000).subscribe(() => {
        this.solicitarActualizacion();
      })
    );

    // PUNTO 6 - INTERVAL
    // Actualizar cada 15 segundos

    this.suscripciones.add(
      interval(15000).subscribe(() => {
        this.solicitarActualizacion();
      })
    );
  }

  // PUNTO 6 - CANCELAR SUSCRIPCIONES

  ngOnDestroy() {
    this.suscripciones.unsubscribe();
    this.revisionActual?.unsubscribe();
  }
}
