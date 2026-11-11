import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import {
  Aprovacoes, Disponibilidade, LoginResposta, Movimentacao, Movimentacoes, Notificacoes, OpcoesReserva, Painel,
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
  disponibilidade(data: string, tipo?: string, busca?: string): Observable<Disponibilidade> {
    let params = new HttpParams().set('data', data);
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

  // Topo das telas
  notificacoes(): Observable<Notificacoes> {
    return this.http.get<Notificacoes>(`${this.base}/notificacoes`);
  }
  marcarNotificacoesLidas(): Observable<void> {
    return this.http.post<void>(`${this.base}/notificacoes/lidas`, {});
  }
}
