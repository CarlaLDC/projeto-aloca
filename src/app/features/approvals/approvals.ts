import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Aprovacoes, Solicitacao } from '../../core/api.models';
import { dataBr, hm, mensagemErro, tempoDecorrido } from '../../core/formato';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './approvals.html',
  styleUrl: './approvals.css'
})
export class Approvals implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly dados = signal<Aprovacoes | null>(null);
  readonly erro = signal('');
  readonly enviando = signal<string | null>(null);
  readonly rejecting = signal<Solicitacao | null>(null);
  readonly rejectionReason = signal('');
  readonly feedback = signal('');
  readonly hm = hm;
  readonly dataBr = dataBr;
  readonly tempoDecorrido = tempoDecorrido;

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.aprovacoes().subscribe({
      next: d => { this.dados.set(d); this.erro.set(''); },
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível carregar as solicitações.'))
    });
  }

  dois(n: number | undefined): string {
    return String(n ?? 0).padStart(2, '0');
  }

  approve(item: Solicitacao): void {
    if (this.enviando()) return;
    this.enviando.set(item.codigo);
    this.api.aprovar(item.codigo).subscribe({
      next: () => {
        this.enviando.set(null);
        this.feedback.set(`Solicitação #${item.codigo} aprovada. ${item.solicitante} foi notificado(a).`);
        this.carregar();
      },
      error: e => { this.enviando.set(null); this.feedback.set(mensagemErro(e)); this.carregar(); }
    });
  }

  openRejection(item: Solicitacao): void {
    this.rejectionReason.set('');
    this.rejecting.set(item);
  }

  closeRejection(): void {
    this.rejecting.set(null);
  }

  reject(): void {
    const item = this.rejecting();
    const motivo = this.rejectionReason().trim();
    if (!item || !motivo || this.enviando()) return;
    this.enviando.set(item.codigo);
    this.api.rejeitar(item.codigo, motivo).subscribe({
      next: () => {
        this.enviando.set(null);
        this.rejecting.set(null);
        this.feedback.set(`Solicitação #${item.codigo} rejeitada. Motivo: ${motivo}`);
        this.carregar();
      },
      error: e => { this.enviando.set(null); this.feedback.set(mensagemErro(e)); }
    });
  }
}
