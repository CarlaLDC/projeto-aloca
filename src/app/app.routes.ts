import { Routes } from '@angular/router';
import { Login } from './features/login/login';
import { Overview } from './features/overview/overview';
import { Availability } from './features/availability/availability';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, title: 'Entrar | Aloca' },
  { path: 'visao-geral', component: Overview, title: 'Visão geral | Aloca' },
  { path: 'disponibilidade', component: Availability, title: 'Disponibilidade | Aloca' },
  { path: '**', redirectTo: 'visao-geral' }
];
