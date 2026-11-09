import { Component, inject } from '@angular/core';
import { Router } from '@angular/router';
import { RouterLink } from '@angular/router';

import { DEMO_RESERVATIONS } from '../reservation-detail/reservation-data';

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-reservations.html',
  styleUrl: './my-reservations.css'
})
export class MyReservations {
  private readonly router = inject(Router);

  readonly reservations = DEMO_RESERVATIONS;

  newReservation(): void {
    void this.router.navigate(['/nova-reserva']);
  }
}
