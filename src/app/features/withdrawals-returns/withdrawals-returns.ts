import { Component, computed, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';

type MovementStatus = 'Aguardando retirada' | 'Em uso' | 'Devolução pendente' | 'Devolvido';
type FilterStatus = 'Todos' | MovementStatus;

interface Movement {
  id: string;
  reservation: string;
  item: string;
  category: string;
  requester: string;
  destination: string;
  pickup: string;
  due: string;
  status: MovementStatus;
  protocol?: string;
}

@Component({
  selector: 'app-withdrawals-returns',
  standalone: true,
  imports: [FormsModule, RouterLink],
  templateUrl: './withdrawals-returns.html',
  styleUrl: './withdrawals-returns.css'
})
export class WithdrawalsReturns {
  readonly searchTerm = signal('');
  readonly selectedStatus = signal<FilterStatus>('Todos');
  readonly feedback = signal('');

  readonly filters: FilterStatus[] = ['Todos', 'Aguardando retirada', 'Em uso', 'Devolução pendente', 'Devolvido'];

  readonly movements = signal<Movement[]>([
    { id: 'MOV-0318', reservation: 'RES-0251', item: 'Kit de robótica educacional', category: 'Equipamentos', requester: 'Ricardo Alves', destination: 'Laboratório 02', pickup: 'Hoje · 09:00', due: 'Hoje · 12:00', status: 'Aguardando retirada' },
    { id: 'MOV-0317', reservation: 'RES-0250', item: 'Projetor multimídia Epson', category: 'Equipamentos', requester: 'Mariana Costa', destination: 'Sala 204 · Bloco A', pickup: 'Hoje · 08:30', due: 'Hoje · 11:30', status: 'Em uso' },
    { id: 'MOV-0316', reservation: 'RES-0247', item: 'Notebook Dell · patrimônio 0482', category: 'Informática', requester: 'Pedro Santos', destination: 'Sala de reuniões', pickup: 'Ontem · 14:00', due: 'Hoje · 10:00', status: 'Devolução pendente' },
    { id: 'MOV-0315', reservation: 'RES-0245', item: 'Caixa de som amplificada', category: 'Áudio e vídeo', requester: 'Juliana Lima', destination: 'Auditório', pickup: 'Ontem · 08:00', due: 'Ontem · 17:00', status: 'Devolvido', protocol: 'DEV-0182' }
  ]);

  readonly filteredMovements = computed(() => {
    const term = this.searchTerm().trim().toLocaleLowerCase('pt-BR');
    return this.movements().filter(movement => {
      const matchesStatus = this.selectedStatus() === 'Todos' || movement.status === this.selectedStatus();
      const haystack = `${movement.id} ${movement.reservation} ${movement.item} ${movement.requester} ${movement.destination}`.toLocaleLowerCase('pt-BR');
      return matchesStatus && (!term || haystack.includes(term));
    });
  });

  readonly waitingCount = computed(() => this.movements().filter(item => item.status === 'Aguardando retirada').length);
  readonly inUseCount = computed(() => this.movements().filter(item => item.status === 'Em uso').length);
  readonly returnCount = computed(() => this.movements().filter(item => item.status === 'Devolução pendente').length);

  setFilter(status: FilterStatus): void {
    this.selectedStatus.set(status);
  }

  registerPickup(id: string): void {
    this.updateMovement(id, 'Em uso', 'Retirada registrada');
  }

  registerReturn(id: string): void {
    this.updateMovement(id, 'Devolvido', 'Devolução registrada');
  }

  private updateMovement(id: string, status: MovementStatus, action: string): void {
    const movement = this.movements().find(item => item.id === id);
    if (!movement) return;
    this.movements.update(items => items.map(item =>
      item.id === id ? { ...item, status, protocol: status === 'Devolvido' ? this.createProtocol() : item.protocol } : item
    ));
    this.feedback.set(`${action} para ${movement.item}. Registro demonstrativo; nenhum dado foi enviado ao servidor.`);
  }

  private createProtocol(): string {
    return 'DEV-' + String(this.movements().filter(item => item.status === 'Devolvido').length + 183).padStart(4, '0');
  }
}
