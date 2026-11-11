import { Injectable, computed, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Observable, map, tap } from 'rxjs';
import { ApiService } from './api.service';
import { Notificacoes, PerfilCodigo, Usuario } from './api.models';

const CHAVE = 'aloca.sessao';

interface Sessao {
  token: string;
  expiraEm: string;
  usuario: Usuario;
}

/** Guarda a sessão do usuário (token + dados) e expõe o nome e o perfil para as telas. */
@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly api = inject(ApiService);
  private readonly router = inject(Router);
  private readonly sessao = signal<Sessao | null>(this.lerSessao());

  readonly usuario = computed(() => this.sessao()?.usuario ?? null);
  readonly nome = computed(() => this.usuario()?.nome ?? '');
  readonly iniciais = computed(() => this.usuario()?.iniciais ?? '');
  readonly perfilRotulo = computed(() => this.usuario()?.perfilRotulo ?? '');
  readonly unidade = computed(() => this.usuario()?.unidade ?? '');
  readonly notificacoes = signal<Notificacoes | null>(null);
  readonly naoLidas = computed(() => this.notificacoes()?.naoLidas ?? 0);

  get token(): string | null {
    const s = this.sessao();
    if (!s) return null;
    if (new Date(s.expiraEm).getTime() <= Date.now()) {
      this.limpar();
      return null;
    }
    return s.token;
  }

  autenticado(): boolean {
    return this.token !== null;
  }

  possui(...perfis: PerfilCodigo[]): boolean {
    const atuais = this.usuario()?.perfis ?? [];
    return perfis.some(p => atuais.includes(p));
  }

  entrar(email: string, senha: string): Observable<Usuario> {
    return this.api.login(email, senha).pipe(
      tap(r => this.gravar({ token: r.token, expiraEm: r.expiraEm, usuario: r.usuario })),
      tap(() => this.atualizarNotificacoes()),
      map(r => r.usuario)
    );
  }

  /** Tela inicial de acordo com o perfil. */
  telaInicial(): string {
    if (this.possui('SOLICITANTE', 'PROFESSOR')) return '/visao-geral';
    if (this.possui('RESPONSAVEL')) return '/aprovacoes';
    if (this.possui('ADMINISTRADOR')) return '/relatorios';
    return '/visao-geral';
  }

  sair(): void {
    this.limpar();
    void this.router.navigate(['/login']);
  }

  limpar(): void {
    this.sessao.set(null);
    this.notificacoes.set(null);
    try { localStorage.removeItem(CHAVE); } catch { /* armazenamento indisponível */ }
  }

  atualizarNotificacoes(): void {
    if (!this.autenticado()) return;
    this.api.notificacoes().subscribe({ next: n => this.notificacoes.set(n), error: () => undefined });
  }

  marcarNotificacoesLidas(): void {
    this.api.marcarNotificacoesLidas().subscribe({ next: () => this.atualizarNotificacoes(), error: () => undefined });
  }

  private gravar(s: Sessao): void {
    this.sessao.set(s);
    try { localStorage.setItem(CHAVE, JSON.stringify(s)); } catch { /* armazenamento indisponível */ }
  }

  private lerSessao(): Sessao | null {
    try {
      const bruto = localStorage.getItem(CHAVE);
      if (!bruto) return null;
      const s = JSON.parse(bruto) as Sessao;
      return s?.token && new Date(s.expiraEm).getTime() > Date.now() ? s : null;
    } catch {
      return null;
    }
  }
}
