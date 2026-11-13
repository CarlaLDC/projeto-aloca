import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { ApiService } from './api.service';

describe('ApiService disponibilidade', () => {
  it('envia capacidade mínima com a data para o servidor', () => {
    TestBed.configureTestingModule({providers: [provideHttpClient(), provideHttpClientTesting()]});
    const http = TestBed.inject(HttpTestingController);
    TestBed.inject(ApiService).disponibilidade('2030-01-15', undefined, undefined, 30).subscribe();
    const req = http.expectOne(r => r.url === '/api/disponibilidade');
    expect(req.request.params.get('capacidadeMinima')).toBe('30');
    expect(req.request.params.get('data')).toBe('2030-01-15');
    req.flush({}); http.verify();
  });
});
