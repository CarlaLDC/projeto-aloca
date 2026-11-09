import { Component, ElementRef, inject, signal, viewChild } from '@angular/core';
import { FormsModule, NgForm } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DEMO_RESERVATIONS, DemoReservation } from './reservation-data';

type Draft = Pick<DemoReservation, 'activity' | 'date' | 'start' | 'end' | 'room' | 'teacher' | 'material'>;

@Component({
  selector: 'app-reservation-detail',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './reservation-detail.html',
  styleUrl: './reservation-detail.css'
})
export class ReservationDetail {
  readonly reservation = signal<DemoReservation | null>(null);
  readonly action = signal<'edit' | 'cancel' | null>(null);
  readonly error = signal('');
  readonly feedback = signal('');
  readonly dialog = viewChild<ElementRef<HTMLDialogElement>>('actionDialog');
  draft: Draft = { activity: '', date: '', start: '', end: '', room: '', teacher: '', material: '' };
  reason = '';

  constructor() {
    inject(ActivatedRoute).paramMap.pipe(takeUntilDestroyed()).subscribe(params => {
      const item = DEMO_RESERVATIONS.find(reservation => reservation.id === params.get('id'));
      this.reservation.set(item ? { ...item, history: item.history.map(event => ({ ...event })) } : null);
      this.feedback.set('');
      this.closeDialog();
    });
  }

  get canManage(): boolean {
    const status = this.reservation()?.status;
    return status === 'Aprovada' || status === 'Solicitada';
  }

  openAction(action: 'edit' | 'cancel'): void {
    const item = this.reservation();
    if (!item || !this.canManage) return;
    this.draft = { activity: item.activity, date: item.date, start: item.start, end: item.end,
      room: item.room, teacher: item.teacher, material: item.material };
    this.reason = '';
    this.error.set('');
    this.action.set(action);
    this.dialog()?.nativeElement.showModal();
  }

  closeDialog(): void {
    this.dialog()?.nativeElement.close();
    this.action.set(null);
    this.error.set('');
  }

  save(form: NgForm): void {
    const item = this.reservation();
    if (!item || !this.canManage || !this.action()) return;
    if (form.invalid || (this.action() === 'edit' && (!this.draft.activity.trim() || !this.draft.room.trim())) ||
        (this.action() === 'cancel' && !this.reason.trim())) {
      form.control.markAllAsTouched();
      this.error.set('Preencha os campos obrigatórios.');
      return;
    }
    if (this.action() === 'edit' && this.draft.end <= this.draft.start) {
      this.error.set('O término deve ser posterior ao início.');
      return;
    }
    const editing = this.action() === 'edit';
    const date = new Intl.DateTimeFormat('pt-BR', {
      timeZone: 'America/Sao_Paulo', day: '2-digit', month: '2-digit', hour: '2-digit', minute: '2-digit'
    }).format(new Date());
    const updated = editing ? { ...item, ...this.draft } : { ...item, status: 'Cancelada' as const, statusClass: 'cancelled' };
    this.reservation.set({ ...updated, history: [...item.history, {
      date, actor: item.requester, action: editing ? 'Alterada · simulação' : 'Cancelada · simulação',
      description: editing ? 'Dados alterados nesta demonstração' : this.reason.trim()
    }] });
    this.feedback.set(editing ? 'Alteração simulada. Nenhum dado foi enviado ao servidor.' : 'Cancelamento simulado. Nenhum dado foi enviado ao servidor.');
    this.closeDialog();
  }
}
