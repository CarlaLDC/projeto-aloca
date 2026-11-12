import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ReservationDetail } from './reservation-detail';

describe('ReservationDetail', () => {
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ id: 'RES-0248' }));
    await TestBed.configureTestingModule({
      imports: [ReservationDetail],
      providers: [provideRouter([]), { provide: ActivatedRoute, useValue: { paramMap: params } }]
    }).compileComponents();
  });

  async function render() {
    const fixture = TestBed.createComponent(ReservationDetail);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  it('loads the selected reservation and handles an unknown code without showing another reservation', async () => {
    const fixture = await render();
    expect(fixture.nativeElement.textContent).toContain('Aula de Programação Web');
    params.next(convertToParamMap({ id: 'RES-0249' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reserva de Projetor 04');
    expect(fixture.componentInstance.reservation()?.history.length).toBe(0);
    params.next(convertToParamMap({ id: 'invalid' }));
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reserva não encontrada');
    expect(fixture.componentInstance.reservation()).toBeNull();
  });

  it('cancels only after confirmation and appends a simulated audit event', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    component.openAction('cancel');
    fixture.detectChanges();
    await fixture.whenStable();
    component.closeDialog();
    expect(component.reservation()?.status).toBe('Aprovada');
    component.openAction('cancel');
    fixture.detectChanges();
    await fixture.whenStable();
    component.reason = 'Atividade remarcada';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('dialog form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(component.reservation()?.status).toBe('Cancelada');
    expect(component.reservation()?.history.at(-1)?.description).toBe('Atividade remarcada');
    expect(component.canManage).toBeFalse();
    expect(component.feedback()).toContain('simulado');
  });

  it('appends a local audit event when a valid edit is saved', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    const before = component.reservation()?.history.length ?? 0;
    component.openAction('edit');
    fixture.detectChanges();
    await fixture.whenStable();
    component.draft.activity = 'Aula revisada';
    fixture.nativeElement.querySelector('dialog form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(component.reservation()?.activity).toBe('Aula revisada');
    expect(component.reservation()?.history.length).toBe(before + 1);
    expect(component.reservation()?.history.at(-1)?.action).toContain('Alterada');
    expect(component.feedback()).toContain('simulada');
  });

  it('rejects an invalid edit without changing the original data or history', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    component.openAction('edit');
    fixture.detectChanges();
    await fixture.whenStable();
    component.draft.end = '08:00';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('dialog form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
    expect(component.error()).toContain('posterior');
    expect(component.reservation()?.end).toBe('11:00');
    expect(component.reservation()?.history.length).toBe(3);
  });

  it('keeps completed reservations read only', async () => {
    params.next(convertToParamMap({ id: 'RES-0241' }));
    const fixture = await render();
    fixture.componentInstance.openAction('edit');
    expect(fixture.componentInstance.action()).toBeNull();
    expect(fixture.componentInstance.canManage).toBeFalse();
    expect(fixture.nativeElement.querySelector('button').disabled).toBeTrue();
  });
});
