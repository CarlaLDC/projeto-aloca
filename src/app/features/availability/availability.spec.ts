import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { Availability } from './availability';

describe('Availability', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [Availability],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the selected date and filters resources locally', () => {
    const fixture = TestBed.createComponent(Availability);
    fixture.detectChanges();
    const request = http.expectOne(req => req.url === '/api/disponibilidade');
    expect(request.request.params.get('data')).toBe(fixture.componentInstance.selectedDate());
    request.flush({
      data: fixture.componentInstance.selectedDate(),
      resumo: { disponiveis: 2, ocupados: 1, total: 3 },
      recursos: [
        { id: 1, nome: 'Sala 204', tipo: 'SALA', tipoRotulo: 'Salas', localizacao: 'Bloco A', capacidade: 40, restrito: false, status: 'Disponível', livreDe: '09:00:00', livreAte: '11:00:00', ocupacoes: [], janelasLivres: [] },
        { id: 2, nome: 'Laboratório 02', tipo: 'LABORATORIO', tipoRotulo: 'Laboratórios', localizacao: 'Bloco C', capacidade: 32, restrito: true, status: 'Ocupado', livreDe: null, livreAte: null, ocupacoes: [], janelasLivres: [] },
        { id: 3, nome: 'Projetor 04', tipo: 'EQUIPAMENTO', tipoRotulo: 'Equipamentos', localizacao: 'Central', capacidade: 1, restrito: false, status: 'Disponível', livreDe: null, livreAte: null, ocupacoes: [], janelasLivres: [] }
      ]
    });

    fixture.componentInstance.setType('Laboratórios');
    expect(fixture.componentInstance.filteredResources().map(resource => resource.id)).toEqual([2]);
    fixture.componentInstance.searchTerm.set('Bloco A');
    expect(fixture.componentInstance.filteredResources()).toEqual([]);
  });

  it('creates reservation parameters for an available resource', () => {
    const fixture = TestBed.createComponent(Availability);
    const component = fixture.componentInstance;
    const resource = {
      id: 3, nome: 'Projetor 04', tipo: 'EQUIPAMENTO' as const, tipoRotulo: 'Equipamentos',
      localizacao: 'Central', capacidade: 1, restrito: false, status: 'Disponível' as const,
      livreDe: '10:30:00', livreAte: '12:00:00', ocupacoes: [], janelasLivres: []
    };
    expect(component.reservaParams(resource)).toEqual({ data: component.selectedDate(), material: '3', inicio: '10:30' });
  });
});