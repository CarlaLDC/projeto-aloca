const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('@playwright/test');

function dataFutura(dias) {
  const d = new Date();
  d.setUTCDate(d.getUTCDate() + dias);
  return d.toISOString().slice(0, 10);
}

Given('que acesso a tela de login integrada', async function () {
  await this.page.goto('/login');
  await expect(this.page.getByRole('heading', { name: 'Entre na sua conta' })).toBeVisible();
});

When('entro com o usuário de teste {string}', async function (email) {
  await this.page.getByLabel('E-mail').fill(email);
  await this.page.getByLabel('Senha').fill(process.env.BDD_PASSWORD || 'aloca123');
  await this.page.getByRole('button', { name: 'Entrar', exact: true }).click();
  await expect.poll(() => new URL(this.page.url()).pathname).not.toBe('/login');
  this.token = await this.page.evaluate(() => JSON.parse(localStorage.getItem('aloca.sessao')).token);
  expect(this.token).toBeTruthy();
});

Then('vejo a página {string}', async function (titulo) {
  await expect(this.page.getByRole('heading', { name: titulo, exact: true })).toBeVisible();
});

When('acesso a disponibilidade', async function () {
  await this.page.goto('/disponibilidade');
  await expect(this.page.getByRole('heading', { name: 'Disponibilidade', exact: true })).toBeVisible();
});

Then('recebo recursos cadastrados pela API', async function () {
  const response = await this.page.request.get('/api/disponibilidade', { headers: { Authorization: 'Bearer ' + this.token } });
  expect(response.status()).toBe(200);
  const dados = await response.json();
  expect(dados.recursos.length).toBeGreaterThan(0);
  await expect(this.page.getByText(dados.recursos[0].nome, { exact: true }).first()).toBeVisible();
});

Then('a API de administração responde 403', async function () {
  const response = await this.page.request.get('/api/admin/recursos', { headers: { Authorization: 'Bearer ' + this.token } });
  expect(response.status()).toBe(403);
});

When('cadastro uma reserva futura pela interface', async function () {
  this.atividade = 'BDD integrado ' + Date.now();
  await this.page.goto('/nova-reserva');
  await expect(this.page.locator('select[name="room"] option').first()).toBeAttached();
  await this.page.locator('[name="activity"]').fill(this.atividade);
  await this.page.getByLabel('Data', { exact: true }).fill(dataFutura(50));
  await this.page.getByLabel('Início', { exact: true }).fill('13:00');
  await this.page.getByLabel('Término', { exact: true }).fill('14:00');
  await this.page.getByRole('button', { name: 'Revisar e solicitar' }).click();
  await expect(this.page.getByRole('heading', { name: 'Revise sua reserva' })).toBeVisible();
  await this.page.getByRole('button', { name: 'Confirmar solicitação' }).click();
});

Then('vejo o código de reserva registrado', async function () {
  await expect(this.page.getByRole('heading', { name: 'Solicitação registrada' })).toBeVisible();
  const text = await this.page.locator('.demo-note strong').textContent();
  this.codigo = text?.replace('#', '').trim();
  expect(this.codigo).toMatch(/^RES-\d+$/);
});

Then('a reserva está persistida e disponível na API', async function () {
  const response = await this.page.request.get('/api/reservas/' + this.codigo, { headers: { Authorization: 'Bearer ' + this.token } });
  expect(response.status()).toBe(200);
  const dados = await response.json();
  expect(dados.atividade).toBe(this.atividade);
  expect(dados.codigo).toBe(this.codigo);
});

When('crio duas reservas para a mesma sala e horário pela API', async function () {
  const headers = { Authorization: 'Bearer ' + this.token };
  const opcoes = await this.page.request.get('/api/reservas/opcoes', { headers });
  expect(opcoes.status()).toBe(200);
  const dados = await opcoes.json();
  const sala = dados.espacos.find(r => !r.restrito);
  expect(sala).toBeTruthy();
  const pedido = {
    atividade: 'BDD concorrência ' + Date.now(), data: dataFutura(80),
    inicio: '15:00', fim: '16:00', espacoId: sala.id, professorId: null, materialIds: []
  };
  const primeira = await this.page.request.post('/api/reservas', { headers, data: pedido });
  expect(primeira.status()).toBe(201);
  this.segundaResposta = await this.page.request.post('/api/reservas', { headers, data: { ...pedido, atividade: 'BDD conflito' } });
});

Then('a segunda reserva recebe conflito HTTP 409', async function () {
  expect(this.segundaResposta.status()).toBe(409);
  const erro = await this.segundaResposta.json();
  expect(erro.conflitos.length).toBeGreaterThan(0);
});
