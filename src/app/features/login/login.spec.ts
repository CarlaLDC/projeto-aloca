import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router, provideRouter } from '@angular/router';
import { Login } from './login';

describe('Login', () => {
  let http: HttpTestingController;

  beforeEach(() => {
    localStorage.clear();
    TestBed.configureTestingModule({ providers: [provideRouter([]), provideHttpClient(), provideHttpClientTesting()] });
    http = TestBed.inject(HttpTestingController);
  });

  afterEach(() => http.verify());

  it('shows required field errors after an empty submission', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#email-error')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#password-error')).toBeTruthy();
    expect(fixture.componentInstance.feedback()).toBe('');
  });

  it('rejects an invalid email without calling the API', () => {
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({ email: 'invalido', password: 'senha' });
    login.submit();
    expect(login.form.controls.email.invalid).toBeTrue();
    http.expectNone('/api/auth/login');
  });

  it('signs in and opens the start page of the profile', () => {
    const router = TestBed.inject(Router);
    const navigate = spyOn(router, 'navigateByUrl').and.resolveTo(true);
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({ email: 'roberto.almeida@aloca.edu.br', password: 'aloca123' });
    login.submit();

    const req = http.expectOne('/api/auth/login');
    expect(req.request.body).toEqual({ email: 'roberto.almeida@aloca.edu.br', senha: 'aloca123' });
    req.flush({
      token: 't', expiraEm: new Date(Date.now() + 3600_000).toISOString(),
      usuario: { id: 3, nome: 'Roberto Almeida', email: 'r', unidade: 'U', perfis: ['RESPONSAVEL'], perfilRotulo: 'Responsável', iniciais: 'RA' }
    });
    http.expectOne('/api/notificacoes').flush({ naoLidas: 0, itens: [] });
    expect(navigate).toHaveBeenCalledWith('/aprovacoes');
  });

  it('shows the API message and clears the password when credentials are wrong', () => {
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({ email: 'nome@instituicao.edu.br', password: 'errada' });
    login.submit();
    http.expectOne('/api/auth/login').flush({ mensagem: 'E-mail ou senha incorretos.' }, { status: 401, statusText: 'Unauthorized' });
    expect(login.feedback()).toBe('E-mail ou senha incorretos.');
    expect(login.form.controls.password.value).toBe('');
  });

  it('provides institutional recovery guidance when clicked', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('.recovery').click();
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('[role="status"]').textContent).toContain('suporte da sua instituição');
  });
});
