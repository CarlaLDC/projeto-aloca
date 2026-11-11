import { Component, inject, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import { AuthService } from '../../core/auth.service';
import { mensagemErro } from '../../core/formato';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);

  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });
  readonly feedback = signal(
    inject(ActivatedRoute).snapshot.queryParamMap.has('expirada') ? 'Sua sessão expirou. Entre novamente.' : ''
  );
  readonly sending = signal(false);

  submit(): void {
    this.feedback.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const { email, password } = this.form.getRawValue();
    this.sending.set(true);
    this.auth.entrar(email.trim(), password).subscribe({
      next: () => {
        this.sending.set(false);
        void this.router.navigateByUrl(this.auth.telaInicial());
      },
      error: erro => {
        this.sending.set(false);
        this.feedback.set(mensagemErro(erro, 'Não foi possível entrar. Tente novamente.'));
        this.form.controls.password.reset();
      }
    });
  }

  recoverPassword(): void {
    this.feedback.set('Para recuperar sua senha, entre em contato com o suporte da sua instituição.');
  }
}
