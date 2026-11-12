import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Overview } from './overview';

describe('Overview', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    localStorage.clear(); // Impede vazamento de sessão dos testes de autenticação.
    await TestBed.configureTestingModule({
      imports: [Overview],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads the dashboard data', () => {
    const fixture = TestBed.createComponent(Overview);
    fixture.detectChanges();
    http.expectOne('/api/painel').flush({
      perfil: 'Solicitante', data: '2030-10-22', reservasHoje: 1, recursosDisponiveis: 4,
      aguardandoAprovacao: 0, agenda: []
    });
    expect(fixture.componentInstance.painel()?.reservasHoje).toBe(1);
    expect(fixture.componentInstance.today).toBeTruthy();
    expect(fixture.componentInstance.erro()).toBe('');
  });

  it('opens the new reservation form', () => {
    const navigate = spyOn(TestBed.inject(Router), 'navigate').and.resolveTo(true);
    TestBed.createComponent(Overview).componentInstance.openReservation();
    expect(navigate).toHaveBeenCalledWith(['/nova-reserva']);
  });
});