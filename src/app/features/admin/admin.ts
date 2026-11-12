import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AdminRecurso, AdminProfessor, AdminBloqueio } from '../../core/api.models';
import { mensagemErro } from '../../core/formato';

@Component({
  selector: 'app-admin',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './admin.html',
  styleUrl: './admin.css'
})
export class Admin implements OnInit {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);
  readonly recursos = signal<AdminRecurso[]>([]);
  readonly professores = signal<AdminProfessor[]>([]);
  readonly bloqueios = signal<AdminBloqueio[]>([]);
  readonly erro = signal('');
  readonly feedback = signal('');
  readonly enviando = signal(false);
  readonly aba = signal<'recursos' | 'professores' | 'bloqueios'>('recursos');
  recurso: Omit<AdminRecurso, 'id' | 'ativo'> = {
    nome: '', tipo: 'SALA', categoria: '', localizacao: '', capacidade: 1,
    restrito: false, abertura: '07:00', fechamento: '22:00'
  };
  professor = { nome: '', competencia: '' };
  bloqueio = { recursoId: 0, inicio: '', fim: '', motivo: '' };
  editandoRecurso: number | null = null;
  editandoProfessor: number | null = null;

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.api.adminRecursos().subscribe({ next: x => this.recursos.set(x), error: e => this.erro.set(mensagemErro(e)) });
    this.api.adminProfessores().subscribe({ next: x => this.professores.set(x), error: e => this.erro.set(mensagemErro(e)) });
    this.api.adminBloqueios().subscribe({ next: x => this.bloqueios.set(x), error: e => this.erro.set(mensagemErro(e)) });
  }

  editarRecurso(item: AdminRecurso): void {
    this.editandoRecurso = item.id;
    this.recurso = { nome: item.nome, tipo: item.tipo, categoria: item.categoria ?? '', localizacao: item.localizacao,
      capacidade: item.capacidade, restrito: item.restrito, abertura: item.abertura.slice(0, 5), fechamento: item.fechamento.slice(0, 5) };
  }

  salvarRecurso(): void {
    if (this.enviando()) return;
    this.enviando.set(true); this.erro.set('');
    const request = this.editandoRecurso === null
      ? this.api.criarRecurso(this.recurso)
      : this.api.editarRecurso(this.editandoRecurso, this.recurso);
    request.subscribe({
      next: () => { this.enviando.set(false); this.editandoRecurso = null; this.recurso = {
        nome: '', tipo: 'SALA', categoria: '', localizacao: '', capacidade: 1, restrito: false, abertura: '07:00', fechamento: '22:00'
      }; this.feedback.set('Recurso salvo com sucesso.'); this.carregar(); },
      error: e => { this.enviando.set(false); this.erro.set(mensagemErro(e)); }
    });
  }

  ativarRecurso(item: AdminRecurso): void {
    this.api.alterarAtivoRecurso(item.id, !item.ativo).subscribe({
      next: () => { this.feedback.set('Situação do recurso atualizada.'); this.carregar(); },
      error: e => this.erro.set(mensagemErro(e))
    });
  }

  editarProfessor(item: AdminProfessor): void {
    this.editandoProfessor = item.id;
    this.professor = { nome: item.nome, competencia: item.competencia };
  }

  salvarProfessor(): void {
    if (this.enviando()) return;
    this.enviando.set(true); this.erro.set('');
    const request = this.editandoProfessor === null
      ? this.api.criarProfessor(this.professor)
      : this.api.editarProfessor(this.editandoProfessor, this.professor);
    request.subscribe({
      next: () => { this.enviando.set(false); this.editandoProfessor = null; this.professor = { nome: '', competencia: '' };
        this.feedback.set('Professor salvo com sucesso.'); this.carregar(); },
      error: e => { this.enviando.set(false); this.erro.set(mensagemErro(e)); }
    });
  }

  ativarProfessor(item: AdminProfessor): void {
    this.api.alterarAtivoProfessor(item.id, !item.ativo).subscribe({
      next: () => { this.feedback.set('Situação do professor atualizada.'); this.carregar(); },
      error: e => this.erro.set(mensagemErro(e))
    });
  }

  salvarBloqueio(): void {
    if (this.enviando()) return;
    this.enviando.set(true); this.erro.set('');
    this.api.criarBloqueio(this.bloqueio).subscribe({
      next: () => { this.enviando.set(false); this.bloqueio = { recursoId: 0, inicio: '', fim: '', motivo: '' };
        this.feedback.set('Bloqueio registrado.'); this.carregar(); },
      error: e => { this.enviando.set(false); this.erro.set(mensagemErro(e)); }
    });
  }

  removerBloqueio(item: AdminBloqueio): void {
    if (!confirm('Remover este bloqueio de manutenção?')) return;
    this.api.removerBloqueio(item.id).subscribe({
      next: () => { this.feedback.set('Bloqueio removido.'); this.carregar(); },
      error: e => this.erro.set(mensagemErro(e))
    });
  }
}
