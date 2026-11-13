import { TestBed } from '@angular/core/testing';
import { Router, ActivatedRouteSnapshot, RouterStateSnapshot, provideRouter } from '@angular/router';
import { AuthService } from './auth.service';
import { exigirPerfil, somenteVisitante } from './auth.guard';

describe('Guards de navegação', () => {
  let auth: jasmine.SpyObj<AuthService>;
  beforeEach(() => {
    auth = jasmine.createSpyObj('AuthService', ['autenticado', 'possui', 'telaInicial']); auth.telaInicial.and.returnValue('/visao-geral');
    TestBed.configureTestingModule({providers:[provideRouter([]), {provide:AuthService,useValue:auth}]});
  });
  function executar(guard: ReturnType<typeof exigirPerfil>) { return TestBed.runInInjectionContext(() => guard({} as ActivatedRouteSnapshot, {} as RouterStateSnapshot)); }
  it('redireciona visitante para login', () => { auth.autenticado.and.returnValue(false); expect(String(executar(exigirPerfil()))).toBe('/login'); });
  it('permite autenticado sem restrição de perfil', () => { auth.autenticado.and.returnValue(true); expect(executar(exigirPerfil())).toBeTrue(); });
  it('permite perfil autorizado e nega outro', () => {
    auth.autenticado.and.returnValue(true); auth.possui.and.returnValue(true); expect(executar(exigirPerfil('ADMINISTRADOR'))).toBeTrue();
    auth.possui.and.returnValue(false); expect(String(executar(exigirPerfil('ADMINISTRADOR')))).toBe('/visao-geral');
  });
  it('login aceita visitante e redireciona usuário já autenticado', () => {
    auth.autenticado.and.returnValue(false); expect(executar(somenteVisitante)).toBeTrue();
    auth.autenticado.and.returnValue(true); expect(String(executar(somenteVisitante))).toBe('/visao-geral');
  });
});
