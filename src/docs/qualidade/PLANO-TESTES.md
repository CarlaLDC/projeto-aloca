# Plano unificado de testes — Aloca

Versão 1.0 — 12/11/2026. Base inspecionada: develop, commit `0d3ef6a6e2cb56facc8475ba696ef3dbee55a295`. Responsáveis são papéis a atribuir pela equipe; não houve aprovação ou revisão por pares presumida.

Este é o plano que organiza **todas as modalidades** do projeto. O [índice da suíte](SUITE-TESTES.md) indica testes e comandos; o [relatório consolidado](RELATORIO-TESTES.md) registra resultados e pendências. Planejamento, automação disponível e resultado aprovado são estados distintos.

## 1. Objetivo, escopo e base de teste

Validar alocação de salas, materiais e professores com autorização, prevenção de sobreposição/dupla reserva, aprovação de restritos, manutenção, movimentação e auditoria. A avaliação inclui evidências de execução, rastreabilidade, segurança e desempenho.

Base: enunciado fornecido pela equipe; [requisitos funcionais](../requisitos-funcionais.md), [não funcionais](../requisitos-nao-funcionais.md), [arquitetura](../arquitetura.md), contratos HTTP no [README](../../../README.md), código e evidências referenciadas no índice. A numeração RF-01..RF-14 usada abaixo segue o enunciado da revisão, que pode diferir dos documentos históricos.

Fora do aceite automático deste plano: desempenho de produção, conformidade legal, pentest completo, validação de treinamento de modelos e interfaces não implementadas. Não se extrapola aprovação de uma suíte para todas as dimensões.

## 2. Prioridades por risco

| Prioridade | Risco | Verificação mínima |
|---|---|---|
| P0 — bloqueia entrega | Aplicação não executa; autorização indevida; dupla reserva; ausência de pipeline | Build/instalação, matriz de perfis, disputa simultânea e execução do PR |
| P1 — regra crítica | Horário inválido, manutenção ignorada, aprovação indevida, auditoria incompleta, perda de dados | Unitários + integração/API + BDD e reinício real |
| P2 — operação e experiência | Relatório incorreto, mensagens confusas, navegação/responsividade e latência | Relatórios, E2E, manual e carga |
| P1/P2 transversal | Vulnerabilidades, dependências e exposição de detalhes internos | Testes negativos, SonarCloud, análise de dependências e inspeção |

P0 não é sinônimo de severidade de defeito; prioridade do teste e severidade do defeito são registrados separadamente.

## 3. Estratégia por modalidade

| Modalidade | Objetivo / técnica | Ambiente | Artefato / situação na base |
|---|---|---|---|
| Unitário backend | Decisões, partições, fronteiras, auditoria e estados | JUnit + Mockito em dependências, relógio fixo, sem Spring/banco | 3 classes isoladas; executáveis |
| Parametrizado | Matriz de intervalos e fronteiras de sala/material/professor | JUnit MethodSource/CsvSource, aplicação e MySQL | ReservaParametrizadaApiTest; executável |
| Integração | Controllers/serviços/JPA, transações e estado persistido | Spring + MockMvc + MySQL 8.4/Testcontainers | Suíte ApiTesteBase; executável |
| API caixa-preta | Contratos/segurança/regras exclusivamente por HTTP/JSON | HttpClient, servidor real, MySQL/Testcontainers | RegrasCriticasHttpTest; incorporada pelo PR #38 |
| Concorrência | Exatamente uma aceitação por disputa do mesmo alvo | Integração paralela MySQL; HTTP paralelo após #38 | ConcorrenciaReservaTest + suíte HTTP |
| Frontend | Componentes, formulários, estados e formatação | Angular/Karma/Jasmine/ChromeHeadless | Specs existentes; executáveis |
| E2E | Navegador → Angular → API → banco | Cucumber/Playwright, Chromium, H2 descartável no CI | bdd/features; nem todo cenário usa interface |
| BDD de incremento | Exemplos antes da regra, falha observada e aprovação posterior | Cenários de desativação antes da aprovação | Ciclo com JSONs originais versionados |
| Integração externa | Sucesso, timeout, erro e resposta inválida | WireMock; sem chamada à API de terceiros nos testes | EX-01 a EX-04 implementados e executados: 21 testes aprovados ([RELATORIO-WIREMOCK](RELATORIO-WIREMOCK.md)) |
| Carga | Percentis, throughput, erros e disputas sob carga | JMeter em API/BD real e ambiente controlado | PE-01/PE-02/PE-03 em `testes/carga` executados no PostgreSQL ([RELATORIO-CARGA](RELATORIO-CARGA.md)) |
| Segurança/robustez | Perfil, propriedade, entradas, erros seguros e dependências | API, SonarCloud e análise de dependências | Casos funcionais existem; parecer consolidado depende de relatórios |
| Manual/usabilidade | Fluxos e mensagens em 360/1280 px | Navegador e capturas datadas | Roteiros neste plano; evidências pendentes |
| Recuperação | Reserva e histórico após reinício da API | MySQL persistente fora do ciclo descartável de teste | Roteiro neste plano; execução específica pendente |
| Instalação/release | Executar a partir de checkout limpo e instruções | Java 21/Maven/Node 22; Docker quando necessário | Checklist e evidência por release |
| Análise estática/cobertura | Linhas, branches, bugs e vulnerabilidades | JaCoCo e SonarCloud | Relatórios por SHA; percentual antigo no README não é aceite atual |

