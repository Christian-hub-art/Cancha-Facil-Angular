# Permisos por rol: matriz, pruebas y evidencias (Persona 3)

El servidor decide quién puede hacer cada operación; Angular solo oculta las
opciones que el rol no puede usar. Si un USER envía la petición a mano, el
backend responde 403 y los datos no cambian.

## Matriz final

| Ruta y operación | Visitante | USER | ADMIN |
|---|---|---|---|
| GET /api/espacios y consultas (paginado, /{id}, /precio) | 200 | 200 | 200 |
| GET /api/auth/me | 401 | 200 | 200 |
| /api/usuarios y subrutas (GET, POST, PUT, DELETE) | 401 | 403 | Permitido |
| POST, PUT, DELETE /api/espacios/** | 401 | 403 | Permitido |
| Páginas /usuarios/** y /espacios/add, /update/**, /delete/** | Login | 403 | Permitido |
| Páginas /negocios/delete/**, /pagos/update/**, /pagos/estado/**, /pagos/delete/** | Login | 403 | Permitido |
| Páginas /notificaciones/add, /update/**, /delete/**; /calificaciones/delete/**; /reservas/delete/** | Login | 403 | Permitido |

Las reglas están en `SeguridadConfig.securityFilterChain` (`hasRole("ADMIN")`).
Las rutas Thymeleaf de la última fila se agregaron porque, antes, cualquier USER
con sesión podía abrirlas escribiendo la URL y borrar datos.

## Dónde se ve en el código

| Qué | Archivo |
|---|---|
| Reglas por ruta y método | `backend/.../Controladores/SeguridadConfig.java` |
| Pruebas de la Persona 3 | `backend/src/test/java/com/example/demo/PermisosTests.java` |
| Menú por rol, correo y botón Salir | `frontend/src/app/app.component.*` |
| Botones de canchas solo para ADMIN | `frontend/src/app/components/espacios/*` |
| Mensajes 401 y 403 | `mostrarError` en espacios y `manejarError` en usuarios |
| Sesión para la interfaz (`sesion`, `esAdmin`, `cargarSesion`, `logout`) | `frontend/src/app/services/auth.service.ts` |

## Pruebas automáticas

```sh
cd backend
mvn test -Dtest=PermisosTests
```

| Prueba | Qué demuestra |
|---|---|
| `visitanteConsultaCatalogo` | GET /api/espacios responde 200 sin sesión. |
| `visitanteNoEntraARutasProtegidas` | GET /api/usuarios y POST /api/espacios dan 401; no se devuelven correos ni se crea nada. |
| `userRecibe403YAdminPuedeEliminar` | ADMIN crea una cancha temporal (201). USER, con sesión y CSRF válido, recibe 403 al editarla, eliminarla y crear otra; la cancha queda intacta. ADMIN la elimina (204). |
| `rutasThymeleafAdministrativasRechazanUser` | Un USER recibe 403 en las páginas administrativas y no cambia ningún dato; /perfil y /espacios siguen funcionando. |

El 403 del USER se obtiene enviando un token CSRF válido. Así se distingue una
denegación por rol de un rechazo por falta de CSRF (que también sería 403).

## Prueba manual (para la sustentación)

1. Iniciar sesión en Angular como `admin@canchafacil.com`: aparecen «Usuarios»,
   «Nueva cancha», «Editar» y «Eliminar».
2. Crear una cancha de prueba y eliminarla: responde 201 y luego 204.
3. Salir e iniciar sesión como `laura@correo.com`: el menú ya no muestra
   «Usuarios» y las tarjetas no tienen botones de administración.
4. Abrir `/usuarios` escribiendo la URL: el servidor responde 403 y la pantalla
   muestra «No tienes permiso para administrar usuarios».
5. En DevTools → Network, repetir como USER un DELETE a `/api/espacios/{id}`
   con la cabecera `X-CSRF-TOKEN` vigente: responde 403 y la cancha sigue en la lista.

## Registro de evidencias

Completar al ejecutar cada caso. No pegar valores de cookies, tokens ni contraseñas.

| Fecha | Rol | Petición | Código esperado | Código obtenido | Resultado y evidencia |
|---|---|---|---|---|---|
| | Visitante | GET /api/espacios | 200 | | |
| | Visitante | GET /api/usuarios | 401 | | |
| | Visitante | POST /api/espacios | 401 | | |
| | USER | GET /api/usuarios | 403 | | |
| | USER | DELETE /api/espacios/{id temporal} (con CSRF) | 403 | | La cancha sigue existiendo |
| | USER | GET /negocios/delete/1 | 403 | | El negocio sigue existiendo |
| | ADMIN | POST /api/espacios (cancha temporal) | 201 | | |
| | ADMIN | DELETE /api/espacios/{id temporal} | 204 | | La cancha ya no aparece |
