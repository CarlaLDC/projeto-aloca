const { chromium, expect } = require('@playwright/test');
const fs = require('fs');
const path = require('path');
const out = process.env.EVIDENCE_DIR || 'evidencias-candidato';
fs.mkdirSync(out, { recursive: true });
const resultados = [];
(async () => {
  const browser = await chromium.launch({ headless: true, args: ['--no-sandbox'] });
  try {
    for (const width of [360, 1280]) {
      const context = await browser.newContext({ viewport: { width, height: 900 }, locale: 'pt-BR' });
      const page = await context.newPage();
      const problemas=[];
      page.on('pageerror', e => problemas.push(e.message));
      async function foto(etapa) {
        await page.screenshot({ path: path.join(out, `${width}-${etapa}.png`), fullPage: true });
        const medida = await page.evaluate(() => ({ viewport:innerWidth, conteudo:document.documentElement.scrollWidth }));
        expect(medida.conteudo, `overflow horizontal em ${etapa}`).toBeLessThanOrEqual(width+1);
        resultados.push({ width, etapa, status:'APROVADO', ...medida });
      }
      await page.goto('http://127.0.0.1:4200/login');
      await page.getByLabel('E-mail', {exact:true}).fill('andre.lima@aloca.edu.br');
      await page.getByLabel('Senha', {exact:true}).fill('aloca123');
      await page.getByRole('button',{name:'Entrar',exact:true}).click();
      await expect(page.getByRole('heading',{name:'Visão geral',exact:true})).toBeVisible();
      await page.goto('http://127.0.0.1:4200/disponibilidade');
      await expect(page.getByRole('heading',{name:'Disponibilidade',exact:true})).toBeVisible();
      const request=page.waitForResponse(r=>r.url().includes('/api/disponibilidade')&&r.url().includes('capacidadeMinima=30'));
      await page.getByLabel('Capacidade mínima',{exact:true}).fill('30');
      const response=await request;expect(response.status()).toBe(200);
      const dados=await response.json();expect(dados.recursos.every(r=>r.capacidade>=30)).toBe(true);
      await page.getByLabel('Buscar recurso',{exact:true}).fill('Sala');
      await expect(page.locator('.resource-card').first()).toBeVisible();
      await foto('pesquisa');
      await page.goto('http://127.0.0.1:4200/nova-reserva');
      await expect(page.locator('select[name="room"] option').first()).toBeAttached();
      const token=await page.evaluate(()=>JSON.parse(localStorage.getItem('aloca.sessao')).token);
      const op=await page.request.get('http://127.0.0.1:4200/api/reservas/opcoes',{headers:{Authorization:'Bearer '+token}});
      const sala=(await op.json()).espacos.find(s=>!s.restrito);expect(sala).toBeTruthy();
      const dia=new Date();dia.setDate(dia.getDate()+220+(width===360?1:2));
      await page.getByLabel('Atividade',{exact:true}).fill('Responsividade professor '+width);
      await page.getByLabel('Data',{exact:true}).fill(dia.toISOString().slice(0,10));
      await page.getByLabel('Início',{exact:true}).fill('13:00');
      await page.getByLabel('Término',{exact:true}).fill('14:00');
      await page.locator('select[name="room"]').selectOption({label:sala.rotulo});
      await page.locator('select[name="teacher"]').selectOption({label:'Sem professor'});
      await page.locator('select[name="material"]').selectOption({label:'Sem materiais'});
      await foto('formulario-reserva');
      await page.getByRole('button',{name:'Revisar e solicitar',exact:true}).click();
      await page.getByRole('button',{name:'Confirmar solicitação',exact:true}).click();
      await expect(page.getByRole('heading',{name:'Solicitação registrada',exact:true})).toBeVisible();
      await foto('reserva-confirmada');
      await page.getByRole('link',{name:'Ver reserva',exact:true}).click();
      await page.getByRole('button',{name:'Cancelar reserva',exact:true}).click();
      await page.getByLabel('Motivo do cancelamento',{exact:true}).fill('Validação de cancelamento em '+width+'px');
      await foto('dialogo-cancelamento');
      const cancel=page.waitForResponse(r=>r.url().endsWith('/cancelamento')&&r.request().method()==='POST');
      await page.getByRole('button',{name:'Confirmar cancelamento',exact:true}).click();
      expect((await cancel).status()).toBe(200);
      await expect(page.locator('.status').first()).toHaveText('Cancelada');
      await foto('cancelamento-confirmado');
      expect(problemas).toEqual([]);
      // Gestão de usuários pela interface, no mesmo viewport.
      await page.evaluate(() => localStorage.clear());
      await page.goto('http://127.0.0.1:4200/login');
      await page.getByLabel('E-mail', {exact:true}).fill('patricia.souza@aloca.edu.br');
      await page.getByLabel('Senha', {exact:true}).fill('aloca123');
      await page.getByRole('button',{name:'Entrar',exact:true}).click();
      await expect(page.getByRole('heading',{name:'Administração',exact:true})).toBeVisible();
      await page.getByRole('button',{name:'Usuários e perfis',exact:true}).click();
      const email=`responsivo-${width}-${Date.now()}@aloca.edu.br`;
      await page.getByLabel('Nome do usuário',{exact:true}).fill('Usuário responsivo '+width);
      await page.getByLabel('E-mail do usuário',{exact:true}).fill(email);
      await page.getByLabel('Unidade do usuário',{exact:true}).fill('Teste');
      await page.getByLabel('Senha inicial',{exact:true}).fill('Candidato-12345');
      await page.getByRole('checkbox',{name:'PROFESSOR',exact:true}).check();
      await page.getByRole('checkbox',{name:'SOLICITANTE',exact:true}).uncheck();
      await page.getByRole('button',{name:'Salvar usuário',exact:true}).click();
      const row=page.getByRole('row').filter({hasText:email});
      await expect(row).toBeVisible();
      await expect(row).toContainText('PROFESSOR');
      await row.getByRole('button',{name:'Editar',exact:true}).click();
      await page.getByLabel('Nome do usuário',{exact:true}).fill('Usuário editado '+width);
      await page.getByRole('button',{name:'Salvar usuário',exact:true}).click();
      await expect(row).toContainText('Usuário editado '+width);
      await row.getByRole('button',{name:'Desativar',exact:true}).click();
      await expect(row).toContainText('Inativo');
      await row.getByRole('button',{name:'Ativar',exact:true}).click();
      await expect(row.getByRole('button',{name:'Desativar',exact:true})).toBeEnabled();
      await foto('usuarios-perfis');
      await context.close();
    }
  } finally {
    fs.writeFileSync(path.join(out,'responsividade.json'),JSON.stringify(resultados,null,2));
    await browser.close();
  }
})().catch(e=>{ console.error(e); process.exitCode=1; });
