import { TestBed } from '@angular/core/testing';
import { Approvals } from './approvals';
import { DemoAuditLog } from '../../core/demo-audit-log';

describe('Approvals audit integration', () => {
  beforeEach(async () => TestBed.configureTestingModule({ imports: [Approvals] }).compileComponents());

  it('records pending -> approved', () => {
    const fixture = TestBed.createComponent(Approvals);
    fixture.detectChanges();
    fixture.componentInstance.approve();
    const event = TestBed.inject(DemoAuditLog).events()[0];
    expect(event.fromState).toBe('Pendente');
    expect(event.toState).toBe('Aprovada');
    expect(event.action).toBe('Aprovação');
  });

  it('records pending -> rejected with the reason', () => {
    const fixture = TestBed.createComponent(Approvals);
    fixture.detectChanges();
    fixture.componentInstance.rejectionReason.set('Recurso indisponível');
    fixture.componentInstance.reject();
    const event = TestBed.inject(DemoAuditLog).events()[0];
    expect(event.fromState).toBe('Pendente');
    expect(event.toState).toBe('Rejeitada');
    expect(event.description).toBe('Recurso indisponível');
  });
});
