import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of, Subject, throwError } from 'rxjs';
import { Admin } from './admin';
import { ApiService } from '../../core/api.service';
import { AuthService } from '../../core/auth.service';
import { AdminUsuario, AdminRecurso, AdminProfessor, AdminBloqueio } from '../../core/api.models';

describe('Administração: validação, persistência e revogação de sessão', () => {
  let c: Admin;
  let api: jasmine.SpyObj<ApiService>;
  let auth: {usuario: jasmine.Spy; sair: jasmine.Spy};
  const usuario: AdminUsuario = {id:2,nome:'Ana',email:'ana@aloca.edu.br',unidade:'Centro',perfis:['PROFESSOR'],ativo:true};
  const recurso: AdminRecurso = {id:3,nome:'Sala',tipo:'SALA',categoria:null,localizacao:'Centro',capacidade:20,restrito:false,ativo:true,abertura:'07:00:00',fechamento:'22:00:00'};
  const professor: AdminProfessor = {id:4,nome:'Bia',competencia:'Java',ativo:true};
  const bloqueio: AdminBloqueio = {id:5,recursoId:3,recurso:'Sala',inicio:'2030-01-01T09:00',fim:'2030-01-01T10:00',motivo:'Reparo',criadoPor:null};
  beforeEach(() => {
    api = jasmine.createSpyObj('ApiService',['adminUsuarios','adminRecursos','adminProfessores','adminBloqueios','criarUsuario','editarUsuario','ativarUsuario','senhaUsuario','criarRecurso','editarRecurso','alterarAtivoRecurso','criarProfessor','editarProfessor','alterarAtivoProfessor','criarBloqueio','removerBloqueio']);
    for (const nome of ['adminUsuarios','adminRecursos','adminProfessores','adminBloqueios'] as const) api[nome].and.returnValue(of([]));
    auth = {usuario:jasmine.createSpy().and.returnValue({id:1}),sair:jasmine.createSpy()};
    TestBed.configureTestingModule({imports:[Admin],providers:[provideRouter([]),{provide:ApiService,useValue:api},{provide:AuthService,useValue:auth}]});
    c = TestBed.createComponent(Admin).componentInstance;
  });
  it('carrega cadastros e apresenta falhas das quatro consultas', () => {
    api.adminUsuarios.and.returnValue(of([usuario]));api.adminRecursos.and.returnValue(of([recurso]));api.adminProfessores.and.returnValue(of([professor]));api.adminBloqueios.and.returnValue(of([bloqueio]));
    c.ngOnInit(); expect(c.usuarios()).toEqual([usuario]);expect(c.recursos()).toEqual([recurso]);expect(c.professores()).toEqual([professor]);expect(c.bloqueios()).toEqual([bloqueio]);
    for(const nome of ['adminUsuarios','adminRecursos','adminProfessores','adminBloqueios'] as const){api[nome].and.returnValue(throwError(()=>({error:{mensagem:'Sem acesso'}})));c.carregar();expect(c.erro()).toBe('Sem acesso');api[nome].and.returnValue(of([]));}
  });
  it('edita perfis sem alterar o objeto carregado e sem duplicatas', () => {
    c.editarUsuario(usuario);c.marcarPerfil('ADMINISTRADOR',true);c.marcarPerfil('ADMINISTRADOR',true);c.marcarPerfil('PROFESSOR',false);
    expect(c.usuario.perfis).toEqual(['ADMINISTRADOR']);expect(usuario.perfis).toEqual(['PROFESSOR']);c.limparUsuario();expect(c.editandoUsuario).toBeNull();expect(c.usuario.perfis).toEqual(['SOLICITANTE']);
  });
  for(const caso of ['nome','email','unidade','perfis','senha','senhaAusente'] as const) it('recusa cadastro de usuário inválido: '+caso,()=>{
    c.usuario={nome:'Ana',email:'ana@aloca.edu.br',unidade:'Centro',perfis:['PROFESSOR'],senha:'12345678'};
    if(caso==='perfis') c.usuario.perfis=[]; else if(caso==='senhaAusente') delete c.usuario.senha; else c.usuario[caso]='';
    c.salvarUsuario();expect(api.criarUsuario).not.toHaveBeenCalled();expect(c.erro()).toContain('Informe');
  });
  it('cria usuário, impede envio duplicado e libera formulário ao concluir',()=>{
    const resposta=new Subject<any>();api.criarUsuario.and.returnValue(resposta);c.usuario={nome:'Ana',email:'a@b.com',unidade:'Centro',perfis:['PROFESSOR'],senha:'12345678'};
    c.salvarUsuario();c.salvarUsuario();expect(api.criarUsuario).toHaveBeenCalledTimes(1);expect(c.enviando()).toBeTrue();resposta.next(usuario);resposta.complete();expect(c.enviando()).toBeFalse();expect(c.feedback()).toContain('Usuário salvo');expect(c.usuario.nome).toBe('');
  });
  for(const propria of [false,true]) it('edita usuário e encerra sessão apenas ao editar a própria conta: '+propria,()=>{
    api.editarUsuario.and.returnValue(of(usuario));auth.usuario.and.returnValue(propria?{id:2}:null);c.editarUsuario(usuario);c.salvarUsuario();expect(api.editarUsuario).toHaveBeenCalledWith(2,jasmine.objectContaining({email:usuario.email}));expect(auth.sair.calls.count()).toBe(propria?1:0);
  });
  it('expõe erro de usuário e permite tentar novamente',()=>{api.editarUsuario.and.returnValue(throwError(()=>({error:{mensagem:'E-mail duplicado'}})));c.editarUsuario(usuario);c.salvarUsuario();expect(c.erro()).toBe('E-mail duplicado');expect(c.enviando()).toBeFalse();});
  it('valida senha e impede envio sem conta ou em andamento',()=>{c.redefinirSenha();expect(api.senhaUsuario).not.toHaveBeenCalled();c.redefinindo=2;c.senhaNova='curta';c.redefinirSenha();expect(c.erro()).toContain('8');c.enviando.set(true);c.redefinirSenha();expect(api.senhaUsuario).not.toHaveBeenCalled();});
  for(const propria of [false,true]) it('redefine senha e revoga sessão própria: '+propria,()=>{auth.usuario.and.returnValue(propria?{id:2}:null);api.senhaUsuario.and.returnValue(of(void 0));c.redefinindo=2;c.senhaNova='12345678';c.redefinirSenha();expect(api.senhaUsuario).toHaveBeenCalledWith(2,'12345678');expect(c.senhaNova).toBe('');expect(c.redefinindo).toBeNull();expect(c.enviando()).toBeFalse();expect(auth.sair.calls.count()).toBe(propria?1:0);});
  it('falha na senha mantém dados para nova tentativa',()=>{api.senhaUsuario.and.returnValue(throwError(()=>({error:{mensagem:'Negado'}})));c.redefinindo=2;c.senhaNova='12345678';c.redefinirSenha();expect(c.erro()).toBe('Negado');expect(c.enviando()).toBeFalse();expect(c.redefinindo).toBe(2);});
  for(const alvo of ['Recurso','Professor','Usuario'] as const) it('ativa/desativa '+alvo+' e trata erro',()=>{
    const nome=alvo==='Usuario'?'ativarUsuario':alvo==='Recurso'?'alterarAtivoRecurso':'alterarAtivoProfessor'; const item=alvo==='Usuario'?usuario:alvo==='Recurso'?recurso:professor;
    api[nome].and.returnValue(of(item as any));(c['ativar'+alvo as keyof Admin] as Function).call(c,item);expect(api[nome]).toHaveBeenCalledWith(item.id,false);expect(c.feedback()).toContain('atualizada');
    api[nome].and.returnValue(throwError(()=>({error:{mensagem:'Não permitido'}})));(c['ativar'+alvo as keyof Admin] as Function).call(c,{...item,ativo:false});expect(api[nome]).toHaveBeenCalledWith(item.id,true);expect(c.erro()).toBe('Não permitido');
  });
  for(const caso of ['nome','localizacao','capacidade','fracao','horario'] as const) it('recusa recurso inválido: '+caso,()=>{c.editarRecurso(recurso);if(caso==='capacidade')c.recurso.capacidade=0;else if(caso==='fracao')c.recurso.capacidade=1.5;else if(caso==='horario')c.recurso.fechamento=c.recurso.abertura;else c.recurso[caso]=' ';c.salvarRecurso();expect(api.editarRecurso).not.toHaveBeenCalled();expect(c.erro()).toContain('Informe');});
  for(const editar of [false,true]) it('salva recurso e restaura formulário: '+editar,()=>{c.editarRecurso({...recurso,categoria:editar?'Sala':null});if(!editar)c.editandoRecurso=null;const req=editar?api.editarRecurso:api.criarRecurso;req.and.returnValue(of(recurso));c.salvarRecurso();expect(req).toHaveBeenCalled();expect(c.recurso.nome).toBe('');expect(c.editandoRecurso).toBeNull();expect(c.feedback()).toContain('sucesso');});
  it('recurso não duplica envio e trata falha',()=>{c.editarRecurso(recurso);c.enviando.set(true);c.salvarRecurso();expect(api.editarRecurso).not.toHaveBeenCalled();c.enviando.set(false);api.editarRecurso.and.returnValue(throwError(()=>({error:{mensagem:'Conflito'}})));c.salvarRecurso();expect(c.erro()).toBe('Conflito');expect(c.enviando()).toBeFalse();});
  it('valida nome e competência do professor',()=>{c.salvarProfessor();expect(api.criarProfessor).not.toHaveBeenCalled();c.professor.nome='Bia';c.salvarProfessor();expect(api.criarProfessor).not.toHaveBeenCalled();});
  for(const editar of [false,true]) it('salva professor: '+editar,()=>{c.editarProfessor(professor);if(!editar)c.editandoProfessor=null;const req=editar?api.editarProfessor:api.criarProfessor;req.and.returnValue(of(professor));c.salvarProfessor();expect(req).toHaveBeenCalled();expect(c.professor.nome).toBe('');expect(c.feedback()).toContain('sucesso');});
  it('professor não duplica envio e mantém edição após falha',()=>{c.editarProfessor(professor);c.enviando.set(true);c.salvarProfessor();expect(api.editarProfessor).not.toHaveBeenCalled();c.enviando.set(false);api.editarProfessor.and.returnValue(throwError(()=>({error:{mensagem:'Falha'}})));c.salvarProfessor();expect(c.erro()).toBe('Falha');expect(c.enviando()).toBeFalse();expect(c.editandoProfessor).toBe(4);});
  for(const caso of ['recurso','inicio','fim','ordem','motivo'] as const) it('recusa bloqueio inválido: '+caso,()=>{c.bloqueio={...bloqueio};if(caso==='recurso')c.bloqueio.recursoId=0;else if(caso==='ordem')c.bloqueio.fim=c.bloqueio.inicio;else c.bloqueio[caso]='';c.salvarBloqueio();expect(api.criarBloqueio).not.toHaveBeenCalled();expect(c.erro()).toContain('Informe');});
  it('salva bloqueio, impede duplicidade e restaura formulário',()=>{c.bloqueio={...bloqueio};c.enviando.set(true);c.salvarBloqueio();expect(api.criarBloqueio).not.toHaveBeenCalled();c.enviando.set(false);api.criarBloqueio.and.returnValue(of(bloqueio));c.salvarBloqueio();expect(api.criarBloqueio).toHaveBeenCalled();expect(c.bloqueio.recursoId).toBe(0);expect(c.feedback()).toBe('Bloqueio registrado.');});
  it('falha no bloqueio libera reenvio',()=>{c.bloqueio={...bloqueio};api.criarBloqueio.and.returnValue(throwError(()=>({error:{mensagem:'Período inválido'}})));c.salvarBloqueio();expect(c.erro()).toBe('Período inválido');expect(c.enviando()).toBeFalse();});
  it('só remove bloqueio após confirmação e apresenta falha',()=>{const confirmar=spyOn(window,'confirm').and.returnValue(false);c.removerBloqueio(bloqueio);expect(api.removerBloqueio).not.toHaveBeenCalled();confirmar.and.returnValue(true);api.removerBloqueio.and.returnValue(of(void 0));c.removerBloqueio(bloqueio);expect(api.removerBloqueio).toHaveBeenCalledWith(5);expect(c.feedback()).toBe('Bloqueio removido.');api.removerBloqueio.and.returnValue(throwError(()=>({error:{mensagem:'Negado'}})));c.removerBloqueio(bloqueio);expect(c.erro()).toBe('Negado');});
});
