import { ApplicationConfig, inject, provideAppInitializer, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { credencialesInterceptor } from './interceptors/credenciales.interceptor';
import { AuthService } from './services/auth.service';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes),
    // El interceptor envía la cookie de sesión y el token CSRF al backend.
    provideHttpClient(withInterceptors([credencialesInterceptor])),
    // Al cargar o recargar la app se recupera la identidad desde /api/auth/me.
    // No se espera la respuesta para no bloquear el arranque; los guards sí la esperan.
    provideAppInitializer(() => {
      void inject(AuthService).cargarSesion();
    }),
  ],
};
