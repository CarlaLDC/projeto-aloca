import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { WithdrawalsReturns } from './withdrawals-returns';

describe('WithdrawalsReturns', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WithdrawalsReturns],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads movements and filters by status and search term', () => {
    const fixture = TestBed.createComponent(WithdrawalsReturns);
    fixture.detectChanges();
    http.expectOne('/api/movimentacoes').flush({
      aguardandoRetirada: 1, emUso: 1, devolucoesPendentes: 0,
      movimentacoes: [
        {
          codigo: 'MOV-01', reserva: 'RES-01', item: 'Projetor 04', categoria: 'Equipamento',
          responsavel: 'Ana Martins', destino: 'Bloco A', retiradaPrevista: '2030-10-22T09:00:00',
          devolucaoPrevista: '2030-10-22T11:00:00', retiradaEm: null, devolucaoEm: null,
          status: 'AGUARDANDO_RETIRADA', statusRotulo: 'Aguardando retirada', protocolo: null
        },
        {
          codigo: 'MOV-02', reserva: 'RES-02', item: 'Notebook 02', categoria: 'Equipamento',
          responsavel: 'Bruno Lima', destino: 'Bloco C', retiradaPrevista: '2030-10-22T09:00:00',
          devolucaoPrevista: '2030-10-22T11:00:00', retiradaEm: '2030-10-22T09:00:00', devolucaoEm: null,
          status: 'EM_USO', statusRotulo: 'Em uso', protocolo: 'PR-02'
        }
      ]
    });

    expect(fixture.componentInstance.waitingCount()).toBe(1);
    fixture.componentInstance.setFilter('EM_USO');
    expect(fixture.componentInstance.filteredMovements().map(movement => movement.codigo)).toEqual(['MOV-02']);
    fixture.componentInstance.searchTerm.set('projetor');
    expect(fixture.componentInstance.filteredMovements()).toEqual([]);
  });

  it('registers a pickup and reloads the movement list', () => {
    const fixture = TestBed.createComponent(WithdrawalsReturns);
    fixture.detectChanges();
    const movement = {
      codigo: 'MOV-01', reserva: 'RES-01', item: 'Projetor 04', categoria: 'Equipamento',
      responsavel: 'Ana Martins', destino: 'Bloco A', retiradaPrevista: '2030-10-22T09:00:00',
      devolucaoPrevista: '2030-10-22T11:00:00', retiradaEm: null, devolucaoEm: null,
      status: 'AGUARDANDO_RETIRADA' as const, statusRotulo: 'Aguardando retirada', protocolo: null
    };
    http.expectOne('/api/movimentacoes').flush({
      aguardandoRetirada: 1, emUso: 0, devolucoesPendentes: 0, movimentacoes: [movement]
    });

    fixture.componentInstance.registerPickup(movement);
    http.expectOne({ method: 'POST', url: '/api/movimentacoes/MOV-01/retirada' }).flush({
      ...movement, status: 'EM_USO', statusRotulo: 'Em uso', protocolo: 'PR-01'
    });
    expect(fixture.componentInstance.feedback()).toContain('PR-01');
    http.expectOne('/api/movimentacoes').flush({
      aguardandoRetirada: 0, emUso: 1, devolucoesPendentes: 0, movimentacoes: []
    });
  });
});