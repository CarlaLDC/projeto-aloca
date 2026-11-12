const { Before, After, Given, When, Then, setDefaultTimeout } = require('@cucumber/cucumber');
const { request, expect } = require('@playwright/test');
const { randomUUID } = require('node:crypto');
setDefaultTimeout(60000);
Before({ tags: '@aprovacao-inativos' }, async function () {
  this.apiInativos = await request.newContext({ baseURL: process.env.API_URL || 'http://127.0.0.1:8080' });
});
After({ tags: '@aprovacao-inativos' }, async function () { await this.apiInativos?.dispose(); });
function h(token) { return token ? { Authorization: 'Bearer ' + token } : {}; }
async function post(world, token, path, data, status) {
  const response = await world.apiInativos.post(path, { headers: h(token), data });
  expect(response.status(), path + ': ' + await response.text()).toBe(status);
  return response.json();
}
async function detalhe(world) {
  const response = await world.apiInativos.get('/api/reservas/' + world.reservaInativos.codigo, { headers: h(world.anaInativos) });
  expect(response.status()).toBe(200);
  return response.json();
}
async function pendente(world) {
  const response = await world.apiInativos.get('/api/aprovacoes', { headers: h(world.responsavelInativos) });
  expect(response.status()).toBe(200);
  const item = (await response.json()).solicitacoes.find(r => r.codigo === world.reservaInativos.codigo);
  expect(item).toBeTruthy();
  return item;
}
Given('uma solicitação restrita com sala, material e professor ativos', async function () {
  const login = async email => (await post(this, null, '/api/auth/login',
    { email, senha: process.env.BDD_PASSWORD || 'aloca123' }, 200)).token;
  this.anaInativos = await login('ana.martins@aloca.edu.br');
  this.adminInativos = await login('patricia.souza@aloca.edu.br');
  this.responsavelInativos = await login('roberto.almeida@aloca.edu.br');
  const criar = async (tipo, restrito) => post(this, this.adminInativos, '/api/admin/recursos',
    { nome: 'BDD ' + tipo + ' ' + randomUUID(), tipo, restrito, localizacao: 'Teste isolado',
      capacidade: 20, abertura: '07:00', fechamento: '22:00' }, 201);
  const sala = await criar('SALA', true);
  const material = await criar('EQUIPAMENTO', false);
  const professor = await post(this, this.adminInativos, '/api/admin/professores',
    { nome: 'Docente BDD ' + randomUUID(), competencia: 'BDD' }, 201);
  this.cadastrosInativos = {
    sala: '/api/admin/recursos/' + sala.id,
    material: '/api/admin/recursos/' + material.id,
    professor: '/api/admin/professores/' + professor.id
  };
  this.nomesInativos = { sala: sala.nome, material: material.nome, professor: professor.nome };
  const futuro = new Date();
  futuro.setUTCDate(futuro.getUTCDate() + 130);
  this.reservaInativos = await post(this, this.anaInativos, '/api/reservas', {
    atividade: 'BDD revalidação ' + randomUUID(), data: futuro.toISOString().slice(0, 10),
    inicio: '10:00', fim: '11:00', espacoId: sala.id, professorId: professor.id, materialIds: [material.id]
  }, 201);
  expect(this.reservaInativos.status).toBe('SOLICITADA');
  this.auditoriaInativos = (await detalhe(this)).historico;
});
When('o administrador desativa o {string} da solicitação', async function (alvo) {
  this.alvoInativos = alvo;
  const cadastro = await post(this, this.adminInativos, this.cadastrosInativos[alvo] + '/desativar', undefined, 200);
  expect(cadastro.ativo).toBe(false);
});
Then('o painel de aprovações informa a desativação do {string}', async function (alvo) {
  const item = await pendente(this);
  expect(item.disponibilidadeValidada).toBe(false);
  expect(item.problemas.join(' ')).toContain(this.nomesInativos[alvo]);
  expect(item.problemas.join(' ')).toMatch(/desativado/i);
});
When('o responsável tenta aprovar a solicitação com cadastro desativado', async function () {
  this.respostaInativos = await this.apiInativos.post('/api/aprovacoes/' + this.reservaInativos.codigo + '/aprovar',
    { headers: h(this.responsavelInativos) });
});
Then('a API recusa com 409 e preserva a solicitação e sua auditoria', async function () {
  expect(this.respostaInativos.status()).toBe(409);
  const erro = await this.respostaInativos.json();
  expect(erro.mensagem).toContain(this.nomesInativos[this.alvoInativos]);
  expect(erro.mensagem).toMatch(/desativado/i);
  const reserva = await detalhe(this);
  expect(reserva.status).toBe('SOLICITADA');
  expect(reserva.historico).toEqual(this.auditoriaInativos);
});
When('o administrador reativa o {string} da solicitação', async function (alvo) {
  const cadastro = await post(this, this.adminInativos, this.cadastrosInativos[alvo] + '/ativar', undefined, 200);
  expect(cadastro.ativo).toBe(true);
});
Then('o painel informa que a solicitação pode ser aprovada', async function () {
  const item = await pendente(this);
  expect(item.disponibilidadeValidada).toBe(true);
  expect(item.problemas).toHaveLength(0);
});
When('o responsável aprova a solicitação revalidada', async function () {
  await post(this, this.responsavelInativos, '/api/aprovacoes/' + this.reservaInativos.codigo + '/aprovar', undefined, 200);
});
Then('a reserva fica aprovada com auditoria da decisão', async function () {
  const reserva = await detalhe(this);
  expect(reserva.status).toBe('APROVADA');
  expect(reserva.historico).toEqual(expect.arrayContaining([
    expect.objectContaining({ estadoAnterior: 'Solicitada', estadoNovo: 'Aprovada', ator: 'Roberto Almeida' })
  ]));
});
