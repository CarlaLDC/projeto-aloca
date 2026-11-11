import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Approvals } from './approvals';

describe('Approvals', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Approvals],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads pending requests from the API', () => {
    const fixture = TestBed.createComponent(Approvals);
    fixture.detectChanges();
    http.expectOne('/api/aprovacoes').flush({
      pendentes: 1,
      aprovadasHoje: 0,
      rejeitadasHoje: 0,
      solicitacoes: [{
        codigo: 'RES-100', atividade: 'Aula de laboratório', solicitante: 'Ana Martins',
        criadaEm: '2030-10-21T16:42:00', recursos: 'Laboratório 02', data: '2030-10-22',
        inicio: '09:00:00', fim: '11:00:00', professor: null,
        disponibilidadeValidada: true, problemas: []
      }]
    });
    fixture.detectChanges();
    expect(fixture.componentInstance.dados()?.solicitacoes[0].codigo).toBe('RES-100');
    expect(fixture.componentInstance.erro()).toBe('');
  });

  it('approves a request and reloads the pending list', () => {
    const fixture = TestBed.createComponent(Approvals);
    fixture.detectChanges();
    const initial = http.expectOne('/api/aprovacoes');
    const request = {
      codigo: 'RES-100', atividade: 'Aula', solicitante: 'Ana Martins', criadaEm: '2030-10-21T16:42:00',
      recursos: 'Sala 204', data: '2030-10-22', inicio: '09:00:00', fim: '11:00:00', professor: null,
      disponibilidadeValidada: true, problemas: []
    };
    initial.flush({ pendentes: 1, aprovadasHoje: 0, rejeitadasHoje: 0, solicitacoes: [request] });
    fixture.componentInstance.approve(request);
    http.expectOne({ method: 'POST', url: '/api/aprovacoes/RES-100/aprovar' }).flush({});
    expect(fixture.componentInstance.feedback()).toContain('RES-100 aprovada');
    http.expectOne('/api/aprovacoes').flush({ pendentes: 0, aprovadasHoje: 1, rejeitadasHoje: 0, solicitacoes: [] });
    expect(fixture.componentInstance.dados()?.pendentes).toBe(0);
  });
});