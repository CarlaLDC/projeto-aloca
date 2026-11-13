import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PerfilCodigo } from './api.models';
import { AuthService } from './auth.service';

/** Exige login e, se informado, um dos perfis. Quem não tem o perfil volta para sua tela inicial. */
export function exigirPerfil(...perfis: PerfilCodigo[]): CanActivateFn {
  return () => {
    const auth = inject(AuthService);
    const router = inject(Router);
    if (!auth.autenticado()) return router.createUrlTree(['/login']);
    if (perfis.length && !auth.possui(...perfis)) return router.createUrlTree([auth.telaInicial()]);
    return true;
  };
}

/** Na tela de login: quem já entrou vai direto para a tela inicial. */
export const somenteVisitante: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.autenticado() ? inject(Router).createUrlTree([auth.telaInicial()]) : true;
};
