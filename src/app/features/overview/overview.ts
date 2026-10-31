import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './overview.html',
  styleUrl: './overview.css'
})
export class Overview {
  readonly reservationDialogOpen = signal(false);
  readonly reservationSent = signal(false);
  readonly selectedSection = signal('Visão geral');

  readonly navigation = [
    { label: 'Visão geral', icon: '▦' },
    { label: 'Disponibilidade', icon: '◷' },
    { label: 'Minhas reservas', icon: '▤' },
    { label: 'Aprovações', icon: '✓' },
    { label: 'Materiais', icon: '▧' },
    { label: 'Administração', icon: '⚙' },
    { label: 'Relatórios', icon: '▥' }
  ];

  readonly agenda = [
    { time: '09:00 – 11:00', place: 'Lab. de informática 02', subject: 'Programação web', status: 'Aprovada', kind: 'approved' },
    { time: '14:00 – 16:00', place: 'Sala 204 + Projetor', subject: 'Projeto integrador', status: 'Solicitada', kind: 'pending' },
    { time: '19:00 – 21:00', place: 'Auditório', subject: 'Apresentação final', status: 'Aprovada', kind: 'approved' }
  ];

  readonly today = new Intl.DateTimeFormat('pt-BR', {
    weekday: 'long', day: 'numeric', month: 'long', year: 'numeric'
  }).format(new Date());

  openReservation(): void {
    this.reservationSent.set(false);
    this.reservationDialogOpen.set(true);
  }

  closeReservation(): void {
    this.reservationDialogOpen.set(false);
  }

  submitReservation(event: Event): void {
    event.preventDefault();
    this.reservationSent.set(true);
  }

  selectSection(label: string): void {
    this.selectedSection.set(label);
  }
}
