import { TestBed } from '@angular/core/testing';
import { DemoAuditLog } from './demo-audit-log';

describe('DemoAuditLog', () => {
  beforeEach(() => TestBed.configureTestingModule({}));

  it('records each state transition with before/after state and a timestamp', () => {
    const audit = TestBed.inject(DemoAuditLog);
    audit.append({
      entityId: 'MOV-TEST',
      entityType: 'Movimentação',
      fromState: 'Aguardando retirada',
      toState: 'Em uso',
      action: 'Retirada registrada',
      description: 'Teste de retirada'
    });
    const event = audit.events()[0];
    expect(event.entityId).toBe('MOV-TEST');
    expect(event.fromState).toBe('Aguardando retirada');
    expect(event.toState).toBe('Em uso');
    expect(event.actor).toContain('não autenticado');
    expect(Number.isNaN(Date.parse(event.occurredAt))).toBeFalse();
    expect(event.id).toBeTruthy();
  });

  it('preserves multiple events in order', () => {
    const audit = TestBed.inject(DemoAuditLog);
    audit.append({ entityId: 'RES-TEST', entityType: 'Reserva', fromState: 'Pendente', toState: 'Aprovada', action: 'Aprovação', description: 'Aprovada' });
    audit.append({ entityId: 'RES-TEST', entityType: 'Reserva', fromState: 'Aprovada', toState: 'Cancelada', action: 'Cancelamento', description: 'Cancelada' });
    expect(audit.events().length).toBe(2);
    expect(audit.events()[1].fromState).toBe('Aprovada');
  });
});
