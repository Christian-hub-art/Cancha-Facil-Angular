import { Routes } from '@angular/router';
import { Login } from './components/login/login.component';
import { Usuarios } from './components/usuarios/usuarios.component';
import { Espacios } from './components/espacios/espacios.component';

// Routing: cada ruta muestra un componente distinto dentro de <router-outlet>.
// La barra de navegación (app.component.html) usa routerLink para ir a cada una.
export const routes: Routes = [
  // Al abrir http://localhost:4200 se envía a la lista de canchas.
  { path: '', redirectTo: 'espacios', pathMatch: 'full' },

  { path: 'login', component: Login, title: 'Ingresar · CanchaFácil' },
  { path: 'usuarios', component: Usuarios, title: 'Usuarios · CanchaFácil' },
  { path: 'espacios', component: Espacios, title: 'Canchas · CanchaFácil' },

  // Cualquier ruta que no exista vuelve a las canchas.
  { path: '**', redirectTo: 'espacios' },
];
