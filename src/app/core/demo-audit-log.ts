import { Injectable, signal } from '@angular/core';

export interface DemoAuditEvent {
  id: string;
  entityId: string;
  entityType: 'Reserva' | 'Movimentação';
  fromState: string;
  toState: string;
  action: string;
  actor: string;
  occurredAt: string;
  description: string;
}

/**
 * Histórico compartilhado apenas durante a sessão da SPA.
 * Não substitui auditoria persistente no servidor nem identidade autenticada.
 */
@Injectable({ providedIn: 'root' })
export class DemoAuditLog {
  readonly events = signal<DemoAuditEvent[]>([]);

  append(event: Omit<DemoAuditEvent, 'id' | 'occurredAt' | 'actor'>): void {
    this.events.update(events => [...events, {
      ...event,
      id: globalThis.crypto?.randomUUID?.() ?? `audit-${Date.now()}-${events.length + 1}`,
      occurredAt: new Date().toISOString(),
      actor: 'Demonstração — ator não autenticado'
    }]);
  }
}
