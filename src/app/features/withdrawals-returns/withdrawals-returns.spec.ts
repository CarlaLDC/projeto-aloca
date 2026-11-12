import { TestBed } from '@angular/core/testing';
import { WithdrawalsReturns } from './withdrawals-returns';
import { DemoAuditLog } from '../../core/demo-audit-log';

describe('WithdrawalsReturns audit integration', () => {
  beforeEach(async () => TestBed.configureTestingModule({ imports: [WithdrawalsReturns] }).compileComponents());

  it('records pickup and return transitions', () => {
    const fixture = TestBed.createComponent(WithdrawalsReturns);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.registerPickup('MOV-0318');
    component.registerReturn('MOV-0316');
    const events = TestBed.inject(DemoAuditLog).events();
    expect(events.length).toBe(2);
    expect(events[0].fromState).toBe('Aguardando retirada');
    expect(events[0].toState).toBe('Em uso');
    expect(events[1].fromState).toBe('Devolução pendente');
    expect(events[1].toState).toBe('Devolvido');
  });
});
