const { Given, When, Then } = require('@cucumber/cucumber');
const { expect } = require('@playwright/test');

Given('que acesso {string}', async function (path) { await this.page.goto(path); });
When('clico em {string}', async function (name) { await this.page.getByRole('button', { name, exact: true }).click(); });
When('preencho {string} com {string}', async function (label, value) { await this.page.getByLabel(label, { exact: true }).fill(value); });
Then('vejo o texto {string}', async function (text) { await expect(this.page.getByText(text, { exact: true })).toBeVisible(); });
Then('vejo o título {string}', async function (name) { await expect(this.page.getByRole('heading', { name, exact: true })).toBeVisible(); });
Then('não vejo o título {string}', async function (name) { await expect(this.page.getByRole('heading', { name, exact: true })).toHaveCount(0); });
Then('permaneço em {string}', async function (path) { await expect.poll(() => new URL(this.page.url()).pathname).toBe(path); });
Then('o campo {string} está vazio', async function (label) { await expect(this.page.getByLabel(label, { exact: true })).toHaveValue(''); });
Then('o campo {string} contém {string}', async function (label, value) { await expect(this.page.getByLabel(label, { exact: true })).toHaveValue(value); });
Then('o botão {string} está desabilitado', async function (name) { await expect(this.page.getByRole('button', { name, exact: true })).toBeDisabled(); });
Then('o histórico contém {string}', async function (text) { await expect(this.page.getByRole('table', { name: 'Histórico da reserva' })).toContainText(text); });
Then('o diálogo permanece aberto', async function () { await expect(this.page.getByRole('dialog')).toBeVisible(); });
