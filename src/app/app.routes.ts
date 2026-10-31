import { Routes } from '@angular/router';
import { Login } from './features/login/login';
import { Overview } from './features/overview/overview';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, title: 'Entrar | Aloca' },
  { path: 'visao-geral', component: Overview, title: 'Visão geral | Aloca' },
  { path: '**', redirectTo: 'visao-geral' }
];