Mocks são permitidos somente para isolar dependências em unitários. Consultas SQL, integridade, locks e integração crítica precisam das suítes reais. H2 do BDD não substitui MySQL/Testcontainers. Uma feature Gherkin não prova desenvolvimento guiado sem histórico e execuções vinculados.

## 4. Ambientes, dados e isolamento

| Ambiente | Configuração | Uso e restrições |
|---|---|---|
| U | Java 21; Maven; dependências disponíveis | Unitários, sem Docker/banco/servidor |
| I | Java 21, Maven, Docker, MySQL 8.4 descartável | Integração/parametrizados/concorrência; schema recriado; dados sintéticos |
| H | Ambiente I + servidor Spring em porta aleatória | HTTP caixa-preta após incorporação do #38 |
| F | Node 22, npm, Chrome/Chromium | Build e specs Angular |
| E | API 8080 e Angular 4200; H2 isolado; Chromium | BDD/E2E conforme workflow; usar base demonstrativa |
| C | API + banco real, máquina/recursos medidos | JMeter; registrar JVM, CPU/RAM, SO, versões, URL e volume; sem outros testes concorrendo |
| R | API + MySQL persistente controlado | Reinício sem apagar volume; não usar Testcontainers descartável para comprovar retenção |

Contas são as de demonstração do README. Tokens e senhas não entram em relatório/logs públicos. Cada execução usa massa sintética e identificador exclusivo; a suíte HTTP cria cadastros pela API. IDs e horários só são compartilhados deliberadamente nos casos de conflito. Não executar carga ou alteração de massa contra dados reais.

Datas variáveis usam o fuso America/Sao_Paulo. Testes unitários usam Clock fixo. Testes de fronteira temporal do agendador devem manter o relógio e as condições documentados em suas classes. Falta de ambiente é “bloqueado”, não “aprovado” nem falha de negócio.

## 5. Ordem de execução

1. Registrar branch, SHA, versões e pré-condições; conferir que testes/documentos do PR pretendido estão incorporados.
2. Build e unitários isolados; interromper investigação de regressão se o básico não executa.
3. Backend completo com MySQL: inclui parametrizados, integração e concorrência; HTTP já integra pelo #38.
4. Frontend: build e specs; em seguida E2E/BDD com processos e banco isolados.
5. Regras negativas de segurança, integração externa/WireMock quando disponível e recuperação por reinício.
6. Manual responsivo e acessibilidade básica dos fluxos críticos.
7. Carga no ambiente C, sem competir com outras suítes; guardar JTL/dashboard e resultados por operação.
8. Reunir cobertura, SonarCloud e dependências; registrar defeitos/regressões; consolidar a decisão.

