import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { MyReservations } from './my-reservations';

describe('MyReservations', () => {
  let http: HttpTestingController;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [MyReservations],
      providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()]
    }).compileComponents();
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('loads reservations and formats their schedule', () => {
    const fixture = TestBed.createComponent(MyReservations);
    fixture.detectChanges();
    http.expectOne('/api/reservas/minhas').flush([{
      codigo: 'RES-0248', atividade: 'Aula de Programação Web', recurso: 'Laboratório 02',
      data: '2030-10-22', inicio: '09:00:00', fim: '11:00:00', status: 'APROVADA', statusRotulo: 'Aprovada'
    }]);
    const reservation = fixture.componentInstance.reservations()[0];
    expect(reservation.schedule).toContain('09h');
    expect(reservation.schedule).toContain('11h');
    expect(reservation.statusClass).toBeTruthy();
    expect(fixture.componentInstance.carregado()).toBeTrue();
  });

  it('navigates to the new reservation form', () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigate').and.resolveTo(true);
    TestBed.createComponent(MyReservations).componentInstance.newReservation();
    expect(navigate).toHaveBeenCalledWith(['/nova-reserva']);
  });
});
