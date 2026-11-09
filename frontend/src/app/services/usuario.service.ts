import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Usuario } from '../models/usuario.model';


// Servicio con la lógica de los usuarios. Mientras Angular no se conecte al
// backend (/api/usuarios), guarda los usuarios temporalmente en un arreglo.
@Injectable({
  providedIn: 'root',
})
export class UsuarioService {
  

  private readonly http = inject(HttpClient);
  private readonly url = 'http://localhost:8080/api/usuarios';
  
  
  //crud de usuario
  
  //get
  obtenerUsuarios(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>(this.url);
  }
  //post
  crearUsuario(usuario: Usuario): Observable<Usuario> {
    return this.http.post<Usuario>(
      this.url, usuario
    );
  }
  //put
  actualizarUsuario(idUsuario: number, usuario: Usuario): Observable<Usuario> {
    return this.http.put<Usuario>(
      `${this.url}/${idUsuario}`, usuario
    );
  }

  //delete
  borrarUsuario(idUsuario: number): Observable<void> {
    return this.http.delete<void>(
      `${this.url}/${idUsuario}`
    );
  }
}
