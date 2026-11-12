const { BeforeAll, AfterAll, Before, After, setDefaultTimeout } = require('@cucumber/cucumber');
const { chromium } = require('@playwright/test');
const fs = require('node:fs');
setDefaultTimeout(60000);
let browser;
BeforeAll(async () => {
  fs.mkdirSync('bdd/reports', { recursive: true });
  browser = await chromium.launch({ headless: true });
});
Before(async function () {
  this.context = await browser.newContext({
    baseURL: process.env.BASE_URL || 'http://127.0.0.1:4200',
    locale: 'pt-BR', timezoneId: 'America/Sao_Paulo'
  });
  this.page = await this.context.newPage();
});
After(async function ({ result }) {
  try {
    if (result.status === 'FAILED') await this.attach(await this.page.screenshot({ fullPage: true }), 'image/png');
  } finally { await this.context.close(); }
});
AfterAll(async () => { if (browser) await browser.close(); });
