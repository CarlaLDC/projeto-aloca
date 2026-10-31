import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';

type ReservationStatus = 'Aprovada' | 'Solicitada' | 'Concluída' | 'Cancelada';

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-reservations.html',
  styleUrl: './my-reservations.css'
})
export class MyReservations {
  private readonly router = inject(Router);

  readonly reservations: {
    id: string;
    resource: string;
    schedule: string;
    status: ReservationStatus;
    statusClass: string;
  }[] = [
    { id: 'RES-0248', resource: 'Laboratório 02', schedule: '22/10 · 09h–11h', status: 'Aprovada', statusClass: 'approved' },
    { id: 'RES-0249', resource: 'Projetor 04', schedule: '22/10 · 14h–16h', status: 'Solicitada', statusClass: 'requested' },
    { id: 'RES-0241', resource: 'Sala 204', schedule: '21/10 · 19h–21h', status: 'Concluída', statusClass: 'completed' },
    { id: 'RES-0238', resource: 'Auditório', schedule: '20/10 · 10h–12h', status: 'Cancelada', statusClass: 'cancelled' }
  ];

  newReservation(): void {
    void this.router.navigate(['/nova-reserva']);
  }
}
