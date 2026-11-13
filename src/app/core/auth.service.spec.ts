import { TestBed } from '@angular/core/testing';
import { Router } from '@angular/router';
import { of, throwError } from 'rxjs';
import { ApiService } from './api.service';
import { AuthService } from './auth.service';
import { PerfilCodigo, Usuario } from './api.models';
describe('Sessão e permissões', () => {
    const usuario: Usuario = { id: 1, nome: 'Ana Silva', email: 'ana@aloca.edu.br', unidade: 'Centro', perfis: ['SOLICITANTE'], perfilRotulo: 'Solicitante', iniciais: 'AS' };
    let api: jasmine.SpyObj<ApiService>;
    let router: jasmine.SpyObj<Router>;
    const sessao = (perfis: PerfilCodigo[] = ['SOLICITANTE'], expiraEm = '2099-01-01') => ({ token: 'jwt', expiraEm, usuario: { ...usuario, perfis } });
    beforeEach(() => {
        localStorage.clear();
        api = jasmine.createSpyObj('ApiService', ['login', 'notificacoes', 'marcarNotificacoesLidas']);
        api.notificacoes.and.returnValue(of({ naoLidas: 2, itens: [] }));
        api.marcarNotificacoesLidas.and.returnValue(of(void 0));
        router = jasmine.createSpyObj('Router', ['navigate']);
        TestBed.configureTestingModule({ providers: [{ provide: ApiService, useValue: api }, { provide: Router, useValue: router }] });
    });
    afterEach(() => localStorage.clear());
    it('sem sessão não autentica nem consulta notificações', () => {
        const auth = TestBed.inject(AuthService);
        auth.atualizarNotificacoes();
        expect(auth.autenticado()).toBeFalse();
        expect(auth.possui('ADMINISTRADOR')).toBeFalse();
        expect(auth.nome()).toBe('');
        expect(auth.iniciais()).toBe('');
        expect(auth.perfilRotulo()).toBe('');
        expect(auth.unidade()).toBe('');
        expect(auth.naoLidas()).toBe(0);
        expect(api.notificacoes).not.toHaveBeenCalled();
    });
    for (const bruto of ['{inválido', 'null', JSON.stringify(sessao([], '2000-01-01')), JSON.stringify({ expiraEm: '2099-01-01' })]) {
        it('ignora sessão inválida: ' + bruto, () => { localStorage.setItem('aloca.sessao', bruto); expect(TestBed.inject(AuthService).autenticado()).toBeFalse(); });
    }
    for (const [perfil, rota] of [['SOLICITANTE', '/visao-geral'], ['PROFESSOR', '/visao-geral'], ['RESPONSAVEL', '/aprovacoes'], ['ADMINISTRADOR', '/administracao']] as const) {
        it('restaura sessão e direciona ' + perfil, () => {
            localStorage.setItem('aloca.sessao', JSON.stringify(sessao([perfil])));
            const auth = TestBed.inject(AuthService);
            expect(auth.token).toBe('jwt');
            expect(auth.telaInicial()).toBe(rota);
            expect(auth.possui(perfil)).toBeTrue();
            expect(auth.nome()).toBe(usuario.nome);
            expect(auth.iniciais()).toBe('AS');
            expect(auth.unidade()).toBe('Centro');
            expect(auth.perfilRotulo()).toBe('Solicitante');
        });
    }
    it('login persiste sessão e atualiza notificações; logout remove dados', () => {
        api.login.and.returnValue(of(sessao()));
        const auth = TestBed.inject(AuthService);
        let recebido: Usuario | undefined;
        auth.entrar(usuario.email, 'senha').subscribe(u => recebido = u);
        expect(recebido?.id).toBe(1);
        expect(api.login).toHaveBeenCalledWith(usuario.email, 'senha');
        expect(JSON.parse(localStorage.getItem('aloca.sessao')!).token).toBe('jwt');
        expect(auth.naoLidas()).toBe(2);
        auth.marcarNotificacoesLidas();
        expect(api.marcarNotificacoesLidas).toHaveBeenCalled();
        auth.sair();
        expect(auth.token).toBeNull();
        expect(auth.notificacoes()).toBeNull();
        expect(localStorage.getItem('aloca.sessao')).toBeNull();
        expect(router.navigate).toHaveBeenCalledWith(['/login']);
    });
    it('expira sessão durante o uso', () => {
        localStorage.setItem('aloca.sessao', JSON.stringify(sessao()));
        const auth = TestBed.inject(AuthService);
        spyOn(Date, 'now').and.returnValue(new Date('2100-01-01').getTime());
        expect(auth.token).toBeNull();
        expect(auth.usuario()).toBeNull();
    });
    it('falha de login é propagada sem criar sessão', () => {
        api.login.and.returnValue(throwError(() => new Error('Credenciais inválidas')));
        const auth = TestBed.inject(AuthService);
        let erro = '';
        auth.entrar('', '').subscribe({ error: e => erro = e.message });
        expect(erro).toBe('Credenciais inválidas');
        expect(auth.autenticado()).toBeFalse();
    });
    it('falhas de notificação não derrubam a sessão', () => {
        localStorage.setItem('aloca.sessao', JSON.stringify(sessao([])));
        api.notificacoes.and.returnValue(throwError(() => new Error()));
        api.marcarNotificacoesLidas.and.returnValue(throwError(() => new Error()));
        const auth = TestBed.inject(AuthService);
        auth.atualizarNotificacoes();
        auth.marcarNotificacoesLidas();
        expect(auth.autenticado()).toBeTrue();
        expect(auth.telaInicial()).toBe('/visao-geral');
    });
    it('funciona quando armazenamento está indisponível', () => {
        spyOn(Storage.prototype, 'getItem').and.throwError('indisponível');
        spyOn(Storage.prototype, 'setItem').and.throwError('indisponível');
        spyOn(Storage.prototype, 'removeItem').and.throwError('indisponível');
        api.login.and.returnValue(of(sessao()));
        const auth = TestBed.inject(AuthService);
        auth.entrar('', '').subscribe();
        expect(auth.autenticado()).toBeTrue();
        auth.limpar();
        expect(auth.autenticado()).toBeFalse();
    });
});
