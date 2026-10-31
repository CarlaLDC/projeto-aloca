import { Routes } from '@angular/router';
import { Login } from './features/login/login';

export const routes: Routes = [
  { path: '', pathMatch: 'full', redirectTo: 'login' },
  { path: 'login', component: Login, title: 'Entrar | Aloca' }
];