Em cada PR: executar os workflows configurados. Antes da release: todos os itens críticos, manual, recuperação, carga e análise estática. Depois de correção: caso reprodutor + regressão da modalidade afetada e das dependentes; não somar resultados antigos como se fossem execução única.

## 6. Critérios de entrada, suspensão e retomada

Entrada: build instalável, massa sintética, credenciais de teste, ambiente acessível e identificador do código. Integração exige Docker disponível; HTTP está incorporado pelo #38; carga exige plano de negócio validado, metas e ambiente registrados.

Suspender execução se ambiente estiver indisponível, massa contaminada, relógio/configuração divergente, risco de modificar dados reais ou defeito impedir avaliar os próximos passos. Registrar causa e o que não foi executado.

Retomar após corrigir ambiente/defeito, recriar massa quando necessário e confirmar a mesma pré-condição. Preservar o resultado anterior e gerar nova execução/SHA; não sobrescrever uma falha histórica para simular aprovação.

## 7. Critérios de saída e decisão de qualidade

| Dimensão | Critério |
|---|---|
| Executabilidade/CI | Checkout instalável e workflows do SHA candidato aprovados |
| Regras críticas | Todos os casos P0/P1 aplicáveis aprovados; sem falha de autorização ou dupla reserva |
| Testes | Sem falhas, erros, undefined/pending ou ignorados nos casos exigidos; exclusões justificadas não equivalem a aprovação |
| Cobertura | Backend >=80% de linhas e >=70% de branches em JaCoCo, escopo e SHA declarados |
| Segurança estática | Zero bugs/vulnerabilidades críticas; RNF-03 local também exige zero vulnerabilidades altas; aplicar o critério mais restritivo |
| Rastreabilidade | 100% dos requisitos críticos ligados a risco, caso e evidência; revisar RTM separadamente |
| BDD | Incremento novo com exemplos prévios e execuções antes/depois preservadas |
| Desempenho | A proposta RNF-01 é 50 usuários: P95 disponibilidade <=2 s; demais operações comuns <=3 s; validar o perfil de carga antes do aceite |
| Recuperação/manual | Sem perda de reserva/histórico após reinício; fluxos de consulta/reserva/cancelamento válidos a 360 e 1280 px |
| Defeitos | Nenhum defeito crítico aberto; riscos residuais com aceite explícito e responsável |

Para carga, a taxa de erro deve discriminar 409 esperado por disputa de 5xx, timeout ou falha inesperada. A condição de negócio é uma reserva aceita por disputa, mesmo sob volume; latência baixa sozinha não aprova. Não há resultado de performance presumido.

Decisões possíveis: aprovado, aprovado com restrições documentadas, reprovado ou inconclusivo/bloqueado. Limitadores do enunciado não são liberados por média de cobertura. Este plano não declara aprovação da release atual.

## 8. Roteiros ainda necessários

### Carga JMeter — PE-01 a PE-03

- PE-01: aquecer API e executar pesquisa de disponibilidade com 50 usuários; separar aquecimento da medição; medir P95, throughput, erros e recursos.
- PE-02: criar/alterar/cancelar com massa exclusiva, sem restritos pendentes acidentais; medir P95 de cada operação, consistência e limpeza.
- PE-03: grupos simultâneos disputam sala, professor e material; correlacionar tentativas por alvo/intervalo e verificar uma única aceitação e estado persistido.
- Registrar duração, ramp-up, iterações, think time, threads e tamanho da massa no plano aprovado. Como proposta inicial de campanha, usar ramp-up de 60 s e medição de 10 min, ajustando somente com justificativa.
- PE-01, PE-02 e PE-03 estão implementados em [testes/carga](../../../testes/carga/README.md) e executados ([RELATORIO-CARGA](RELATORIO-CARGA.md)). PE-01 roda numa fase própria, depois de PE-02/PE-03, com 50 usuários pesquisando. Os JMX de outra branch medem SPA ou API de leitura e não comprovam PE-02/PE-03. Antes de executar, validar a versão da API, conferir asserções e implementar o perfil mutável ausente.

