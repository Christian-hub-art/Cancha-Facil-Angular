
import { CommonModule } from '@angular/common';
import { Component, inject, signal, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { Usuario } from '../../models/usuario.model';
import { UsuarioService } from '../../services/usuario.service';
import { UsuarioForm } from '../usuario-form/usuario-form.component';
import { finalize, switchMap } from 'rxjs';
import { HttpErrorResponse } from '@angular/common/http';


@Component({
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    UsuarioForm
  ],
  selector: 'app-usuarios',
  styleUrl: './usuarios.component.css',
  templateUrl: './usuarios.component.html',
})
export class Usuarios implements OnInit{

  private readonly usuarioService = inject(UsuarioService);
  //guardar usuarios para mostrar en el front
  usuarios = signal<Usuario[]>([]);
  //crear signal para mostrar cargando
  cargando = signal(false)

  mensajeError = signal('');

  filtro = '';
  mostrarFormulario = false;

  //para poder motsrar la info del usuario seleccionado
  detalleUsuario = false;

  //guardar el usuario seleccionado
  usuarioSeleccionado: Usuario | null = null
  // Para ver información del detalle del usuario
  usuarioDetalle: Usuario | null = null;      
  //mostrar info del usuario
  mostrarDetalle(usuario: Usuario): void {
    if(this.usuarioDetalle?.id === usuario.id){
      this.detalleUsuario = !this.detalleUsuario
    } else {
      this.usuarioDetalle = usuario
      this.detalleUsuario = true
    }
  }

  

  //metodo para guardar el usuario y mostrar el formulario
  editarUsuario(usuario: Usuario): void {
    this.usuarioSeleccionado = usuario;
    this.mostrarFormulario = true;
  } 
  
  //para ejecutar cuando seleccione el usuario, nuevo usuario
  nuevoUsuario(): void{
    this.usuarioSeleccionado = null;
    this.mostrarFormulario = !this.mostrarFormulario;
  }

  //para que el formulario sepa si tiene que cargar datos o no
  procesarEvento(usuario: Usuario): void{
    if(this.usuarioSeleccionado?.id != null){
      this.actualizarUsuario(this.usuarioSeleccionado.id, usuario)
    } else {
      this.guardarUsuario(usuario)
    }
  }

  ngOnInit(): void {
    this.cargarUsuarios();
  }

  
cargarUsuarios(): void {
  this.cargando.set(true);
  this.usuarioService.obtenerUsuarios().pipe(
    finalize(() => this.cargando.set(false))
  )
  .subscribe({
    next: (usuarios) => {
      this.usuarios.set(usuarios);
      this.mensajeError.set('');
    },
    error: (error: HttpErrorResponse) => {
      this.manejarError(error)
    },
    complete: () =>{
        console.log('Usuarios cargados correctamente');
      }
  });
}


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
    this.cargando.set(true)
    this.usuarioService.crearUsuario(usuario).pipe(
      switchMap(() => this.usuarioService.obtenerUsuarios()),
      finalize(() => this.cargando.set(false))
    ).subscribe({
      next : (usuarios) => {
        this.usuarios.set(usuarios);
        this.mostrarFormulario = false;
        this.mensajeError.set(''); 
      },
      error: (error: HttpErrorResponse) => {
        console.log('Error al guadar usuario: ', error);
        this.manejarError(error)
      },
      complete: () =>{
        console.log('Guardar usuario termino correctamente');
      }
    })
  }

  borrarUsuario(idUsuario: number): void {
    this.cargando.set(true)
    this.usuarioService.borrarUsuario(idUsuario).pipe(
      switchMap(() => this.usuarioService.obtenerUsuarios()),
      finalize(() => this.cargando.set(false))
    ).subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.mensajeError.set('');
      },
      error: (error: HttpErrorResponse) => {
        console.log('No se puedo eliminar el usuario: ', error);
        this.manejarError(error)
      },
      complete: () => {
        console.log('Se borro el usuario de manera correcta');
      }
    })
  }

  actualizarUsuario (idUsuario: number, usuario: Usuario): void {
    this.cargando.set(true)
    this.usuarioService.actualizarUsuario(idUsuario, usuario).pipe(
      switchMap(()=> this.usuarioService.obtenerUsuarios()),
      finalize(() => this.cargando.set(false))
    ).subscribe({
      next: (usuarios) => {
        this.usuarios.set(usuarios);
        this.mostrarFormulario = false;
        this.mensajeError.set('');
      },
      error: (error: HttpErrorResponse) => {
        console.log('Ocurrio un error al actualizar el usuario: ', error);
        this.manejarError(error);
      },
      complete: () => {
        console.log('Se actualizo de manera correcta el usuario')
      }
    })
  }


  manejarError(error: HttpErrorResponse): void{
    
    if(error.status === 0){
      this.mensajeError.set('No se pudo conectar con el servidor')
    } else if (error.status === 400) {
      this.mensajeError.set('Se ingresaron datos invalidos ')
    } else if (error.status === 409){
      this.mensajeError.set('Ya existe un usuario con ese correo')
    } else if (error.status === 404){
      this.mensajeError.set('No se encontro el usuario')
    } else {
      this.mensajeError.set('Error inseperado en el servidor')
    }

  }

}
