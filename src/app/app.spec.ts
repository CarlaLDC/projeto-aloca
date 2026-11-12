import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideRouter } from '@angular/router';
import { App } from './app';
import { routes } from './app.routes';
import { RouterTestingHarness } from '@angular/router/testing';

describe('Login route', () => {
  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [provideRouter(routes), provideHttpClient(), provideHttpClientTesting()]
    });
  });

  it('redirects the home page to login', async () => {
    const harness = await RouterTestingHarness.create('/');
    expect(harness.routeNativeElement?.querySelector('#login-heading')?.textContent).toBe('Entre na sua conta');
  });

  it('creates the application shell', () => {
    TestBed.configureTestingModule({ imports: [App] });
    expect(TestBed.createComponent(App).componentInstance).toBeTruthy();
  });
});
