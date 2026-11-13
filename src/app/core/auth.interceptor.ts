import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

/** Envia o token em toda chamada /api e volta para o login quando a sessão expira. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const token = auth.token;
  const pedido = token && req.url.startsWith('/api') && !req.url.startsWith('/api/auth/login')
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;
  return next(pedido).pipe(
    catchError((erro: unknown) => {
      if (erro instanceof HttpErrorResponse && erro.status === 401 && !req.url.startsWith('/api/auth/login')) {
        auth.limpar();
        void router.navigate(['/login'], { queryParams: { expirada: 1 } });
      }
      return throwError(() => erro);
    })
  );
};
