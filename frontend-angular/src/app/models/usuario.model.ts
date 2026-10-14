export interface Usuario { // Define la estructura de cada usuario.
  id?: number; // Identificador único del usuario (opcional: al crear todavía no tiene id).
  nombre: string; // Nombre completo del usuario.
  email: string; // Correo electrónico del usuario.
  telefono: string; // Número telefónico del usuario.
  rol: 'ADMINISTRADOR' | 'CLIENTE'; // Limita el rol a estas dos opciones.
  fechaRegistro: string; // Guarda la fecha de registro como texto.
}
