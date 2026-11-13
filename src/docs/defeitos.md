# Registro de defeitos, correções e regressões

Data da consolidação: 12/11/2026. Base inspecionada: develop `611c9402c458d395126f014e720fc45a27ceeef8`.

Este registro consolida ocorrências comprovadas no histórico e reavalia impedimentos antigos. Não é uma afirmação de ausência de defeitos nem uma reconstrução de datas de descoberta: a classificação e a triagem foram feitas nesta consolidação. Os autores e horários originais permanecem nos commits vinculados. A equipe mantém a responsabilidade pela revisão no PR e pela atribuição de novos registros.

## Critérios de triagem

| Severidade (impacto) | Critério |
|---|---|
| S1 — crítica | Dupla reserva, acesso indevido ou perda/corrupção de dados |
| S2 — alta | Regra essencial inválida ou impedimento de entrega sem alternativa adequada |
| S3 — média | Falha parcial ou automação que produz resultado incorreto, com alternativa |
| S4 — baixa | Apresentação/documentação sem impacto na decisão funcional |

| Prioridade (ordem de atendimento) | Critério |
|---|---|
| P0 | Interromper entrega e corrigir imediatamente |
| P1 | Resolver antes do próximo candidato à entrega |
| P2 | Resolver no próximo ciclo de qualidade |
| P3 | Melhoria planejada |

Estados: Aberto → Triado → Em correção → Corrigido → Validado. Reabrir se a regressão falhar. “Corrigido” exige commit; “Validado” exige resultado identificável no ambiente informado. Um impedimento pode ser superado parcialmente, sem certificar execução de carga. Falhas de automação não contam como bugs do produto.

## Inventário priorizado

| ID | Natureza / requisitos do enunciado | Severidade | Prioridade | Situação nesta base |
|---|---|---|---|---|
| DEF-001 | Produto: revalidação de cadastro na aprovação; RF-02/07/10 | S2 | P1 | Validado historicamente em H2; correção integrada via PR #34 |
| AUT-001 | Automação: Bearer null no login BDD | S3 | P1 | Validado historicamente no ciclo BDD |
| AUT-002 | Automação: contagem global no teste de ciclo; RF-10 | S3 | P2 | Corrigido e suíte posterior aprovada |
| NC-001 | Conformidade: Spring Boot 4.1.2 versus 3.x exigido | S2 | P1 | Aberto; não alterar framework neste PR documental |
| IMP-003 | Evidência: resultado JMeter não consolidado em develop | S2 | P1 | Encerrado: PE-01/PE-02/PE-03 executados no PostgreSQL, evidências em `testes/carga/resultados/20261113-campanha-03` ([relatório](qualidade/RELATORIO-CARGA.md)) |
| DEF-PERF-001 | Produto: 409 de disputa por professor/material leva ~0,9 s (P95 1,6 s) por varrer todos os espaços nas alternativas sob trava; RNF-01 | S3 | P2 | Validado: P95 do 409 caiu de ~1,2 s para 90 ms na campanha 03 |
| DEF-INT-001 | Produto: em 4xx/5xx do serviço de notificação, o log gravava o corpo da resposta externa e o e-mail do destinatário; EX-03, RNF-03 | S3 | P1 | Validado: testes WireMock ([relatório](qualidade/RELATORIO-WIREMOCK.md)) |
| DEF-INT-002 | Produto: resposta corrompida do serviço de notificação gerava `IllegalArgumentException` fora do tratamento de erro; EX-04 | S3 | P1 | Validado: testes WireMock ([relatório](qualidade/RELATORIO-WIREMOCK.md)) |
| DEF-INT-003 | Produto: notificação sem destinatário era enviada ao serviço externo; EX-04 | S4 | P2 | Validado: testes WireMock ([relatório](qualidade/RELATORIO-WIREMOCK.md)) |

## DEF-001 — solicitação com cadastro desativado continuava válida no painel

