import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AdminUsuario, UsuarioPedido, PerfilCodigo, AdminRecurso, AdminProfessor, AdminBloqueio } from '../../core/api.models';
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
  readonly aba = signal<'recursos' | 'professores' | 'bloqueios' | 'usuarios'>('recursos');
  recurso: Omit<AdminRecurso, 'id' | 'ativo'> = {
    nome: '', tipo: 'SALA', categoria: '', localizacao: '', capacidade: 1,
    restrito: false, abertura: '07:00', fechamento: '22:00'
  };
  professor = { nome: '', competencia: '' };
  bloqueio = { recursoId: 0, inicio: '', fim: '', motivo: '' };
  editandoRecurso: number | null = null;
  editandoProfessor: number | null = null;

  readonly usuarios = signal<AdminUsuario[]>([]);
  readonly perfis: PerfilCodigo[] = ['SOLICITANTE', 'PROFESSOR', 'RESPONSAVEL', 'ADMINISTRADOR'];
  usuario: UsuarioPedido = { nome: '', email: '', unidade: '', perfis: ['SOLICITANTE'], senha: '' };
  editandoUsuario: number | null = null;
  senhaNova = '';
  redefinindo: number | null = null;

  editarUsuario(u: AdminUsuario): void {
    this.editandoUsuario = u.id;
    this.usuario = { nome: u.nome, email: u.email, unidade: u.unidade, perfis: [...u.perfis] };
  }
  marcarPerfil(p: PerfilCodigo, marcado: boolean): void {
    this.usuario.perfis = marcado ? [...new Set([...this.usuario.perfis, p])] : this.usuario.perfis.filter(x => x !== p);
  }
  limparUsuario(): void {
    this.editandoUsuario = null;
    this.usuario = { nome: '', email: '', unidade: '', perfis: ['SOLICITANTE'], senha: '' };
  }
  salvarUsuario(): void {
    if (this.enviando()) return;
    if (!this.usuario.nome.trim() || !this.usuario.email.trim() || !this.usuario.unidade.trim() ||
        !this.usuario.perfis.length || (this.editandoUsuario === null && (this.usuario.senha?.length ?? 0) < 8)) {
      this.erro.set('Informe nome, e-mail, unidade, ao menos um perfil e senha inicial com 8 caracteres.'); return;
    }
    this.enviando.set(true); this.erro.set('');
    const req = this.editandoUsuario === null ? this.api.criarUsuario(this.usuario) : this.api.editarUsuario(this.editandoUsuario, this.usuario);
    const propria = this.editandoUsuario === this.auth.usuario()?.id;
    req.subscribe({ next: () => {
      this.enviando.set(false); this.limparUsuario();
      if (propria) { this.auth.sair(); return; }
      this.feedback.set('Usuário salvo. Sessões anteriores foram encerradas após alteração.'); this.carregar();
    }, error: e => { this.enviando.set(false); this.erro.set(mensagemErro(e)); } });
  }
  ativarUsuario(u: AdminUsuario): void {
    this.api.ativarUsuario(u.id, !u.ativo).subscribe({ next: () => {
      this.feedback.set('Situação do usuário atualizada.'); this.carregar();
    }, error: e => this.erro.set(mensagemErro(e)) });
  }
  redefinirSenha(): void {
    if (this.redefinindo === null || this.enviando()) return;
    if (this.senhaNova.length < 8) { this.erro.set('A senha deve ter ao menos 8 caracteres.'); return; }
    const propria = this.redefinindo === this.auth.usuario()?.id;
    this.enviando.set(true);
    this.api.senhaUsuario(this.redefinindo, this.senhaNova).subscribe({ next: () => {
      this.enviando.set(false); this.senhaNova = ''; this.redefinindo = null;
      if (propria) { this.auth.sair(); return; }
      this.feedback.set('Senha redefinida e sessões anteriores encerradas.');
    }, error: e => { this.enviando.set(false); this.erro.set(mensagemErro(e)); } });
  }

  ngOnInit(): void { this.carregar(); }

  carregar(): void {
    this.api.adminUsuarios().subscribe({ next: x => this.usuarios.set(x), error: e => this.erro.set(mensagemErro(e)) });
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
    if (!this.recurso.nome.trim() || !this.recurso.localizacao.trim() ||
        !Number.isInteger(this.recurso.capacidade) || this.recurso.capacidade < 1 ||
        this.recurso.fechamento <= this.recurso.abertura) {
      this.erro.set('Informe nome e localização, capacidade inteira positiva e horário de fechamento posterior à abertura.');
      return;
    }
    this.enviando.set(true); this.erro.set(''); this.feedback.set('');
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
    if (!this.professor.nome.trim() || !this.professor.competencia.trim()) {
      this.erro.set('Informe o nome e a competência do professor.');
      return;
    }
    this.enviando.set(true); this.erro.set(''); this.feedback.set('');
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
    if (!this.bloqueio.recursoId || !this.bloqueio.inicio || !this.bloqueio.fim ||
        this.bloqueio.fim <= this.bloqueio.inicio || !this.bloqueio.motivo.trim()) {
      this.erro.set('Informe recurso, período válido e motivo para o bloqueio.');
      return;
    }
    this.enviando.set(true); this.erro.set(''); this.feedback.set('');
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
