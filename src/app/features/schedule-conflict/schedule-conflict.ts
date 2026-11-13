import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { ConflitoStore } from '../../core/conflito.store';
import { Alternativa, ItemConflito } from '../../core/api.models';
import { dataExtensa, hm, hojeIso, somarDias } from '../../core/formato';

const MESES = ['jan', 'fev', 'mar', 'abr', 'mai', 'jun', 'jul', 'ago', 'set', 'out', 'nov', 'dez'];

function minutos(hora: string): number {
  const [h, m] = hora.split(':').map(Number);
  return h * 60 + m;
}

@Component({
  selector: 'app-schedule-conflict',
  standalone: true,
  imports: [RouterLink],
  templateUrl: './schedule-conflict.html',
  styleUrl: './schedule-conflict.css'
})
export class ScheduleConflict {
  readonly auth = inject(AuthService);
  private readonly store = inject(ConflitoStore);
  readonly hm = hm;

  /** Conflito devolvido pela API na última tentativa de reserva (null se a tela foi aberta diretamente). */
  readonly dados = this.store.ultimo;
  readonly principal = computed<ItemConflito | null>(() => this.dados()?.conflitos[0] ?? null);

  readonly conflict = computed(() => {
    const d = this.dados();
    const c = this.principal();
    if (!d || !c) return null;
    const hoje = hojeIso();
    return {
      resource: c.recurso,
      location: c.localizacao,
      date: (d.data === hoje ? 'Hoje, ' : '') + dataExtensa(d.data),
      requestedTime: `${hm(d.inicio)} – ${hm(d.fim)}`,
      conflictingTime: `${hm(c.inicio)} – ${hm(c.fim)}`,
      activity: d.atividade,
      responsible: c.responsavel,
      kind: c.tipo
    };
  });

  /** Posição das barras da linha do tempo, em % de uma janela que cobre os dois horários. */
  readonly barras = computed(() => {
    const d = this.dados();
    const c = this.principal();
    if (!d || !c) return null;
    const ini = Math.max(0, Math.min(minutos(d.inicio), minutos(c.inicio)) - 60);
    const fim = Math.min(24 * 60, Math.max(minutos(d.fim), minutos(c.fim)) + 60);
    const total = fim - ini;
    const barra = (a: string, b: string) => ({
      left: ((minutos(a) - ini) / total) * 100,
      width: ((minutos(b) - minutos(a)) / total) * 100
    });
    return { pedido: barra(d.inicio, d.fim), existente: barra(c.inicio, c.fim) };
  });

  readonly sobreposicao = computed(() => {
    const min = this.principal()?.sobreposicaoMinutos ?? 0;
    const h = Math.floor(min / 60), m = min % 60;
    const partes = [h ? `${h} hora${h > 1 ? 's' : ''}` : '', m ? `${m} minuto${m > 1 ? 's' : ''}` : ''].filter(Boolean);
    return partes.join(' e ');
  });

  /** Mensagem da API sem o prefixo, que já aparece no título do aviso. */
  readonly motivo = computed(() => {
    const texto = (this.dados()?.mensagem ?? '').replace(/^Este horário não está disponível:\s*/, '');
    return texto.charAt(0).toUpperCase() + texto.slice(1);
  });

  readonly alternatives = computed(() => (this.dados()?.alternativas ?? []).map(a => this.alternativa(a)));

  private alternativa(a: Alternativa) {
    const hoje = hojeIso();
    const [, mes, dia] = a.data.split('-');
    const nomeDia = a.data === hoje ? 'Hoje' : a.data === somarDias(hoje, 1) ? 'Amanhã'
      : new Intl.DateTimeFormat('pt-BR', { weekday: 'long', timeZone: 'UTC' }).format(new Date(`${a.data}T12:00:00Z`));
    return {
      day: nomeDia.charAt(0).toUpperCase() + nomeDia.slice(1),
      date: `${dia} ${MESES[Number(mes) - 1]}`,
      time: `${hm(a.inicio)} – ${hm(a.fim)}`,
      resource: a.espaco,
      motivo: a.motivo,
      status: 'Disponível',
      params: this.parametros(a)
    };
  }

  /** Pré-preenche a nova reserva com a opção escolhida, mantendo atividade, professor e material. */
  private parametros(a: Alternativa): Record<string, string> {
    const d = this.dados()!;
    const p: Record<string, string> = {
      atividade: d.atividade, data: a.data, inicio: hm(a.inicio), fim: hm(a.fim), espaco: String(a.espacoId)
    };
    if (d.professorId !== null) p['professor'] = String(d.professorId);
    if (d.materialIds.length) p['material'] = String(d.materialIds[0]);
    return p;
  }

  editarParams(): Record<string, string> {
    const d = this.dados();
    if (!d) return {};
    const p: Record<string, string> = {
      atividade: d.atividade, data: d.data, inicio: hm(d.inicio), fim: hm(d.fim), espaco: String(d.espacoId)
    };
    if (d.professorId !== null) p['professor'] = String(d.professorId);
    if (d.materialIds.length) p['material'] = String(d.materialIds[0]);
    return p;
  }
}
