import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { RouterTestingHarness } from '@angular/router/testing';

describe('Login route', () => {
  it('redirects the home page to login', async () => {
    TestBed.configureTestingModule({ providers: [provideRouter(routes)] });
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.querySelector('h2')?.textContent).toBe('Entre na sua conta');
  });
  it('creates the application shell', () => {
    TestBed.configureTestingModule({ imports: [App], providers: [provideRouter(routes)] });
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });
});
