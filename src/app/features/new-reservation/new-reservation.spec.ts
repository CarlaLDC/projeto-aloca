import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { NewReservation } from './new-reservation';
import { ConflitoStore } from '../../core/conflito.store';
import { OpcoesReserva } from '../../core/api.models';

const OPCOES: OpcoesReserva = {
  espacos: [
    { id: 5, nome: 'Laboratório de informática 02', tipo: 'LABORATORIO', tipoRotulo: 'Laboratórios', localizacao: 'Bloco C', capacidade: 32, restrito: true, rotulo: 'Laboratório de informática 02 · 32 lugares' },
    { id: 1, nome: 'Sala 204', tipo: 'SALA', tipoRotulo: 'Salas', localizacao: 'Bloco A', capacidade: 40, restrito: false, rotulo: 'Sala 204 · 40 lugares' }
  ],
  professores: [{ id: 1, nome: 'André Lima', competencia: 'Sistemas', rotulo: 'André Lima · Sistemas' }],
  materiais: [{ id: 8, nome: 'Projetor 04', tipo: 'EQUIPAMENTO', tipoRotulo: 'Equipamentos', localizacao: 'Central', capacidade: 1, restrito: false, rotulo: 'Projetor 04 · 1 unidade' }]
};

describe('NewReservation', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [NewReservation],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  async function render() {
    const fixture = TestBed.createComponent(NewReservation);
    fixture.detectChanges();
    http.expectOne('/api/reservas/opcoes').flush(OPCOES);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    return fixture;
  }

  async function submit(fixture: Awaited<ReturnType<typeof render>>) {
    fixture.detectChanges();
    await fixture.whenStable();
    (fixture.nativeElement.querySelector('form') as HTMLFormElement).dispatchEvent(new Event('submit', { bubbles: true, cancelable: true }));
    fixture.detectChanges();
  }

  it('loads rooms, teachers and materials from the API', async () => {
    const fixture = await render();
    const options = fixture.nativeElement.querySelectorAll('select[name="room"] option');
    expect(options.length).toBe(2);
    expect(fixture.componentInstance.room).toBe(5);
  });

  it('rejects an end time before the start time', async () => {
    const fixture = await render();
    fixture.componentInstance.activity = 'Aula';
    fixture.componentInstance.end = '08:00';
    await submit(fixture);
    expect(fixture.componentInstance.reviewing()).toBeFalse();
    expect(fixture.nativeElement.querySelector('[role="alert"]').textContent).toContain('posterior');
  });

  it('preserves values when returning from review to editing', async () => {
    const fixture = await render();
    fixture.componentInstance.activity = 'Projeto integrador';
    await submit(fixture);
    expect(fixture.componentInstance.reviewing()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('Projeto integrador');
    fixture.nativeElement.querySelector('.secondary').click();
    fixture.detectChanges();
    await fixture.whenStable();
    expect(fixture.nativeElement.querySelector('[name="activity"]').value).toBe('Projeto integrador');
  });

  it('shows the approval notice only for restricted resources', async () => {
    const fixture = await render();
    expect(fixture.nativeElement.querySelector('.notice')).not.toBeNull();
    fixture.componentInstance.room = 1;
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('.notice')).toBeNull();
  });

  it('sends the reservation and shows the code returned by the server', async () => {
    const fixture = await render();
    const c = fixture.componentInstance;
    c.activity = 'Aula de Programação Web';
    c.room = 1;
    c.material = 8;
    await submit(fixture);
    c.confirm();
    const req = http.expectOne('/api/reservas');
    expect(req.request.method).toBe('POST');
    expect(req.request.body).toEqual(jasmine.objectContaining({ espacoId: 1, materialIds: [8], professorId: null }));
    req.flush({ codigo: 'RES-0100', status: 'APROVADA' });
    http.match('/api/notificacoes');
    fixture.detectChanges();
    expect(c.completed()).toBeTrue();
    expect(fixture.nativeElement.textContent).toContain('RES-0100');
  });

  it('opens the conflict screen when the server answers 409', async () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    const fixture = await render();
    fixture.componentInstance.activity = 'Aula';
    await submit(fixture);
    fixture.componentInstance.confirm();
    const conflito = { mensagem: 'Indisponível', conflitos: [{ tipo: 'Sala' }], alternativas: [] };
    http.expectOne('/api/reservas').flush(conflito, { status: 409, statusText: 'Conflict' });
    expect(navigate).toHaveBeenCalledWith(['/conflito-horario']);
    expect(TestBed.inject(ConflitoStore).ultimo()?.mensagem).toBe('Indisponível');
  });
});
