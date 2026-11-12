import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { Administration } from './administration';
import { DemoAuditLog } from '../../core/demo-audit-log';

describe('Administration audit integration', () => {
  beforeEach(async () => TestBed.configureTestingModule({ imports: [Administration], providers: [provideRouter([])] }).compileComponents());

  it('records resource, user and block changes', () => {
    const fixture = TestBed.createComponent(Administration);
    fixture.detectChanges();
    const component = fixture.componentInstance;
    component.toggleResource(component.resources()[0]);
    component.toggleUser(component.users()[0]);
    component.removeBlock(component.blocks()[0]);
    const events = TestBed.inject(DemoAuditLog).events();
    expect(events.map(event => event.entityType)).toEqual(['Recurso', 'Usuário', 'Bloqueio']);
    expect(events[0].fromState).toBe('Ativo');
    expect(events[0].toState).toBe('Inativo');
    expect(events[1].toState).toBe('Inativo');
    expect(events[2].toState).toBe('Removido');
  });
});
