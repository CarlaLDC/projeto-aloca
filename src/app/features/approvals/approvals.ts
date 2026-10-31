import { Component, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

type ApprovalStatus = 'Pendente' | 'Aprovada' | 'Rejeitada';

@Component({
  selector: 'app-approvals',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './approvals.html',
  styleUrl: './approvals.css'
})
export class Approvals {
  readonly status = signal<ApprovalStatus>('Pendente');
  readonly pendingCount = signal(3);
  readonly approvedToday = signal(7);
  readonly awaitingAdjustment = signal(1);
  readonly rejectionDialogOpen = signal(false);
  readonly rejectionReason = signal('');
  readonly feedback = signal('');

  approve(): void {
    if (this.status() !== 'Pendente') return;
    this.status.set('Aprovada');
    this.pendingCount.update(value => Math.max(0, value - 1));
    this.approvedToday.update(value => value + 1);
    this.feedback.set('Solicitação aprovada nesta demonstração. Nenhuma alteração foi enviada ao servidor.');
  }

  openRejection(): void {
    if (this.status() !== 'Pendente') return;
    this.rejectionReason.set('');
    this.rejectionDialogOpen.set(true);
  }

  closeRejection(): void {
    this.rejectionDialogOpen.set(false);
  }

  reject(): void {
    if (this.status() !== 'Pendente' || !this.rejectionReason().trim()) return;
    this.status.set('Rejeitada');
    this.pendingCount.update(value => Math.max(0, value - 1));
    this.rejectionDialogOpen.set(false);
    this.feedback.set('Solicitação rejeitada nesta demonstração. Motivo: ' + this.rejectionReason().trim());
  }
}
