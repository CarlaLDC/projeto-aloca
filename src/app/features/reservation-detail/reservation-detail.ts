import { HttpErrorResponse } from '@angular/common/http';
import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { ConflitoResposta, OpcoesReserva, ReservaDetalhe } from '../../core/api.models';
import { dataBr, dataHoraCurta, hm, mensagemErro } from '../../core/formato';

interface Draft {
  activity: string;
  date: string;
  start: string;
  end: string;
  room: number | null;
  teacher: number | null;
  material: number | null;
}

@Component({
  selector: 'app-reservation-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reservation-detail.html',
  styleUrl: './reservation-detail.css'
})
export class ReservationDetail {
  private readonly api = inject(ApiService);
  readonly auth = inject(AuthService);

  readonly reservation = signal<ReservaDetalhe | null>(null);
  readonly loading = signal(true);
  readonly notFound = signal('');
  readonly options = signal<OpcoesReserva | null>(null);
  readonly action = signal<'edit' | 'cancel' | null>(null);
  readonly error = signal('');
  readonly feedback = signal('');
  readonly sending = signal(false);
  readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('actionDialog');
  readonly hm = hm;
  readonly dataBr = dataBr;
  readonly dataHoraCurta = dataHoraCurta;
  draft: Draft = { activity: '', date: '', start: '', end: '', room: null, teacher: null, material: null };
  reason = '';

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      this.feedback.set('');
      this.closeDialog();
      this.carregar(params.get('id') ?? '');
    });
  }

  get canManage(): boolean {
    return this.reservation()?.podeGerenciar ?? false;
  }

  get podeMarcarAusencia(): boolean {
    const status = this.reservation()?.status;
    return this.auth.possui('RESPONSAVEL') && (status === 'APROVADA' || status === 'EM_USO');
  }

  registrarAusencia(): void {
    const item = this.reservation();
    if (!item || !this.podeMarcarAusencia || this.sending()) return;
    const motivo = window.prompt('Justificativa do não comparecimento:')?.trim();
    if (!motivo) return;
    this.sending.set(true);
    this.error.set('');
    this.api.registrarNaoComparecimento(item.codigo, motivo).subscribe({
      next: atualizada => {
        this.reservation.set(atualizada);
        this.feedback.set('Não comparecimento registrado no histórico.');
        this.sending.set(false);
      },
      error: e => { this.error.set(mensagemErro(e)); this.sending.set(false); }
    });
  }

  get materiais(): string {
    const lista = this.reservation()?.materiais ?? [];
    return lista.length ? lista.map(m => m.nome).join(', ') : 'Sem materiais';
  }

  carregar(codigo: string): void {
    this.loading.set(true);
    this.notFound.set('');
    this.api.reserva(codigo).subscribe({
      next: r => { this.reservation.set(r); this.loading.set(false); },
      error: e => {
        this.reservation.set(null);
        this.loading.set(false);
        this.notFound.set(mensagemErro(e, 'Não há dados para o código informado.'));
      }
    });
  }

  openAction(action: 'edit' | 'cancel'): void {
    const item = this.reservation();
    if (!item || !this.canManage) return;
    this.draft = {
      activity: item.atividade, date: item.data, start: hm(item.inicio), end: hm(item.fim),
      room: item.espaco.id, teacher: item.professor?.id ?? null, material: item.materiais[0]?.id ?? null
    };
    this.reason = '';
    this.error.set('');
    this.action.set(action);
    if (action === 'edit' && !this.options()) {
      this.api.opcoesReserva().subscribe({ next: o => this.options.set(o), error: e => this.error.set(mensagemErro(e)) });
    }
    this.dialog()?.nativeElement.showModal();
  }

  closeDialog(): void {
    this.dialog()?.nativeElement.close();
    this.action.set(null);
    this.error.set('');
  }

  save(form: NgForm): void {
    const item = this.reservation();
    const action = this.action();
    if (!item || !this.canManage || !action || this.sending()) return;
    if (form.invalid || (action === 'edit' && (!this.draft.activity.trim() || this.draft.room === null)) ||
        (action === 'cancel' && !this.reason.trim())) {
      form.control.markAllAsTouched();
      this.error.set('Preencha os campos obrigatórios.');
      return;
    }
    if (action === 'edit' && this.draft.end <= this.draft.start) {
      this.error.set('O término deve ser posterior ao início.');
      return;
    }
    this.sending.set(true);
    const pedido = action === 'edit'
      ? this.api.alterarReserva(item.codigo, {
          atividade: this.draft.activity.trim(), data: this.draft.date, inicio: this.draft.start, fim: this.draft.end,
          espacoId: Number(this.draft.room), professorId: this.draft.teacher === null ? null : Number(this.draft.teacher),
          materialIds: this.draft.material === null ? [] : [Number(this.draft.material)]
        })
      : this.api.cancelarReserva(item.codigo, this.reason.trim());
    pedido.subscribe({
      next: atualizada => {
        this.sending.set(false);
        this.reservation.set(atualizada);
        this.feedback.set(action === 'edit'
          ? (atualizada.status === 'SOLICITADA' ? 'Alteração salva. A reserva aguarda nova aprovação do responsável.' : 'Alteração salva.')
          : 'Reserva cancelada.');
        this.closeDialog();
        this.auth.atualizarNotificacoes();
      },
      error: (e: unknown) => {
        this.sending.set(false);
        if (e instanceof HttpErrorResponse && e.status === 409 && (e.error as ConflitoResposta)?.alternativas) {
          const c = e.error as ConflitoResposta;
          const sugestao = c.alternativas[0];
          this.error.set(c.mensagem + (sugestao
            ? ` Sugestão: ${sugestao.espaco}, ${dataBr(sugestao.data)} das ${hm(sugestao.inicio)} às ${hm(sugestao.fim)}.` : ''));
          return;
        }
        this.error.set(mensagemErro(e));
      }
    });
  }
}
