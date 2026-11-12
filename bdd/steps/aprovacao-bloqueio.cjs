const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('@playwright/test');
const { randomUUID } = require('node:crypto');

function headers(token) { return { Authorization: 'Bearer ' + token }; }
async function login(world, email) {
  const response = await world.page.request.post('/api/auth/login', {
    data: { email, senha: process.env.BDD_PASSWORD || 'aloca123' }
  });
  expect(response.status()).toBe(200);
  return (await response.json()).token;
}
async function detalhe(world) {
  const response = await world.page.request.get('/api/reservas/' + world.codigoRestrito, {
    headers: headers(world.tokensRegra.solicitante)
  });
  expect(response.status()).toBe(200);
  return response.json();
}
Given('que preparo um recurso restrito exclusivo para o cenário', async function () {
  this.tokensRegra = {};
  for (const [perfil, email] of Object.entries({
    solicitante: 'ana.martins@aloca.edu.br',
    responsavel: 'roberto.almeida@aloca.edu.br',
    administrador: 'patricia.souza@aloca.edu.br'
  })) this.tokensRegra[perfil] = await login(this, email);
  this.atividadeRegra = 'BDD aprovação/bloqueio ' + randomUUID();
  const futuro = new Date();
  futuro.setUTCDate(futuro.getUTCDate() + 120);
  this.diaRegra = futuro.toISOString().slice(0, 10);
  const response = await this.page.request.post('/api/admin/recursos', {
    headers: headers(this.tokensRegra.administrador),
    data: { nome: 'Sala BDD ' + randomUUID(), tipo: 'SALA', localizacao: 'Ambiente de testes',
      capacidade: 20, restrito: true, abertura: '07:00', fechamento: '22:00' }
  });
  expect(response.status()).toBe(201);
  this.recursoRegra = (await response.json()).id;
  this.pedidoRegra = { atividade: this.atividadeRegra, data: this.diaRegra,
    inicio: '10:00', fim: '11:00', espacoId: this.recursoRegra,
    professorId: null, materialIds: [] };
});
Given('que existe uma solicitação pendente para o recurso exclusivo', async function () {
  const response = await this.page.request.post('/api/reservas', {
    headers: headers(this.tokensRegra.solicitante), data: this.pedidoRegra
  });
  expect(response.status()).toBe(201);
  const reserva = await response.json();
  expect(reserva.status).toBe('SOLICITADA');
  expect(reserva.exigeAprovacao).toBe(true);
  this.codigoRestrito = reserva.codigo;
  this.historicoAnteriorRegra = (await detalhe(this)).historico;
});
Given('que o administrador bloqueia o recurso exclusivo no período solicitado', async function () {
  const response = await this.page.request.post('/api/admin/bloqueios', {
    headers: headers(this.tokensRegra.administrador),
    data: { recursoId: this.recursoRegra, inicio: this.diaRegra + 'T09:00:00',
      fim: this.diaRegra + 'T12:00:00', motivo: 'Manutenção BDD' }
  });
  expect(response.status()).toBe(201);
});
When('o responsável aprova a solicitação exclusiva', async function () {
  this.respostaRegra = await this.page.request.post('/api/aprovacoes/' + this.codigoRestrito + '/aprovar', {
    headers: headers(this.tokensRegra.responsavel)
  });
});
When('o perfil {string} tenta aprovar a solicitação exclusiva', async function (perfil) {
  expect(this.tokensRegra[perfil]).toBeTruthy();
  this.respostaRegra = await this.page.request.post('/api/aprovacoes/' + this.codigoRestrito + '/aprovar', {
    headers: headers(this.tokensRegra[perfil])
  });
});
Then('a solicitação exclusiva fica aprovada com histórico da decisão', async function () {
  expect(this.respostaRegra.status()).toBe(200);
  const reserva = await detalhe(this);
  expect(reserva.status).toBe('APROVADA');
  expect(reserva.historico).toEqual(expect.arrayContaining([
    expect.objectContaining({ estadoAnterior: 'SOLICITADA', estadoNovo: 'APROVADA', ator: 'Roberto Almeida' })
  ]));
});
Then('a aprovação é negada e a solicitação permanece pendente', async function () {
  expect(this.respostaRegra.status()).toBe(403);
  const reserva = await detalhe(this);
  expect(reserva.status).toBe('SOLICITADA');
  expect(reserva.historico).toEqual(this.historicoAnteriorRegra);
});
When('o solicitante tenta reservar o recurso exclusivo', async function () {
  this.respostaRegra = await this.page.request.post('/api/reservas', {
    headers: headers(this.tokensRegra.solicitante), data: this.pedidoRegra
  });
});
Then('a criação é recusada por manutenção e não há reserva persistida', async function () {
  expect(this.respostaRegra.status()).toBe(409);
  expect((await this.respostaRegra.json()).conflitos).toEqual(expect.arrayContaining([
    expect.objectContaining({ tipo: 'Manutenção' })
  ]));
  const response = await this.page.request.get('/api/reservas/minhas', {
    headers: headers(this.tokensRegra.solicitante)
  });
  expect(response.status()).toBe(200);
  expect((await response.json()).filter(r => r.atividade === this.atividadeRegra)).toHaveLength(0);
});
Then('a aprovação é recusada por manutenção e preserva o histórico anterior', async function () {
  expect(this.respostaRegra.status()).toBe(409);
  expect((await this.respostaRegra.json()).mensagem).toMatch(/manutenção/i);
  const reserva = await detalhe(this);
  expect(reserva.status).toBe('SOLICITADA');
  expect(reserva.historico).toEqual(this.historicoAnteriorRegra);
});
