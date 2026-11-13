import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Movimentacao, Movimentacoes, StatusMovimentacao } from '../../core/api.models';
import { diaRelativo, mensagemErro } from '../../core/formato';

type FilterStatus = 'Todos' | StatusMovimentacao;

@Component({
  selector: 'app-withdrawals-returns',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './withdrawals-returns.html',
  styleUrl: './withdrawals-returns.css'
})
export class WithdrawalsReturns implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly searchTerm = signal('');
  readonly selectedStatus = signal<FilterStatus>('Todos');
  readonly feedback = signal('');
  readonly erro = signal('');
  readonly enviando = signal<string | null>(null);
  readonly dados = signal<Movimentacoes | null>(null);
  readonly diaRelativo = diaRelativo;

  readonly filters: { valor: FilterStatus; rotulo: string }[] = [
    { valor: 'Todos', rotulo: 'Todos' },
    { valor: 'AGUARDANDO_RETIRADA', rotulo: 'Aguardando retirada' },
    { valor: 'EM_USO', rotulo: 'Em uso' },
    { valor: 'DEVOLUCAO_PENDENTE', rotulo: 'Devolução pendente' },
    { valor: 'DEVOLVIDO', rotulo: 'Devolvido' }
  ];

  readonly movements = computed(() => this.dados()?.movimentacoes ?? []);

  readonly filteredMovements = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('pt-BR');
    return this.movements().filter(m => {
      const matchesStatus = this.selectedStatus() === 'Todos' || m.status === this.selectedStatus();
      const haystack = `${m.codigo} ${m.reserva} ${m.item} ${m.responsavel} ${m.destino}`.toLocaleLowerCase('pt-BR');
      return matchesStatus && (!term || haystack.includes(term));
    });
  });

  readonly waitingCount = computed(() => this.dados()?.aguardandoRetirada ?? 0);
  readonly inUseCount = computed(() => this.dados()?.emUso ?? 0);
  readonly returnCount = computed(() => this.dados()?.devolucoesPendentes ?? 0);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.api.movimentacoes().subscribe({
      next: d => { this.dados.set(d); this.erro.set(''); },
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível carregar as movimentações.'))
    });
  }

  dois(n: number): string {
    return String(n).padStart(2, '0');
  }

  setFilter(status: FilterStatus): void {
    this.selectedStatus.set(status);
  }

  registerPickup(m: Movimentacao): void {
    this.executar(m, this.api.registrarRetirada(m.codigo), 'Retirada registrada');
  }

  registerReturn(m: Movimentacao): void {
    this.executar(m, this.api.registrarDevolucao(m.codigo), 'Devolução registrada');
  }

  private executar(m: Movimentacao, chamada: ReturnType<ApiService['registrarRetirada']>, acao: string): void {
    if (this.enviando()) return;
    this.enviando.set(m.codigo);
    chamada.subscribe({
      next: atualizada => {
        this.enviando.set(null);
        this.feedback.set(`${acao} para ${m.item}${atualizada.protocolo ? ' · protocolo ' + atualizada.protocolo : ''}.`);
        this.carregar();
      },
      error: e => { this.enviando.set(null); this.feedback.set(mensagemErro(e)); this.carregar(); }
    });
  }
}
