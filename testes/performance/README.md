# Testes de desempenho e rastreabilidade — Projeto Aloca

> **Diagnóstico histórico:** as afirmações abaixo sobre ausência de backend, persistência, auditoria e testes referem-se à interface antiga desta branch. Para avaliar o sistema integrado, use a [RTM reconciliada](RTM.md), baseada na develop 3fb1f0d, e suas evidências. Este relatório não certifica a versão integrada; a execução de carga continua pendente.


Esta pasta reúne planos JMeter, RTM, matriz de auditoria, registro de defeitos e relatório.

## Estado de validação
- **Auditoria persistente (PR #31):** workflows de Backend, Angular, qualidade integrada, verificação funcional e BDD integrado concluíram com sucesso no commit atual da branch. O BDD executou API + Angular com banco H2 isolado. A PR continua aberta para revisão; não foi mesclada.
- **Auditoria frontend:** `DemoAuditLog` permanece um recurso de demonstração em memória; não substitui o registro persistente no backend.
- **JMeter SPA:** `jmeter/aloca-spa-load-test.jmx` mede entrega das rotas Angular na porta 4200; não mede regras de negócio da API.
- **JMeter API:** `jmeter/aloca-api-read-load-test.jmx` foi adicionado para login e consultas autenticadas somente de leitura na API Spring, com os quatro usuários de demonstração. Não cria, altera ou cancela reservas.

## Executar o teste da API localmente

Pré-requisitos: Apache JMeter 5.6.x, API em execução e banco com as contas/dados de demonstração ativos.

A senha é informada em tempo de execução e **não fica no Git**. No Windows PowerShell, na raiz do repositório:

```powershell
jmeter -n -t testes/performance/jmeter/aloca-api-read-load-test.jmx `
  -JbaseProtocol=http -JbaseHost=localhost -JbasePort=8080 -JbasePath=/api `
  -Jpassword='SENHA_LOCAL' `
  -JusersFile=testes/performance/jmeter/usuarios-demo.csv `
  -l resultados/aloca-api.jtl -e -o resultados/aloca-api-html
```

No Linux/macOS, use o mesmo comando em uma linha ou adapte a quebra de linha. Substitua `SENHA_LOCAL` localmente; não envie senha/token ao chat nem os adicione a arquivos versionados.

O plano usa 4 threads (uma por conta), ramp-up de 20 s, uma iteração por usuário e pausa de 300 ms entre consultas. Endpoints: `POST /auth/login`, `GET /auth/me`, `GET /painel`, `GET /disponibilidade`, `GET /reservas/opcoes`, `GET /reservas/minhas` e `GET /notificacoes`. O JWT é extraído de `$.token` conforme o contrato de `AuthController`.

Para uma rodada controlada maior, aumente threads/loops no JMeter gradualmente, respeitando a capacidade do ambiente. Não execute carga agressiva em produção. A massa de contas é fixa e não inclui senha.

## Métricas e relatório

Após a execução, abra `resultados/aloca-api-html/index.html` e consulte:
- média e mediana de latência;
- percentis p90/p95/p99;
- throughput (requisições/s);
- taxa de erro e códigos HTTP;
- tempo de conexão e bytes enviados/recebidos.

**Nenhum resultado de performance foi fabricado ou medido nesta sessão.** O runtime desta conversa não consegue acessar o `localhost` da sua máquina; por isso, a execução do JMeter e a coleta do JTL/HTML precisam ocorrer no computador onde a API está rodando. Anexe depois o JTL ou o relatório HTML exportado para análise e consolidação.

## Executar carga das rotas SPA

Com Apache JMeter 5.6.x:

```bash
mkdir -p resultados
jmeter -n -t testes/performance/jmeter/aloca-spa-load-test.jmx -Jprotocol=http -Jhost=localhost -Jport=4200 -l resultados/aloca-spa.jtl -e -o resultados/aloca-spa-html
```

Esse plano mede somente entrega HTTP/servidor estático/Angular dev server, não operações de negócio.

## Limites atuais
- Os fluxos mutáveis (criar/editar/cancelar reservas, aprovar/rejeitar, retirada/devolução) não entram no plano automatizado: precisam de política de limpeza e massa de estados controlados para evitar alteração destrutiva dos dados compartilhados.
- Não executar em produção sem autorização e janela aprovada.
