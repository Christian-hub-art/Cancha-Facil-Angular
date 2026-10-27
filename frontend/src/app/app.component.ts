import { Component, signal } from '@angular/core';
import { Espacios } from './components/espacios/espacios.component';

@Component({
  imports: [Espacios],
  selector: 'app-root',
  styleUrl: './app.component.css',
  templateUrl: './app.component.html',
})
export class App {
  protected readonly title = signal('frontend-angular');
}
