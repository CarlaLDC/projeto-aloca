const { BeforeAll, AfterAll, Before, After, setDefaultTimeout } = require('@cucumber/cucumber');
const { chromium } = require('@playwright/test');
const fs = require('node:fs');
setDefaultTimeout(30000);
let browser;
BeforeAll(async function () {
  fs.mkdirSync('reports', { recursive: true });
  browser = await chromium.launch({ headless: process.env.HEADED !== 'true' });
});
Before(async function () {
  const baseUrl = process.env.BASE_URL || 'http://localhost:4200';
  this.context = await browser.newContext({ baseURL: baseUrl, locale: 'pt-BR', timezoneId: 'America/Sao_Paulo' });
  this.page = await this.context.newPage();
});
After(async function ({ result }) {
  try {
    if (result.status === 'FAILED') await this.attach(await this.page.screenshot({ fullPage: true }), 'image/png');
  } finally { await this.context.close(); }
});
AfterAll(async function () { if (browser) await browser.close(); });
