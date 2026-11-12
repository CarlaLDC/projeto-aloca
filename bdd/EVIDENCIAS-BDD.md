# Evidências de BDD — aprovação e bloqueio

Revisão: 12/11/2026. Esta página distingue especificação executável, resultado de execução e evidência de desenvolvimento guiado pela técnica.

## Situação do requisito

**Parcialmente demonstrado.** Há cenários Gherkin e automação Cucumber/Playwright. Esta revisão acrescenta aprovação restrita e bloqueio de manutenção, mas essas regras de negócio já existiam antes dos novos cenários. Portanto, este incremento é cobertura de regressão orientada a comportamento; não comprova retrospectivamente que uma funcionalidade nova foi desenvolvida com BDD/TDD.

O critério “ao menos uma funcionalidade nova desenvolvida com evidências da técnica” só pode ser considerado concluído quando houver evidência verificável de um incremento de negócio guiado pelos exemplos/testes, além de sua execução aprovada. Não foi fabricado ciclo vermelho/verde nem alterado o histórico.

## O que foi encontrado

| Evidência | O que demonstra | O que não demonstra |
|---|---|---|
| [Commit 45f4236](https://github.com/CarlaLDC/projeto-aloca/commit/45f42367eb1410aa03384df405e03e4791b5316e) | Inclusão de cenários integrados de login, autorização, reserva e conflito | Execução aprovada ou teste anterior à implementação |
| [Commit 31738d9](https://github.com/CarlaLDC/projeto-aloca/commit/31738d9262ffc1557f2448f8cd3c0cd3290026d5) | Extensão dos cenários para competência e autorização de ausência | Ciclo BDD/TDD de aprovação e manutenção |
| [AprovacaoApiTest](../backend/src/test/java/br/com/aloca/AprovacaoApiTest.java) | Asserções de autorização, aprovação, justificativa e auditoria | Resultado do teste nesta revisão |
| [AdminRecursosTest](../backend/src/test/java/br/com/aloca/AdminRecursosTest.java) | Asserções de cadastro e validação de bloqueio | Ordem temporal teste → implementação |
| [Cenários novos](features/aprovacao-bloqueio.feature) | Exemplos explícitos de aprovação e manutenção | Nova regra de negócio implementada neste incremento |
| [Steps novos](steps/aprovacao-bloqueio.cjs) | Requisições reais, verificações de estado persistido e histórico | Êxito em runtime antes de consultar o relatório |

## Exemplos e rastreabilidade

Os IDs RF seguem o enunciado: RF-01 autorização, RF-07 aprovação restrita, RF-08 manutenção e RF-10 auditoria.

| ID / tag | Regra e risco | Resultado verificável | Implementação |
|---|---|---|---|
| BDD-APR-01 | RF-07/RF-10: confirmação restrita sem decisão ou auditoria | Criação SOLICITADA; decisão do Responsável produz APROVADA e histórico SOLICITADA → APROVADA com ator | AprovacaoService.aprovar; Reserva.mudarStatus |
| BDD-APR-02 | RF-01/RF-07: perfil indevido aprova | Solicitante e Administrador recebem 403; estado e histórico permanecem iguais | SecurityConfig e AprovacaoController |
| BDD-BLO-01 | RF-08: reserva criada durante manutenção | 409 com conflito de Manutenção; nenhuma reserva com a atividade exclusiva em minhas reservas | AdminService.bloquear; ReservaService.colisoes |
| BDD-BLO-02 | RF-07/RF-08/RF-10: solicitação aprovada após bloqueio posterior | 409; SOLICITADA e histórico anterior preservados | AprovacaoService.aprovar; ReservaService.problemas |

São quatro exemplos, com cinco casos executáveis porque APR-02 possui dois perfis. Cada caso cria um recurso exclusivo, sem depender dos IDs da massa demonstrativa e sem disputar a sala de outro cenário. Usuários são os da base de demonstração.

São testes da API em caixa-preta, executados pelo mesmo runner integrado com navegador disponível. Não são fluxos novos pela interface. Usam login e persistência reais; não usam mocks. O workflow BDD existente usa H2 em memória, portanto estes cenários não substituem os testes MySQL/Testcontainers nem o teste paralelo de concorrência.

## Executar e obter relatório

Use um ambiente descartável com dados de demonstração, conforme [README BDD](README.md). Os casos criam salas, reservas e bloqueios; não devem ser executados contra dados de produção.

Com backend e frontend em execução:

```bash
cd bdd
npm test -- --tags "@aprovacao-bloqueio"
```

Para a suíte completa:

```bash
cd bdd
npm test
```

O arquivo cucumber.cjs descobre automaticamente features/**/*.feature e steps/**/*.cjs.

No GitHub: abra o PR → Checks → **BDD integrado - Angular + API + H2**, ou Actions → mesmo workflow → execução do PR → Artifacts → **bdd-integrado-evidencias**. O artefato contém reports/cucumber.html e reports/cucumber.json; o hook anexa captura nas falhas. É preciso aguardar a conclusão do job para o artefato aparecer. A publicação usa if: always(), mas ausência de relatório quando a aplicação não inicia não significa aprovação.

Para interpretar: todos os cinco casos novos devem estar PASSED; FAILED, UNDEFINED, PENDING ou SKIPPED não atendem ao critério. Preserve o link da execução e seu SHA: o relatório sozinho não identifica desenvolvimento guiado.

## Registro desta revisão

| Etapa | Registro real |
|---|---|
| Exemplos registrados | [64bed8e](https://github.com/CarlaLDC/projeto-aloca/commit/64bed8e94506d7f72fede80dc5044bfd5baa9711): Gherkin de aprovação e manutenção |
| Automação registrada | [58830a7](https://github.com/CarlaLDC/projeto-aloca/commit/58830a73a3365e65408d082e6a7cb4412546c85b): steps e asserções |
| Implementação de negócio | Já existia em develop antes dos exemplos desta revisão |
| Validação local | Sintaxe JavaScript dos novos steps verificada com node --check |
| Execução integrada | Não executada localmente nesta revisão; consultar o job do PR e seu artefato |
| Resultado vermelho de regra ausente | Não produzido; regras já existentes |
| Resultado verde integrado | Não declarado sem relatório |

Ter um commit de cenários antes do commit de steps documenta a ordem da automação, mas não prova que a funcionalidade de negócio nasceu por BDD.

## Como fechar a evidência de funcionalidade nova

No próximo incremento real de aprovação/bloqueio, a equipe deve registrar, nesta ordem:

1. Comportamento novo acordado em exemplos Dado/Quando/Então e critério de aceite no PR.
2. Automação do exemplo, com execução falhando pela regra de negócio ainda ausente. Falha por step indefinido, dependência ou servidor desligado não comprova esse estágio.
3. Implementação mínima da regra e execução passando no mesmo cenário e ambiente.
4. Refatoração, se necessária, com a suíte preservada.
5. Links dos commits, execuções e artefatos dos estágios, requisito/risco correspondente e revisão por outro integrante.

Registro a preencher somente com fatos produzidos:

| Campo | Estado |
|---|---|
| Regra nova e critério de aceite | Pendente de incremento real |
| Commit dos exemplos/teste | Pendente |
| Execução com falha de negócio esperada e SHA | Pendente |
| Commit da implementação | Pendente |
| Execução aprovada e SHA | Pendente |
| Refatoração e regressão | Pendente, quando aplicável |
| Revisor e decisão | Pendente |

Na apresentação: “Temos BDD executável cobrindo aprovação e manutenção, com verificações de autorização, persistência e auditoria. Esses exemplos foram acrescentados a regras existentes. A prova histórica de desenvolvimento de funcionalidade nova guiado por BDD continua separada dos relatórios de regressão.”
