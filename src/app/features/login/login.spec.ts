import { TestBed } from '@angular/core/testing';
import { Login } from './login';

describe('Login', () => {
  it('shows required field errors after an empty submission', () => {
    const fixture = TestBed.createComponent(Login);
    fixture.detectChanges();
    fixture.nativeElement.querySelector('form').dispatchEvent(new Event('submit'));
    fixture.detectChanges();
    expect(fixture.nativeElement.querySelector('#email-error')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#password-error')).toBeTruthy();
    expect(fixture.componentInstance.feedback()).toBe('');
  });
  it('rejects an invalid email', () => {
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({ email: 'invalido', password: 'senha' });
    login.submit();
    expect(login.form.controls.email.invalid).toBeTrue();
    expect(login.feedback()).toBe('');
  });
  it('does not authenticate locally and clears the password after a valid submission', () => {
    const login = TestBed.createComponent(Login).componentInstance;
    login.form.setValue({ email: 'nome@instituicao.edu.br', password: 'senha' });
    login.submit();
    expect(login.feedback()).toContain('O acesso ainda não está disponível');
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
