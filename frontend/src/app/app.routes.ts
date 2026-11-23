import { Routes } from '@angular/router';
import { Login } from './components/login/login.component';
import { Usuarios } from './components/usuarios/usuarios.component';
import { Espacios } from './components/espacios/espacios.component';
import { AccesoDenegado } from './components/acceso-denegado/acceso-denegado.component';
import { PagoResultadoComponent } from './pagos/pago-resultado.component';
import { autenticadoGuard, invitadoGuard, rolGuard } from './guards/auth.guard';

// Routing: cada ruta muestra un componente distinto dentro de <router-outlet>.
// La barra de navegación (app.component.html) usa routerLink para ir a cada una.
//
// Niveles de acceso (iguales a la matriz del backend, ver backend/SEGURIDAD.md):
//  - Público: /espacios (catálogo) y /login (solo visitantes).
//  - Con sesión (USER o ADMIN): /pago/resultado.
//  - Solo ADMIN: /usuarios.
export const routes: Routes = [
  // Al abrir http://localhost:4200 se envía a la lista de canchas.
  { path: '', redirectTo: 'espacios', pathMatch: 'full' },

  { path: 'login', component: Login, canActivate: [invitadoGuard], title: 'Ingresar · CanchaFácil' },
  { path: 'usuarios', component: Usuarios, canActivate: [rolGuard('ADMIN')], title: 'Usuarios · CanchaFácil' },
  { path: 'espacios', component: Espacios, title: 'Canchas · CanchaFácil' },

  // Wompi devuelve aquí al usuario después de pagar. Consultar la orden exige sesión.
  {
    path: 'pago/resultado',
    component: PagoResultadoComponent,
    canActivate: [autenticadoGuard],
    title: 'Resultado del pago · CanchaFácil',
  },

  // Destino de rolGuard cuando hay sesión pero falta el rol.
  { path: 'acceso-denegado', component: AccesoDenegado, title: 'Acceso denegado · CanchaFácil' },

  // Cualquier ruta que no exista vuelve a las canchas.
  { path: '**', redirectTo: 'espacios' },
];
