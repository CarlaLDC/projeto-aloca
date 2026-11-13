import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AdminProfessor, Disponibilidade, RecursoDisponibilidade } from '../../core/api.models';
import { dataExtensa, hm, hojeIso, mensagemErro, somarDias } from '../../core/formato';

type ResourceType = 'Todos' | 'Salas' | 'Laboratórios' | 'Equipamentos';

const TIPO_API: Record<Exclude<ResourceType, 'Todos'>, RecursoDisponibilidade['tipo']> = {
  Salas: 'SALA', 'Laboratórios': 'LABORATORIO', Equipamentos: 'EQUIPAMENTO'
};

@Component({
  selector: 'app-availability',
  standalone: true,
  imports: [FormsModule, RouterModule],
  templateUrl: './availability.html',
  styleUrl: './availability.css'
})
export class Availability implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly searchTerm = signal('');
  readonly competencia = signal('');
  readonly capacidadeMinima = signal<number | null>(null);
  readonly professoresEncontrados = signal<AdminProfessor[] | null>(null);
  readonly erroProfessores = signal('');
  readonly selectedType = signal<ResourceType>('Todos');
  readonly selectedDate = signal(hojeIso());
  readonly selectedView = signal<'lista' | 'horarios'>('lista');
  readonly types: ResourceType[] = ['Todos', 'Salas', 'Laboratórios', 'Equipamentos'];
  readonly hm = hm;

  readonly resultado = signal<Disponibilidade | null>(null);
  readonly carregando = signal(false);
  readonly erro = signal('');
  readonly podeReservar = this.auth.possui('SOLICITANTE', 'RESPONSAVEL', 'ADMINISTRADOR', 'PROFESSOR');

  // A busca e o tipo filtram localmente a lista do dia (resposta imediata); a data consulta a API.
  readonly filteredResources = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('pt-BR');
    const type = this.selectedType();
    return (this.resultado()?.recursos ?? []).filter(resource => {
      const matchesType = type === 'Todos' || resource.tipo === TIPO_API[type];
      const matchesTerm = !term || `${resource.nome} ${resource.localizacao}`.toLocaleLowerCase('pt-BR').includes(term);
      return matchesType && matchesTerm && resource.capacidade >= (this.capacidadeMinima() ?? 0);
    });
  });

  readonly availableCount = computed(() => this.resultado()?.resumo.disponiveis ?? 0);
  readonly occupiedCount = computed(() => this.resultado()?.resumo.ocupados ?? 0);
  readonly totalCount = computed(() => this.resultado()?.resumo.total ?? 0);

  ngOnInit(): void {
    this.carregar();
  }

  carregar(): void {
    this.carregando.set(true);
    this.erro.set('');
    this.api.disponibilidade(this.selectedDate(), undefined, undefined, this.capacidadeMinima() ?? undefined).subscribe({
      next: r => { this.resultado.set(r); this.carregando.set(false); },
      error: e => { this.erro.set(mensagemErro(e, 'Não foi possível consultar a disponibilidade.')); this.carregando.set(false); }
    });
  }

  buscarCompetencia(): void {
    this.erroProfessores.set('');
    this.api.pesquisarProfessores(this.competencia()).subscribe({
      next: docentes => this.professoresEncontrados.set(docentes),
      error: e => this.erroProfessores.set(mensagemErro(e, 'Não foi possível buscar professores.'))
    });
  }

  setCapacidade(valor: number | null): void {
    if (valor !== null && (!Number.isInteger(valor) || valor < 0)) return;
    this.capacidadeMinima.set(valor); this.carregar();
  }

  setType(type: ResourceType): void {
    this.selectedType.set(type);
  }

  setDate(date: string): void {
    if (!date) return;
    this.selectedDate.set(date);
    this.carregar();
  }

  changeDate(days: number): void {
    this.setDate(somarDias(this.selectedDate(), days));
  }

  get formattedDate(): string {
    return dataExtensa(this.selectedDate());
  }

  toDateInputValueForToday(): string {
    return hojeIso();
  }

  symbol(resource: RecursoDisponibilidade): string {
    return resource.tipo === 'SALA' ? '▤' : resource.tipo === 'LABORATORIO' ? '⌘' : '▣';
  }

  /** Parâmetros que pré-preenchem a tela de nova reserva com o recurso e o próximo horário livre. */
  reservaParams(resource: RecursoDisponibilidade): Record<string, string> {
    const params: Record<string, string> = { data: this.selectedDate() };
    if (resource.tipo === 'EQUIPAMENTO') params['material'] = String(resource.id);
    else params['espaco'] = String(resource.id);
    if (resource.livreDe) params['inicio'] = hm(resource.livreDe);
    return params;
  }
}
