import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ReservaResumo } from '../../core/api.models';
import { classeStatus, diaMes, hCurta, mensagemErro } from '../../core/formato';

@Component({
  selector: 'app-my-reservations',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './my-reservations.html',
  styleUrl: './my-reservations.css'
})
export class MyReservations implements OnInit {
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly reservations = signal<(ReservaResumo & { schedule: string; statusClass: string })[]>([]);
  readonly carregado = signal(false);
  readonly erro = signal('');
  readonly podeReservar = this.auth.possui('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR', 'PROFESSOR');

  ngOnInit(): void {
    this.api.minhasReservas().subscribe({
      next: lista => {
        this.reservations.set(lista.map(r => ({
          ...r,
          schedule: `${diaMes(r.data)} · ${hCurta(r.inicio)}–${hCurta(r.fim)}`,
          statusClass: classeStatus(r.status)
        })));
        this.carregado.set(true);
      },
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível carregar suas reservas.'))
    });
  }

  newReservation(): void {
    void this.router.navigate(['/nova-reserva']);
  }
}
