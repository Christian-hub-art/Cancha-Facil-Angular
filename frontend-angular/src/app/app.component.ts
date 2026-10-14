import { Component, signal } from '@angular/core';
import { Usuarios } from './components/usuarios/usuarios.component';
import { Login } from './components/login/login.component';

@Component({
  imports: [Usuarios, Login],
  selector: 'app-root',
  styleUrl: './app.component.css',
  templateUrl: './app.component.html',
})
export class App {
  protected readonly title = signal('frontend-angular');
}
