import { Component, signal } from '@angular/core';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';

@Component({
  selector: 'app-login',
  imports: [ReactiveFormsModule],
  templateUrl: './login.html',
  styleUrl: './login.css'
})
export class Login {
  readonly form = new FormGroup({
    email: new FormControl('', { nonNullable: true, validators: [Validators.required, Validators.email] }),
    password: new FormControl('', { nonNullable: true, validators: [Validators.required] })
  });
  readonly feedback = signal('');

  submit(): void {
    this.feedback.set('');
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    // Authentication will be connected when the institutional API is available.
    this.feedback.set('O acesso ainda não está disponível. Aguarde a liberação pela instituição.');
    this.form.controls.password.reset();
  }

  recoverPassword(): void {
    this.feedback.set('Para recuperar sua senha, entre em contato com o suporte da sua instituição.');
  }
}
