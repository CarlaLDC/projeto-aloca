import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  AdminUsuario, UsuarioPedido, AdminRecurso, AdminProfessor, AdminBloqueio, Aprovacoes, Disponibilidade, LoginResposta, Movimentacao, Movimentacoes, Notificacoes, OpcoesReserva, Painel,
  Relatorio, ReservaDetalhe, ReservaPedido, ReservaResumo, Usuario
} from './api.models';

/** Chamadas HTTP ao back-end. Em desenvolvimento, /api é encaminhado para localhost:8080 (proxy.conf.json). */
@Injectable({ providedIn: 'root' })
export class ApiService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api';

  // 01 · Entrar
  login(email: string, senha: string): Observable<LoginResposta> {
    return this.http.post<LoginResposta>(`${this.base}/auth/login`, { email, senha });
  }
  eu(): Observable<Usuario> {
    return this.http.get<Usuario>(`${this.base}/auth/me`);
  }
  recuperarSenha(email: string): Observable<{ mensagem: string }> {
    return this.http.post<{ mensagem: string }>(`${this.base}/auth/recuperar-senha`, { email });
  }

  // 02 · Visão geral
  painel(): Observable<Painel> {
    return this.http.get<Painel>(`${this.base}/painel`);
  }

  // 03 · Disponibilidade
  disponibilidade(data: string, tipo?: string, busca?: string, capacidadeMinima?: number): Observable<Disponibilidade> {
    let params = new HttpParams().set('data', data);
    if (capacidadeMinima != null) params = params.set('capacidadeMinima', capacidadeMinima);
    if (tipo) params = params.set('tipo', tipo);
    if (busca?.trim()) params = params.set('busca', busca.trim());
    return this.http.get<Disponibilidade>(`${this.base}/disponibilidade`, { params });
  }

  // 04 · Nova reserva, 05 · Conflito, 06 · Minhas reservas, 07 · Detalhe
  opcoesReserva(): Observable<OpcoesReserva> {
    return this.http.get<OpcoesReserva>(`${this.base}/reservas/opcoes`);
  }
  criarReserva(pedido: ReservaPedido): Observable<ReservaDetalhe> {
    return this.http.post<ReservaDetalhe>(`${this.base}/reservas`, pedido);
  }
  minhasReservas(): Observable<ReservaResumo[]> {
    return this.http.get<ReservaResumo[]>(`${this.base}/reservas/minhas`);
  }
  reserva(codigo: string): Observable<ReservaDetalhe> {
    return this.http.get<ReservaDetalhe>(`${this.base}/reservas/${encodeURIComponent(codigo)}`);
  }
  alterarReserva(codigo: string, pedido: ReservaPedido): Observable<ReservaDetalhe> {
    return this.http.put<ReservaDetalhe>(`${this.base}/reservas/${encodeURIComponent(codigo)}`, pedido);
  }
  cancelarReserva(codigo: string, motivo: string): Observable<ReservaDetalhe> {
    return this.http.post<ReservaDetalhe>(`${this.base}/reservas/${encodeURIComponent(codigo)}/cancelamento`, { motivo });
  }

  // 08 · Aprovações
  aprovacoes(): Observable<Aprovacoes> {
    return this.http.get<Aprovacoes>(`${this.base}/aprovacoes`);
  }
  aprovar(codigo: string): Observable<ReservaDetalhe> {
    return this.http.post<ReservaDetalhe>(`${this.base}/aprovacoes/${encodeURIComponent(codigo)}/aprovar`, {});
  }
  rejeitar(codigo: string, justificativa: string): Observable<ReservaDetalhe> {
    return this.http.post<ReservaDetalhe>(`${this.base}/aprovacoes/${encodeURIComponent(codigo)}/rejeitar`, { justificativa });
  }

  // 09 · Retiradas e devoluções
  movimentacoes(): Observable<Movimentacoes> {
    return this.http.get<Movimentacoes>(`${this.base}/movimentacoes`);
  }
  registrarRetirada(codigo: string): Observable<Movimentacao> {
    return this.http.post<Movimentacao>(`${this.base}/movimentacoes/${encodeURIComponent(codigo)}/retirada`, {});
  }
  registrarDevolucao(codigo: string): Observable<Movimentacao> {
    return this.http.post<Movimentacao>(`${this.base}/movimentacoes/${encodeURIComponent(codigo)}/devolucao`, {});
  }

  // 11 · Relatórios
  relatorio(inicio: string, fim: string): Observable<Relatorio> {
    return this.http.get<Relatorio>(`${this.base}/relatorios`, { params: { inicio, fim } });
  }
  relatorioCsv(inicio: string, fim: string): Observable<Blob> {
    return this.http.get(`${this.base}/relatorios/csv`, { params: { inicio, fim }, responseType: 'blob' });
  }

  // RF-03: pesquisa por nome e competência de professor (sem privilégios de administrador).
  pesquisarProfessores(competencia = ''): Observable<AdminProfessor[]> {
    return this.http.get<AdminProfessor[]>(`${this.base}/professores`, { params: { competencia } });
  }

  // RF-02 e RF-08: operações de administração persistidas no backend.
  adminRecursos(): Observable<AdminRecurso[]> { return this.http.get<AdminRecurso[]>(`${this.base}/admin/recursos`); }
  adminProfessores(): Observable<AdminProfessor[]> { return this.http.get<AdminProfessor[]>(`${this.base}/admin/professores`); }
  adminBloqueios(): Observable<AdminBloqueio[]> { return this.http.get<AdminBloqueio[]>(`${this.base}/admin/bloqueios`); }
  criarRecurso(pedido: Omit<AdminRecurso, 'id' | 'ativo'>): Observable<AdminRecurso> {
    return this.http.post<AdminRecurso>(`${this.base}/admin/recursos`, pedido);
  }
  editarRecurso(id: number, pedido: Omit<AdminRecurso, 'id' | 'ativo'>): Observable<AdminRecurso> {
    return this.http.put<AdminRecurso>(`${this.base}/admin/recursos/${id}`, pedido);
  }
  alterarAtivoRecurso(id: number, ativo: boolean): Observable<AdminRecurso> {
    return this.http.post<AdminRecurso>(`${this.base}/admin/recursos/${id}/${ativo ? 'ativar' : 'desativar'}`, {});
  }
  criarProfessor(pedido: { nome: string; competencia: string }): Observable<AdminProfessor> {
    return this.http.post<AdminProfessor>(`${this.base}/admin/professores`, pedido);
  }
  editarProfessor(id: number, pedido: { nome: string; competencia: string }): Observable<AdminProfessor> {
    return this.http.put<AdminProfessor>(`${this.base}/admin/professores/${id}`, pedido);
  }
  alterarAtivoProfessor(id: number, ativo: boolean): Observable<AdminProfessor> {
    return this.http.post<AdminProfessor>(`${this.base}/admin/professores/${id}/${ativo ? 'ativar' : 'desativar'}`, {});
  }
  criarBloqueio(pedido: { recursoId: number; inicio: string; fim: string; motivo: string }): Observable<AdminBloqueio> {
    return this.http.post<AdminBloqueio>(`${this.base}/admin/bloqueios`, pedido);
  }
  removerBloqueio(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/admin/bloqueios/${id}`);
  }
  registrarNaoComparecimento(codigo: string, motivo: string): Observable<ReservaDetalhe> {
    return this.http.post<ReservaDetalhe>(`${this.base}/reservas/${encodeURIComponent(codigo)}/nao-compareceu`, { motivo });
  }

  adminUsuarios(): Observable<AdminUsuario[]> { return this.http.get<AdminUsuario[]>(`${this.base}/admin/usuarios`); }
  criarUsuario(p: UsuarioPedido): Observable<AdminUsuario> { return this.http.post<AdminUsuario>(`${this.base}/admin/usuarios`, p); }
  editarUsuario(id: number, p: UsuarioPedido): Observable<AdminUsuario> { return this.http.put<AdminUsuario>(`${this.base}/admin/usuarios/${id}`, p); }
  ativarUsuario(id: number, ativo: boolean): Observable<AdminUsuario> { return this.http.patch<AdminUsuario>(`${this.base}/admin/usuarios/${id}/ativo`, { ativo }); }
  senhaUsuario(id: number, senha: string): Observable<void> { return this.http.put<void>(`${this.base}/admin/usuarios/${id}/senha`, { senha }); }

  // Topo das telas
  notificacoes(): Observable<Notificacoes> {
    return this.http.get<Notificacoes>(`${this.base}/notificacoes`);
  }
  marcarNotificacoesLidas(): Observable<void> {
    return this.http.post<void>(`${this.base}/notificacoes/lidas`, {});
  }
}
