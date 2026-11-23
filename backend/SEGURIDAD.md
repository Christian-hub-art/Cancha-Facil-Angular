# Seguridad para la entrega

La configuración sigue la estructura del ejemplo de clase: una sola
`SecurityFilterChain`, un `PasswordEncoder` y tres `UserDetails` que se entregan
a `InMemoryUserDetailsManager`. No se usa `@Order` ni otra configuración paralela.

## Cómo leer SeguridadConfig

1. `authorizeHttpRequests`: define quién puede entrar. `permitAll` es público,
   `hasRole("ADMIN")` exige administrador y `authenticated` exige iniciar sesión.
   Las reglas se leen de arriba hacia abajo; se aplica la primera que coincida.
2. `formLogin`: Spring recibe `email` y `password` en POST /login y los verifica.
3. `logout`: POST /logout invalida la sesión; la invalidación es el valor por defecto.
4. `exceptionHandling`: responde 401 si falta iniciar sesión y 403 si se deniega
   el acceso. CSRF también puede producir un 403.
5. `passwordEncoder`: BCrypt codifica y comprueba contraseñas.
6. `usuariosEnMemoria`: declara Laura, Carlos y el administrador por separado.
7. `corsConfigurationSource`: permite las peticiones desde Angular con cookies.

Los dos métodos pequeños del final devuelven JSON cuando la petición es de la API
o lleva `Accept: application/json`. Así Angular recibe datos y las páginas
Thymeleaf conservan sus redirecciones al login y al perfil.

## Explicación del flujo

Angular envía correo y contraseña. Spring verifica la cuenta en memoria y crea
una sesión en el servidor. El navegador conserva una cookie con su identificador.
En cada petición Spring comprueba la sesión y los permisos. Logout cierra la sesión.
CSRF permanece habilitado por defecto, como en las guías de clase.

## Cuentas y datos

Las identidades son `laura@correo.com` y `carlos@correo.com` (USER), y
`admin@canchafacil.com` (ADMIN). Las claves de demostración están definidas en
`usuariosEnMemoria`; se codifican con BCrypt y no se imprimen al arrancar.
Es la única fuente de autenticación activa. `UsuarioDetallesService` queda como
alternativa sin anotación de servicio y no interviene en el login.

`DatosIniciales` garantiza los tres registros de negocio por correo, aunque la BD
ya tenga datos. Se conservan los nombres CLIENTE y ADMINISTRADOR del enum de BD;
las autorizaciones usan USER y ADMIN. El registro y el CRUD de usuarios escriben
la BD: no crean ni cambian las cuentas de autenticación en memoria de esta entrega.

## Contrato para la persona 2

| Petición | Resultado |
|---|---|
| GET /api/auth/csrf | 200 con `headerName` y `token`; disponible sin login |
| POST /login | Formulario URL-encoded con `email` y `password`; JSON 200 o 401 |
| GET /api/auth/me | 200 con `email` y `roles`; 401 sin sesión |
| POST /logout | 204; invalida la sesión |

Angular debe usar `withCredentials: true` en estas peticiones y en las demás
llamadas a la API. En login y logout debe enviar además la cabecera
`Accept: application/json` para recibir 200/401/204 en lugar de redirecciones.
Antes del login, consultar `/api/auth/csrf` y enviar su token
en la cabecera indicada por `headerName` (`X-CSRF-TOKEN`). No enviar el login como
JSON: usar `application/x-www-form-urlencoded`.

Volver a consultar el token después de login y logout, porque Spring lo renueva.
Enviar el token vigente en POST, PUT, PATCH y DELETE, incluido logout.
No guardar contraseñas, cookies ni tokens en localStorage.

El origen del frontend se configura con `app.frontend-url` / `FRONTEND_URL`.
Se permiten también los orígenes locales de Angular ya usados por el proyecto.
Para la integración de esta entrega usar el mismo sitio o un proxy de desarrollo;
CORS por sí solo no habilita cookies entre sitios distintos con políticas SameSite.

## Permisos

- GET /api/espacios y sus consultas: público.
- /api/usuarios y sus subrutas: ADMIN.
- Modificaciones de /api/espacios: ADMIN.
- Las demás rutas API: sesión requerida.
- Errores de autenticación: JSON 401; rol insuficiente o CSRF inválido: JSON 403.

Las páginas Thymeleaf conservan `/login`, `/logout`, sus recursos estáticos y sus
redirecciones. Las rutas de usuarios y los formularios de administración de
espacios requieren ADMIN, también si se abren directamente.

## Verificación

`SeguridadTests` comprueba las tres cuentas y BCrypt, consulta de identidad,
permisos, login incorrecto, CSRF obligatorio, CORS con credenciales, logout y
reinicialización sin duplicados sobre H2. Ejecutar junto al test de contexto:

```sh
mvn test
```

No incluir valores de cookies ni claves en capturas de evidencias.