### Integrações externas WireMock — EX-01 a EX-04

- EX-01: resposta válida produz o resultado esperado sem chamada externa real.
- EX-02: timeout respeita limite e gera resposta/tratamento seguro sem mudança parcial indevida.
- EX-03: 4xx/5xx externo tem política explícita e não expõe payload/credencial.
- EX-04: JSON inválido/campos ausentes são tratados com segurança.
- Integração testada: envio das notificações ao serviço de e-mail da instituição (`NotificacaoExternaClient`), com contrato e política de falhas em [RELATORIO-WIREMOCK](RELATORIO-WIREMOCK.md). Suítes: `NotificacaoExternaClientTest` e `NotificacaoExternaApiTest`.

### Responsividade e experiência — MA-01 a MA-04

| Caso | Passos em 360 px e 1280 px | Resultado esperado / evidência |
|---|---|---|
| MA-01 | Entrar como Solicitante; buscar disponibilidade; usar filtros | Conteúdo legível, controles acessíveis, sem sobreposição; capturas |
| MA-02 | Criar reserva válida e tentar conflito | Confirmação/código; erro compreensível; alternativas utilizáveis |
| MA-03 | Consultar detalhe e cancelar com motivo | Histórico consultável e reserva cancelada; mensagens claras |
| MA-04 | Entrar como Responsável/Admin; aprovar/rejeitar, movimentar e bloquear | Ações e tabelas utilizáveis; sem opção indevida habilitada |

Registrar navegador/SO, tamanho do viewport, caso, esperado/obtido, status e capturas. Verificar navegação por teclado, rótulos e foco nos fluxos críticos. O roteiro não constitui auditoria completa de acessibilidade.

### Recuperação — RE-01

Criar reserva e decisão auditada pela API com MySQL persistente; guardar código e histórico. Parar e reiniciar somente a API, preservando o volume. Consultar os mesmos dados e comparar código, estado, atores e eventos. Guardar logs de parada/início e JSON antes/depois sem tokens. Não usar delete de volume ou recriação automática do schema.

### Instalação e segurança — IN-01 / SE-01

IN-01: checkout limpo da release, seguir README, executar backend/frontend, login e reserva; registrar falhas e versões. SE-01: executar matriz de autorização/posse/validação/erros, conferir SonarCloud e análise de dependências, registrar gravidades e itens abertos. Testes funcionais aprovados não provam ausência de vulnerabilidades.

## 9. Papéis e gestão de defeitos

| Papel | Responsabilidade |
|---|---|
| Autor do incremento | Caso automatizado, massa, evidência e correção |
| Revisor de qualidade | Revisar exemplos, criticidade, relatório e rastreabilidade |
| Responsável por ambiente | Versões, dados, Docker, execução de carga e recuperação |
| Responsável pela release | Decidir aceite e riscos residuais com base nas evidências |

Registrar cada defeito em issue ou registro versionado com ID, requisito, SHA, ambiente, passos, esperado/obtido, severidade, prioridade, evidência, responsável e status. Fluxo: novo → triado → em correção → pronto para reteste → encerrado/reaberto. Encerrar somente com commit da correção e caso de regressão aprovado.

## 10. Evidências e manutenção do plano

Para cada campanha, usar o [registro de execução](REGISTRO-EXECUCAO.md). Preservar comando, SHA, resultado, contagens, links de workflow/job/artefato e limites. Surefire e Cucumber têm critérios de contagem diferentes: invocações parametrizadas e cenários não se somam como uma única contagem de produto.

Artefatos Actions expiram; copiar os resultados necessários para o pacote versionado de evidências ou entrega final. Não salvar credenciais, JWTs ou dados reais. JSONs originais do ciclo BDD já estão versionados.

Atualizar este plano ao mudar regra, ferramenta, ambiente, suíte ou requisito. Atualizar SUITE-TESTES após incorporar novas suítes ou branches. Atualizar RELATORIO-TESTES somente com resultados verificáveis e respectivos SHAs. A matriz deste plano orienta cobertura; não substitui uma RTM fechada nem o relatório final de qualidade da release.
