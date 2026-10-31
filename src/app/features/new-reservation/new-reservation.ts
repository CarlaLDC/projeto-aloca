import { Component, signal } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { RouterLink } from '@angular/router';

@Component({
  selector: 'app-new-reservation',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './new-reservation.html',
  styleUrl: './new-reservation.css'
})
export class NewReservation {
  readonly reviewing = signal(false);
  readonly completed = signal(false);
  readonly error = signal('');
  readonly rooms = ['Laboratório de informática 02 · 32 lugares', 'Sala 204 · 40 lugares', 'Auditório · 100 lugares'];
  readonly teachers = ['Mariana Costa · Desenvolvimento de sistemas', 'Lucas Oliveira · Matemática'];
  readonly materials = ['Projetor 04 · 1 unidade', 'Notebook 08 · 1 unidade', 'Sem materiais'];
  readonly today = this.localDate(new Date());
  activity = 'Aula de Programação Web';
  date = this.today;
  start = '09:00';
  end = '11:00';
  room = this.rooms[0];
  teacher = this.teachers[0];
  material = this.materials[0];

  get restricted(): boolean { return this.room === this.rooms[0]; }

  review(form: NgForm): void {
    this.error.set('');
    if (form.invalid || !this.activity.trim()) {
      form.control.markAllAsTouched();
      this.error.set('Preencha todos os campos obrigatórios.');
      return;
    }
    if (this.date < this.today) {
      this.error.set('Escolha a data de hoje ou uma data futura.');
      return;
    }
    if (this.end <= this.start) {
      this.error.set('O término deve ser posterior ao início.');
      return;
    }
    this.reviewing.set(true);
  }

  confirm(): void {
    // Demonstração local: não representa aprovação ou gravação no servidor.
    this.completed.set(true);
  }

  private localDate(date: Date): string {
    return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`;
  }
}
