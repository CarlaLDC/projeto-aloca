import { Component, OnInit, inject, signal } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Painel } from '../../core/api.models';
import { classeStatus, dataExtensa, hm, mensagemErro } from '../../core/formato';

@Component({
  selector: 'app-overview',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './overview.html',
  styleUrl: './overview.css'
})
export class Overview implements OnInit {
  private readonly router = inject(Router);
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly painel = signal<Painel | null>(null);
  readonly erro = signal('');
  readonly notificacoesAbertas = signal(false);
  readonly hm = hm;
  readonly classeStatus = classeStatus;

  readonly navigation = [
    { label: 'Visão geral', icon: '▦', rota: '/visao-geral' },
    { label: 'Disponibilidade', icon: '◷', rota: '/disponibilidade' },
    { label: 'Minhas reservas', icon: '▤', rota: '/minhas-reservas' },
    { label: 'Aprovações', icon: '✓', rota: '/aprovacoes', perfis: ['RESPONSAVEL'] as const },
    { label: 'Materiais', icon: '▧', rota: '/retiradas-devolucoes', perfis: ['RESPONSAVEL'] as const },
    { label: 'Relatórios', icon: '▥', rota: '/relatorios', perfis: ['RESPONSAVEL', 'ADMINISTRADOR'] as const }
  ].filter(item => !item.perfis || this.auth.possui(...item.perfis));

  readonly podeReservar = this.auth.possui('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR', 'PROFESSOR');

  get today(): string {
    const data = this.painel()?.data;
    return data ? dataExtensa(data) : '';
  }

  ngOnInit(): void {
    this.auth.atualizarNotificacoes();
    this.api.painel().subscribe({
      next: p => this.painel.set(p),
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível carregar o painel.'))
    });
  }

  dois(n: number | undefined): string {
    return String(n ?? 0).padStart(2, '0');
  }

  openReservation(): void {
    void this.router.navigate(['/nova-reserva']);
  }

  alternarNotificacoes(): void {
    const abrir = !this.notificacoesAbertas();
    this.notificacoesAbertas.set(abrir);
    if (abrir && this.auth.naoLidas() > 0) this.auth.marcarNotificacoesLidas();
  }
}
