import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Reports } from './reports';

describe('Reports', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Reports],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('requests the selected period and maps resource metrics', () => {
    const fixture = TestBed.createComponent(Reports);
    fixture.detectChanges();
    const request = http.expectOne(req => req.url === '/api/relatorios');
    expect(request.request.params.get('inicio')).toBe(fixture.componentInstance.inicio());
    expect(request.request.params.get('fim')).toBe(fixture.componentInstance.fim());
    request.flush({
      inicio: fixture.componentInstance.inicio(), fim: fixture.componentInstance.fim(),
      utilizacaoEspacos: 50, horasAlocadas: 12.5, conflitosEvitados: 2, totalReservas: 4,
      recursos: [{ id: 1, nome: 'Sala 204', tipoRotulo: 'Sala', utilizacao: 50, horas: 12.5, reservas: 4, conflitos: 1 }],
      professores: []
    });
    expect(fixture.componentInstance.resources()).toEqual([
      { name: 'Sala 204', usage: 50, hours: 12.5, reservations: 4, conflicts: 1 }
    ]);
    expect(fixture.componentInstance.horas(12.5)).toBe('12,5');
  });
});