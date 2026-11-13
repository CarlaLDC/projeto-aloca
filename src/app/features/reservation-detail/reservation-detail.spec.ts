import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { ActivatedRoute, convertToParamMap, provideRouter } from '@angular/router';
import { BehaviorSubject } from 'rxjs';
import { ReservationDetail } from './reservation-detail';
import { ReservaDetalhe } from '../../core/api.models';

function reserva(parcial: Partial<ReservaDetalhe> = {}): ReservaDetalhe {
  return {
    codigo: 'RES-0248', atividade: 'Aula de Programação Web', data: '2030-10-22', inicio: '09:00:00', fim: '11:00:00',
    espaco: { id: 5, nome: 'Laboratório de informática 02', tipo: 'LABORATORIO', tipoRotulo: 'Laboratórios', localizacao: 'Bloco C', capacidade: 32, restrito: true, rotulo: '' },
    professor: { id: 1, nome: 'André Lima', competencia: 'Sistemas', rotulo: '' },
    materiais: [], solicitante: 'Ana Martins', status: 'APROVADA', statusRotulo: 'Aprovada', motivoRejeicao: null,
    exigeAprovacao: true, podeGerenciar: true, criadaEm: '2030-10-21T16:42:00',
    historico: [
      { dataHora: '2030-10-21T16:42:00', ator: 'Ana Martins', acao: 'Solicitada', descricao: 'Reserva criada', estadoAnterior: null, estadoNovo: 'Solicitada' },
      { dataHora: '2030-10-21T17:05:00', ator: 'Roberto Almeida', acao: 'Aprovada', descricao: 'Alocação validada', estadoAnterior: 'Solicitada', estadoNovo: 'Aprovada' }
    ],
    ...parcial
  };
}

describe('ReservationDetail', () => {
  let params: BehaviorSubject<ReturnType<typeof convertToParamMap>>;
  let http: HttpTestingController;

  beforeEach(async () => {
    params = new BehaviorSubject(convertToParamMap({ id: 'RES-0248' }));
    await TestBed.configureTestingModule({
      imports: [ReservationDetail],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting(),
        { provide: ActivatedRoute, useValue: { paramMap: params } }]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render(dados = reserva()) {
    const fixture = TestBed.createComponent(ReservationDetail);
    fixture.detectChanges();
    http.expectOne('/api/reservas/RES-0248').flush(dados);
    fixture.detectChanges();
    await fixture.whenStable();
    return fixture;
  }

  it('loads the reservation and its audit history from the API', async () => {
    const fixture = await render();
    expect(fixture.nativeElement.textContent).toContain('Aula de Programação Web');
    expect(fixture.nativeElement.textContent).toContain('Roberto Almeida');
    expect(fixture.nativeElement.textContent).toContain('Solicitada → Aprovada');
  });

  it('shows "not found" for an unknown code', async () => {
    const fixture = TestBed.createComponent(ReservationDetail);
    fixture.detectChanges();
    http.expectOne('/api/reservas/RES-0248').flush({ mensagem: 'Reserva RES-0248 não encontrada.' }, { status: 404, statusText: 'Not Found' });
    fixture.detectChanges();
    expect(fixture.nativeElement.textContent).toContain('Reserva não encontrada');
    expect(fixture.componentInstance.reservation()).toBeNull();
  });

  it('cancels with a reason and shows the updated status', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    component.openAction('cancel');
    fixture.detectChanges();
    await fixture.whenStable();
    component.reason = 'Atividade remarcada';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('dialog form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    const req = http.expectOne('/api/reservas/RES-0248/cancelamento');
    expect(req.request.body).toEqual({ motivo: 'Atividade remarcada' });
    req.flush(reserva({ status: 'CANCELADA', statusRotulo: 'Cancelada', podeGerenciar: false }));
    http.match('/api/notificacoes');
    fixture.detectChanges();
    expect(component.reservation()?.status).toBe('CANCELADA');
    expect(component.canManage).toBeFalse();
    expect(component.feedback()).toContain('cancelada');
  });

  it('does not send an invalid edit', async () => {
    const fixture = await render();
    const component = fixture.componentInstance;
    component.openAction('edit');
    http.expectOne('/api/reservas/opcoes').flush({ espacos: [], professores: [], materiais: [] });
    fixture.detectChanges();
    await fixture.whenStable();
    component.draft.end = '08:00';
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.nativeElement.querySelector('dialog form').dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    expect(component.error()).toContain('posterior');
    expect(component.reservation()?.fim).toBe('11:00:00');
  });

  it('keeps reservations the user cannot manage read only', async () => {
    const fixture = await render(reserva({ status: 'CONCLUIDA', statusRotulo: 'Concluída', podeGerenciar: false }));
    fixture.componentInstance.openAction('edit');
    expect(fixture.componentInstance.action()).toBeNull();
    expect(fixture.nativeElement.querySelector('button.primary').disabled).toBeTrue();
  });
});