- **Ambiente anterior:** API real, H2 descartável, Java 21/Node 22; head `cbf4878`. Origem: ciclo BDD do [PR #34](https://github.com/CarlaLDC/projeto-aloca/pull/34).
- **Reprodução:** autenticar solicitante, responsável e administrador; criar solicitação restrita com cadastros ativos; desativar sala, material ou professor; consultar aprovações.
- **Esperado:** painel indicar impedimento; aprovação responder 409 sem mudar estado/histórico; reativação permitir aprovação auditada.
- **Obtido comprovado antes:** `disponibilidadeValidada=true` após desativação, em três cenários. A [execução de falha](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43326822779) parou nessa asserção: ela não comprova execução da tentativa de aprovação.
- **Causa:** `ReservaService.problemas` verificava colisões/manutenção, mas não o estado ativo dos cadastros na decisão.
- **Correção:** [f4b2777](https://github.com/CarlaLDC/projeto-aloca/commit/f4b27779d4ec995db2839d0aa51383fbff85a9b2), revalidando sala, materiais e professor na função compartilhada por painel e aprovação.
- **Regressão:** [feature](../../bdd/features/aprovacao-inativos.feature), três exemplos (sala/material/professor), incluindo recusa 409, preservação de auditoria e reativação.
- **Validação:** [execução aprovada](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43327149247), 3 cenários/27 passos; [JSON antes](../../bdd/evidencias/aprovacao-inativos/antes.json) e [depois](../../bdd/evidencias/aprovacao-inativos/depois.json). [Linha do tempo completa](../../bdd/EVIDENCIAS-APROVACAO-INATIVOS.md).
- **Prevenção:** revalidar condições mutáveis no momento da decisão; manter cenários no CI.
- **Limite:** H2 e desativação concluída antes da aprovação; não prova atomicidade com desativação simultânea nem desempenho MySQL.

## AUT-001 — preparação BDD enviava Bearer null

- **Reprodução/ambiente:** executar os cenários de inativos no head `b061bf8`; o helper construía Authorization mesmo sem token no login.
- **Esperado:** login sem Authorization; obtido: falha na preparação, registrada na [execução 43326687096](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43326687096), conforme a linha do tempo BDD.
- **Causa/correção:** helper incondicional; [cbf4878](https://github.com/CarlaLDC/projeto-aloca/commit/cbf4878b07f9a4b9a2e392c2f417e82fce427727) passou a omitir o cabeçalho sem token.
- **Regressão:** hooks de autenticação em `bdd/steps/aprovacao-inativos.cjs`, exercitados em todas as três linhas de exemplos.
- **Resultado:** execução 43326822779 ultrapassou o login e encontrou DEF-001; execução 43327149247 concluiu os cenários.
- **Prevenção:** distinguir erro de preparação de falha funcional; nunca usar falha de login como prova de regra de negócio ausente.

## AUT-002 — teste do ciclo dependia da contagem de todas as reservas

- **Reprodução/ambiente:** rodar `CicloReservaApiTest` com dados de demonstração e relógio fixo; outras reservas também podem transitar na mesma chamada.
- **Esperado:** verificar transições e auditoria da reserva criada pelo cenário.
- **Defeito constatado por diff:** asserções exigiam contagem global 1/0, embora o agendador opere sobre todas as reservas. Não se preservou neste registro um log original da falha; não se inventa resultado anterior.
- **Correção:** [624f057](https://github.com/CarlaLDC/projeto-aloca/commit/624f0571bc42101c2d2f7e6317c06322d32b2f22) verifica estado da reserva específica e igualdade do histórico após nova execução terminal.
- **Regressão:** [CicloReservaApiTest](../../backend/src/test/java/br/com/aloca/CicloReservaApiTest.java), método `percorreSolicitadaAprovadaEmUsoEConcluidaComAuditoria`.
- **Evidência posterior:** [CI 43330605457](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457) concluído com sucesso; [relatório da suíte](testes-unitarios-backend.md) registra 126 testes sem falhas/erros/ignorados. É validação posterior da suíte, não prova preservada de vermelho/verde exclusivo deste defeito.
- **Prevenção:** asserções sobre dados exclusivos do cenário, preservando as verificações de auditoria e idempotência.

## DEF-PERF-001 — 409 lento em disputa por professor ou material

- **Ambiente:** campanha `20261113-campanha-01`, API `ca5919c`, MySQL 8.4 descartável, 50 usuários ([relatório](qualidade/RELATORIO-CARGA.md)).
- **Reprodução:** 10 Solicitantes, cada um com sua própria sala, pedem simultaneamente o mesmo professor (ou material) em horários sobrepostos, com cerca de 190 espaços ativos.
- **Esperado:** 1×201 e 9×409 em tempo comparável à disputa por sala.
- **Obtido:** regra correta (1×201, 9×409 em 100 de 100 disputas), mas o 409 tem mediana de ~0,9 s e P95 de 1,6 s, contra 83 ms nas disputas por sala; o 201 leva ~15 ms.
- **Causa provável:** `ReservaService.alternativas`, passo “outro local, mesmo horário”, consulta conflitos em cada espaço ativo dentro da transação com as travas; quando o choque é do professor ou do material, nenhum local resolve e a varredura é completa. As perdedoras são serializadas pela trava.
- **Correção proposta:** pular o passo quando o conflito não é do espaço, ou calcular alternativas após liberar as travas. **Regressão:** repetir PE-03 e comparar o P95 do 409 por tipo (`disputas.csv` e `RESULTADO.md`).
- **Reprodução no PostgreSQL:** campanha `20261113-campanha-02` (mesmo perfil, sem correção): 409 por professor com mediana de 620 ms e P95 de 1.167 ms; por material, 686 ms e 1.256 ms.
- **Correção:** `feat/back-reservas` `ed41485`. O passo "outro local, mesmo horário" só roda quando todos os conflitos são do próprio local; professor ou material ocupado continua ocupado em qualquer sala. O resultado da API não muda.
- **Regressão automatizada:** `AlternativasConflitoUnitTest` (3 casos). No código anterior, 2 falhas (professor e material consultavam todos os espaços); no novo, 3 aprovados.
- **Validação:** campanha `20261113-campanha-03`: 409 por professor com mediana de 47 ms e P95 de 90 ms; por material, 49 ms e 90 ms; sala sem mudança (P95 de 118 ms). Regra mantida: 150/150 disputas com uma única aceita ([relatório](qualidade/RELATORIO-CARGA.md)).

## DEF-INT-001 a 003 — notificação externa (WireMock)

- **Ambiente:** `NotificacaoExternaClientTest` com WireMock 3.13.2, sem Spring.
- **Obtido no código anterior:** 15 testes, 5 falhas e 1 erro. O log de falha gravava a mensagem da exceção (com o corpo da resposta externa) e o e-mail; dados aleatórios na resposta geravam `IllegalArgumentException` não tratada; uma notificação sem destinatário era enviada.
- **Correção:** `feat/back-reservas` `57b0d32`. O log registra só a reserva e o status ou tipo da falha; qualquer falha de execução vira falha tratada; destinatário ausente não é enviado.
- **Validação:** 21 testes WireMock aprovados (15 do cliente e 6 pela API com PostgreSQL), dentro de `mvn verify` com 502 testes e 0 falhas ([relatório](qualidade/RELATORIO-WIREMOCK.md)).

## Pendências e reavaliação do registro de performance

Fonte histórica: [defeitos.md na branch qa/performance-jmeter](https://github.com/CarlaLDC/projeto-aloca/blob/qa/performance-jmeter/testes/performance/defeitos.md). O documento original pertence a outro snapshot; não deve ser apresentado como diagnóstico atual de develop.

| ID original / atual | Triagem atual | Próxima ação e critério de encerramento |
|---|---|---|
| IMP-001 — API/ambiente | Superado para PE-02/PE-03: `executar-carga.sh` sobe API + MySQL descartável e registra commit e máquina em `ambiente.txt` | Ambiente de homologação dedicado continua desejável |
| IMP-002 — credenciais/massa | Superado: `preparar_massa.py` cria massa sintética exclusiva por execução (prefixo `CARGA-<RUN_ID>`), senha aleatória fora dos artefatos e banco descartável | — |
| IMP-003 — JMeter sem resultado | Encerrado: plano, JTL, dashboard, logs, parâmetros, commit, métricas, análise e decisão em [RELATORIO-CARGA](qualidade/RELATORIO-CARGA.md) | PE-01 executado e aprovado (campanha 03, PostgreSQL). Depois do merge, repetir pelo workflow para vincular ao SHA do develop |
| GAP-004/005 — auditoria em memória/ator fictício | Diagnóstico histórico superado na implementação integrada; ciclo e auditoria têm testes reais vinculados acima | Manter origem histórica; não converter a inspeção antiga em bug de runtime reproduzido nem declarar AUD-01..10 executados |
| NC-001 — versão Spring Boot | Aberto, S2/P1: [pom.xml](../../backend/pom.xml) declara 4.1.2; enunciado exige 3.x | Alinhar versão ao requisito e executar integração MySQL, unitários, BDD e build; registrar commit/relatórios antes de encerrar |

A existência destas pendências impede afirmar que todos os defeitos foram corrigidos ou que a entrega está aprovada. Não há base neste inventário para declarar “zero vulnerabilidades críticas”: isso exige evidência da análise de segurança.

## Atualização, regressão e encerramento

1. Criar registro com ID estável usando o [template de defeito](../../.github/ISSUE_TEMPLATE/defeito.md); incluir requisito, commit, ambiente, passos, esperado/obtido e evidência.
2. Na triagem, definir severidade, prioridade, justificativa e responsável no GitHub; manter mudanças de classificação no histórico da issue.
3. Associar PR/commit ao ID e adicionar teste que detecte a causa. Preservar falha anterior quando possível, sem forjar histórico.
4. Reexecutar o teste específico e a suíte impactada no commit corrigido; vincular run e artefato. Sem execução, manter “Corrigido — validação pendente”.
5. Encerrar após revisão do resultado; atualizar este inventário e a RTM vigente. Reabrir pelo mesmo ID se houver recorrência.

### Reprodução das regressões existentes

```bash
# Java 21, Maven e Docker; MySQL real por Testcontainers
mvn -f backend/pom.xml -Dtest=CicloReservaApiTest test
```

Para DEF-001/AUT-001, seguir [os dois terminais e pré-requisitos do guia BDD](../../bdd/EVIDENCIAS-APROVACAO-INATIVOS.md#reproduzir), usando banco descartável. Relatórios Maven em `backend/target/surefire-reports`; Cucumber conforme configuração dedicada.

## Validação desta consolidação

Mudança documental: fontes, diffs de correção e estados dos runs 43326822779 (failure), 43327149247 (success) e 43330605457 (success) consultados no GitHub. Nenhuma nova execução da aplicação foi realizada nesta consolidação. As evidências acima são históricas e limitadas aos respectivos commits/ambientes.
