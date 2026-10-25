// Estructura de un espacio deportivo (cancha). Tiene los mismos campos que el
// EspacioDTO que devuelve el backend en /api/espacios.
export interface Espacio {
  id?: number; // Opcional: al crear un espacio todavía no tiene id.
  nombre: string; // Ej: "Cancha Bosque 1".
  tipoDeporte: string; // Ej: "futbol 5", "baloncesto", "tenis".
  precioHora: number; // Precio por hora en pesos colombianos.
  capacidad: number; // Número máximo de personas.
  descripcion: string; // Texto libre (opcional en el formulario).
  negocioId: number; // Negocio (sede) al que pertenece el espacio.
  negocioNombre?: string; // Lo puede enviar el backend solo para mostrarlo.
}

// Forma de la respuesta paginada de Spring Boot (PagedModel):
// GET /api/espacios?page=0&size=5 devuelve una página con su contenido
// y la información para saber cuántas páginas hay en total.
export interface Pagina<T> {
  content: T[]; // Registros de la página actual.
  page: {
    size: number; // Cuántos registros trae cada página.
    number: number; // Número de la página actual (empieza en 0).
    totalElements: number; // Total de registros en la base de datos.
    totalPages: number; // Total de páginas.
  };
}
