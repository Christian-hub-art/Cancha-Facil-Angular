import { CommonModule, DatePipe } from '@angular/common'; // Importa utilidades comunes y el pipe para fechas.
import { Component, inject, signal } from '@angular/core'; // Permite definir el componente Angular y obtener servicios con inject().
import { FormsModule } from '@angular/forms'; // Habilita el enlace de datos de los formularios.
import { 
  catchError, of, Subscription, range, 
  Observable, from, Subject, switchMap, 
  defer, tap, concatMap, mergeMap} from 'rxjs';
import { Usuario } from '../../models/usuario.model'; // Estructura de cada usuario (antes estaba definida aquí).
import { UsuarioService } from '../../services/usuario.service'; // Servicio con la lógica de usuarios.
import { UsuarioForm } from '../usuario-form/usuario-form.component'; // Componente hijo: formulario de usuario.

@Component({
  // decorador: patron de diseño de software que permite agregar funcionalidades a una clase
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, UsuarioForm],
  selector: 'app-usuarios',
  styleUrl: './usuarios.component.css',
  templateUrl: './usuarios.component.html',
})
export class Usuarios {
  filtro = '';

  // Angular entrega la instancia de UsuarioService (no se usa new UsuarioService()).
  private readonly usuarioService = inject(UsuarioService);

  // Los datos de ejemplo ahora viven en UsuarioService.
  usuarios = signal<Usuario[]>([]);
  mensajeError = signal('');
  private readonly suscripciones = new Subscription();

  cargarUsuarios() {
    this.mensajeError.set('');
    this.suscripciones.add(
      this.usuarioService
        .obtenerUsuarios()
        .pipe(
          catchError((error) => {
            this.mensajeError.set('error al cargar los usuarios');
            return of([] as Usuario[]);
          }),
        )
        .subscribe((lista) => this.usuarios.set(lista)),
    );
  }

  // Controla si se muestra el formulario de nuevo usuario.
  mostrarFormulario = false;

  get usuariosFiltrados(): Usuario[] {
    const texto = this.filtro.trim().toLowerCase();
    const usuarios = this.usuarios();

    if (!texto) {
      return usuarios;
    }

    return usuarios.filter(
      (usuario) =>
        usuario.nombre.toLowerCase().includes(texto) ||
        usuario.email.toLowerCase().includes(texto) ||
        usuario.rol.toLowerCase().includes(texto),
    );
  }

  limpiarFiltro(): void {
    this.filtro = '';
  }

  // $event trae el usuario que el formulario hijo envió con emit().
  guardarUsuario(usuario: Usuario): void {
    this.usuarioService.agregarUsuario(usuario);
    this.mostrarFormulario = false;
  }

  

//segundo punto del taller
  examinarUsuario(){
    const datos = this.usuarios();
    of(datos).subscribe(lista => {
      console.log('OF: total', lista.length)
    });

    from(datos).subscribe(usuario => {
      console.log('FROM: id: ',usuario.id, 'nombre: ',usuario.nombre);

    });
    range(0, Math.min(5, datos.length)).subscribe(indice => {
        console.log('RANGE:', indice, datos[indice].nombre);
      })

    const aviso$ = new Observable<string>(observador =>{
      observador.next(`Se recibieron ${datos.length} usuarios`);
      observador.next('Se procesaron todos los usuarios de manera correcta');
      observador.complete();
    });
    aviso$.subscribe(mensaje => {
      console.log('MANUAL:', mensaje);
    })
  }

  //punto 3 taller
  private readonly seleccionId$ = new Subject<number>();
  usuarioActual = signal<Usuario | null>(null);

  seleccionarUsuario(id: number){
    this.usuarioActual.set(null);
    this.seleccionId$.next(id);
  }

  //punto 4 taller 
  resultadosRevision = signal<Usuario[]>([]);
  private revisionActual?: Subscription;
  private consultarConRegistro(id: number){
    return defer(() =>{
      console.log('Inicia consulta: ', id);
      
      return this.usuarioService.consultarPorId(id).pipe(
        tap(()=> console.log('Termina consulta: ', id)),
        catchError(error => {
          console.error('Error:' , id , error);
          return of(null);
        })
      )
    })
  }
//consulta secuencial
  revisarEnOrden(){
    const ids = this.usuarios().slice(0,3).map(u => u.id!);
    this.revisionActual?.unsubscribe();
    this.resultadosRevision.set([]);
    this.revisionActual = from(ids).pipe(
      concatMap(id => this.consultarConRegistro(id))
    ).subscribe(usuario => {
      if(usuario !== null){
        this.resultadosRevision.update(actual => [...actual, usuario]);
      }
    })
  }
  //consulta recurrente
  revisarSimultaneamente() {
    const ids = this.usuarios().slice(0, 3).map(u => u.id!);

    this.revisionActual?.unsubscribe();
    this.resultadosRevision.set([]);

    this.revisionActual = from(ids).pipe(
      mergeMap(id => this.consultarConRegistro(id))
    ).subscribe(usuario => {
      if (usuario !== null) {
        this.resultadosRevision.update(actual => [...actual, usuario]);
      }
    });
  }

  ngOnInit() {
    this.cargarUsuarios();

    //punto 3 taller
    this.suscripciones.add(
      this.seleccionId$
      .pipe(
        switchMap(id => 
          this.usuarioService.consultarPorId(id).pipe(
            catchError((error)=>{
              console.error(error);
              return of(null);
            })
          )
        )
      )
      .subscribe(usuario => this.usuarioActual.set(usuario))
    )
  }
}
