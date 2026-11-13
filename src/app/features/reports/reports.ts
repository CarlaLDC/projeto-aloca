import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink, RouterLinkActive } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { Relatorio } from '../../core/api.models';
import { dataBr, hojeIso, mensagemErro, somarDias } from '../../core/formato';

@Component({
  selector: 'app-reports',
  standalone: true,
  imports: [FormsModule, RouterLink, RouterLinkActive],
  templateUrl: './reports.html',
  styleUrl: './reports.css'
})
export class Reports implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly fim = signal(hojeIso());
  readonly inicio = signal(somarDias(hojeIso(), -29));
  readonly relatorio = signal<Relatorio | null>(null);
  readonly erro = signal('');
  readonly dataBr = dataBr;

  readonly resources = computed(() => (this.relatorio()?.recursos ?? []).map(r => ({
    name: r.nome, usage: r.utilizacao, hours: r.horas, reservations: r.reservas, conflicts: r.conflitos
  })));

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.erro.set('');
    this.api.relatorio(this.inicio(), this.fim()).subscribe({
      next: r => this.relatorio.set(r),
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível gerar o relatório.'))
    });
  }

  horas(valor: number | undefined): string {
    return (valor ?? 0).toLocaleString('pt-BR', { maximumFractionDigits: 1 });
  }

  exportReport(): void {
    this.api.relatorioCsv(this.inicio(), this.fim()).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const anchor = document.createElement('a');
        anchor.href = url;
        anchor.download = `relatorio-aloca-${this.inicio()}-a-${this.fim()}.csv`;
        anchor.click();
        URL.revokeObjectURL(url);
      },
      error: e => this.erro.set(mensagemErro(e, 'Não foi possível exportar o relatório.'))
    });
  }
}
