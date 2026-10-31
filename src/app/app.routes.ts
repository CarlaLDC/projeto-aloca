import { Routes } from '@angular/router';
import { Login } from './features/login/login';
import { Overview } from './features/overview/overview';
import { NewReservation } from './features/new-reservation/new-reservation';
import { Availability } from './features/availability/availability';
import { ScheduleConflict } from './features/schedule-conflict/schedule-conflict';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, title: 'Entrar | Aloca' },
  { path: 'visao-geral', component: Overview, title: 'Visão geral | Aloca' },
  { path: 'disponibilidade', component: Availability, title: 'Disponibilidade | Aloca' },
  { path: 'nova-reserva', component: NewReservation, title: 'Nova reserva | Aloca' },
  { path: 'conflito-horario', component: ScheduleConflict, title: 'Conflito de horário | Aloca' },
  { path: '**', redirectTo: 'visao-geral' }
];
