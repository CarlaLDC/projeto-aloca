import { TestBed } from '@angular/core/testing';
import { HttpClient, HttpErrorResponse, provideHttpClient, withInterceptors } from '@angular/common/http';
import { HttpTestingController, provideHttpClientTesting } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { authInterceptor } from './auth.interceptor';

describe('Interceptor de autenticação', () => {
  let http: HttpTestingController;
  let auth: {token:string|null; limpar:jasmine.Spy};
  let router: jasmine.SpyObj<Router>;
  beforeEach(() => {
    auth = {token:'jwt', limpar:jasmine.createSpy('limpar')}; router = jasmine.createSpyObj('Router',['navigate']);
    TestBed.configureTestingModule({providers:[provideHttpClient(withInterceptors([authInterceptor])),provideHttpClientTesting(),{provide:AuthService,useValue:auth},{provide:Router,useValue:router}]});
    http = TestBed.inject(HttpTestingController);
  });
  afterEach(() => http.verify());
  for (const url of ['/api/reservas','/api/auth/login','https://externo.test/dados']) {
    it('envia token apenas à API protegida: '+url, () => {
      TestBed.inject(HttpClient).get(url).subscribe(); const req = http.expectOne(url);
      expect(req.request.headers.get('Authorization')).toBe(url==='/api/reservas'?'Bearer jwt':null); req.flush({});
    });
  }
  it('sem token não adiciona Authorization', () => {auth.token=null; TestBed.inject(HttpClient).get('/api/reservas').subscribe(); const req=http.expectOne('/api/reservas'); expect(req.request.headers.has('Authorization')).toBeFalse();req.flush({});});
  for (const [url,status,limpa] of [['/api/reservas',401,true],['/api/auth/login',401,false],['/api/reservas',403,false]] as const) {
    it('preserva erro HTTP e encerra sessão somente quando expirada: '+url+status, () => {
      let erro: HttpErrorResponse | undefined;
      TestBed.inject(HttpClient).get(url).subscribe({error:e=>erro=e}); http.expectOne(url).flush({mensagem:'Negado'},{status,statusText:'Negado'});
      expect(erro?.status).toBe(status); expect(auth.limpar.calls.count()).toBe(limpa?1:0);
      if(limpa) expect(router.navigate).toHaveBeenCalledWith(['/login'],{queryParams:{expirada:1}}); else expect(router.navigate).not.toHaveBeenCalled();
    });
  }
});
