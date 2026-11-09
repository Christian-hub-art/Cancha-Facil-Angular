import { Component, effect, input, output } from '@angular/core';
// ReactiveFormsModule habilita [formGroup] y formControlName en la plantilla.
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Espacio } from '../../models/espacio.model';

// Nombres de los campos del formulario (se usa en invalido()).
type CampoEspacio = 'nombre' | 'tipoDeporte' | 'precioHora' | 'capacidad' | 'descripcion' | 'negocioId';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule],
  selector: 'app-espacio-form',
  styleUrl: './espacio-form.component.css',
  templateUrl: './espacio-form.component.html',
})
export class EspacioForm {
  // PADRE -> HIJO: el espacio que se va a editar (null = crear uno nuevo).
  espacio = input<Espacio | null>(null);

  // HIJO -> PADRE: envía el espacio con los datos del formulario.
  guardado = output<Espacio>();

  // HIJO -> PADRE: avisa que la persona canceló.
  cancelado = output<void>();

  // Tipos de cancha (todas son de fútbol). Mismos textos que guarda el backend en tipoDeporte.
  readonly tiposCancha = ['microfutbol', 'futbol 5', 'futbol 8', 'futbol 11'];

  // Formulario reactivo: cada campo es un FormControl con sus validaciones.
  formulario = new FormGroup({
    nombre: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required, Validators.minLength(3), Validators.maxLength(150)],
    }),
    tipoDeporte: new FormControl('', {
      nonNullable: true,
      validators: [Validators.required],
    }),
    precioHora: new FormControl<number | null>(null, [Validators.required, Validators.min(1000)]),
    capacidad: new FormControl<number | null>(null, [
      Validators.required,
      Validators.min(1),
      Validators.max(100),
    ]),
    descripcion: new FormControl('', {
      nonNullable: true,
      validators: [Validators.maxLength(500)],
    }),
    negocioId: new FormControl<number | null>(1, [Validators.required, Validators.min(1)]),
  });

  constructor() {
    // effect() se ejecuta cada vez que cambia el input espacio():
    // si llega un espacio, llena el formulario para editarlo; si no, lo deja vacío.
    effect(() => {
      const espacio = this.espacio();
      if (espacio) {
        this.formulario.reset({
          nombre: espacio.nombre,
          tipoDeporte: espacio.tipoDeporte,
          precioHora: espacio.precioHora,
          capacidad: espacio.capacidad,
          descripcion: espacio.descripcion ?? '',
          negocioId: espacio.negocioId,
        });
      } else {
        this.formulario.reset({
          nombre: '',
          tipoDeporte: '',
          precioHora: null,
          capacidad: null,
          descripcion: '',
          negocioId: 1,
        });
      }
    });
  }

  // true si el campo tiene un error y la persona ya pasó por él.
  // Así los mensajes no aparecen antes de que empiece a escribir.
  invalido(campo: CampoEspacio): boolean {
    const control = this.formulario.controls[campo];
    return control.invalid && (control.touched || control.dirty);
  }

  enviar(): void {
    if (this.formulario.invalid) {
      // Marca todos los campos para que se vean todos los mensajes de error.
      this.formulario.markAllAsTouched();
      return;
    }

    const valores = this.formulario.getRawValue();

    // emit(...) envía el espacio al componente padre, que lo recibe con $event.
    this.guardado.emit({
      id: this.espacio()?.id, // Si viene id, el padre sabe que es una edición (PUT).
      nombre: valores.nombre.trim(),
      tipoDeporte: valores.tipoDeporte,
      precioHora: Number(valores.precioHora),
      capacidad: Number(valores.capacidad),
      descripcion: valores.descripcion.trim(),
      negocioId: Number(valores.negocioId),
    });
  }

  cancelar(): void {
    this.cancelado.emit();
  }
}
