import { HttpErrorResponse } from '@angular/common/http';
import { ErroApi, StatusReserva } from './api.models';

const fuso = 'America/Sao_Paulo';

/** "09:00:00" → "09:00" */
export function hm(hora: string | null | undefined): string {
  return hora ? hora.slice(0, 5) : '—';
}

/** "09:00:00" → "09h", "09:30:00" → "09h30" */
export function hCurta(hora: string): string {
  const [h, m] = hora.split(':');
  return m === '00' ? `${h}h` : `${h}h${m}`;
}

/** "2026-10-22" → "22/10/2026" */
export function dataBr(data: string): string {
  return data.split('-').reverse().join('/');
}

/** "2026-10-22" → "22/10" */
export function diaMes(data: string): string {
  const [, m, d] = data.split('-');
  return `${d}/${m}`;
}

/** Data local (horário de Brasília) no formato yyyy-mm-dd. */
export function hojeIso(base = new Date()): string {
  const partes = new Intl.DateTimeFormat('en-CA', { timeZone: fuso, year: 'numeric', month: '2-digit', day: '2-digit' })
    .format(base);
  return partes;
}

export function somarDias(dataIso: string, dias: number): string {
  const d = new Date(`${dataIso}T12:00:00Z`);
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
}

export function dataExtensa(dataIso: string): string {
  return new Intl.DateTimeFormat('pt-BR', { weekday: 'long', day: 'numeric', month: 'long', year: 'numeric', timeZone: 'UTC' })
    .format(new Date(`${dataIso}T12:00:00Z`));
}

/** "2026-10-21T16:42:00" → "21/10 · 16:42" */
export function dataHoraCurta(dataHora: string): string {
  const [data, hora] = dataHora.split('T');
  return `${diaMes(data)} · ${hm(hora)}`;
}

/** "2026-10-21T16:42:00" → "Hoje · 16:42", "Ontem · 08:00", "Amanhã · 09:00" ou "21/10 · 16:42". */
export function diaRelativo(dataHora: string, hoje = hojeIso()): string {
  const [data, hora] = dataHora.split('T');
  const rotulos: Record<string, string> = {
    [hoje]: 'Hoje', [somarDias(hoje, -1)]: 'Ontem', [somarDias(hoje, 1)]: 'Amanhã'
  };
  return `${rotulos[data] ?? diaMes(data)} · ${hm(hora)}`;
}

/** "há 25 minutos", "há 3 horas", "há 2 dias" (datas da API vêm no horário de Brasília, sem fuso). */
export function tempoDecorrido(dataHora: string, agora = new Date()): string {
  const brasilia = new Date(agora.toLocaleString('en-US', { timeZone: fuso }));
  const minutos = Math.max(0, Math.round((brasilia.getTime() - new Date(dataHora).getTime()) / 60000));
  if (minutos < 1) return 'agora mesmo';
  if (minutos < 60) return `há ${minutos} minuto${minutos === 1 ? '' : 's'}`;
  const horas = Math.round(minutos / 60);
  if (horas < 24) return `há ${horas} hora${horas === 1 ? '' : 's'}`;
  const dias = Math.round(horas / 24);
  return `há ${dias} dia${dias === 1 ? '' : 's'}`;
}

/** Classe CSS usada pelas telas para cada status de reserva. */
export function classeStatus(status: StatusReserva): string {
  switch (status) {
    case 'APROVADA':
    case 'EM_USO':
      return 'approved';
    case 'SOLICITADA':
      return 'requested';
    case 'CONCLUIDA':
      return 'completed';
    default:
      return 'cancelled';
  }
}

/** Mensagem amigável a partir de um erro HTTP. */
export function mensagemErro(erro: unknown, padrao = 'Não foi possível concluir a operação. Tente novamente.'): string {
  if (erro instanceof HttpErrorResponse) {
    if (erro.status === 0) return 'Não foi possível falar com o servidor. Verifique se o back-end está rodando.';
    const corpo = erro.error as Partial<ErroApi> | null;
    if (corpo && typeof corpo.mensagem === 'string') return corpo.mensagem;
  }
  return padrao;
}
