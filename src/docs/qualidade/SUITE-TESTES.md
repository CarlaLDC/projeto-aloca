# Índice unificado da suíte de testes

Snapshot: develop `0d3ef6a6e2cb56facc8475ba696ef3dbee55a295`, em 12/11/2026. Direção e critérios: [Plano](PLANO-TESTES.md). Resultados: [Relatório](RELATORIO-TESTES.md).

## Backend — disponível na base

Diretório: [backend/src/test/java/br/com/aloca](../../../backend/src/test/java/br/com/aloca).

| Modalidade | Classes |
|---|---|
| Unitários isolados (tag unitario) | unit/ReservaUnitTest; unit/ReservaServiceUnitTest; unit/MovimentacaoServiceUnitTest |
| Parametrizados de integração | ReservaParametrizadaApiTest |
| Autenticação/perfil | AutenticacaoTest; AprovacaoApiTest; AdminRecursosTest; ReservaApiTest |
| Reservas, conflitos, manutenção, auditoria | ReservaApiTest; QualidadeFuncionalApiTest; AdminRecursosTest |
| Disponibilidade | DisponibilidadeApiTest |
| Materiais | MaterialApiTest |
| Relatórios | RelatorioApiTest |
| Concorrência | ConcorrenciaReservaTest |
| Ciclo e ausência | CicloReservaApiTest; NaoComparecimentoApiTest |
| Inicialização | AlocaApplicationTest |

```bash
# Java 21 e Maven; não exige Docker
mvn -f backend/pom.xml -Dgroups=unitario test

# Java 21, Maven e Docker; inclui todas as modalidades backend incorporadas
mvn -f backend/pom.xml clean verify

# Seleção útil para investigação
mvn -f backend/pom.xml -Dtest=ReservaParametrizadaApiTest test
mvn -f backend/pom.xml -Dtest=ConcorrenciaReservaTest test
```

O clean verify já inclui os unitários; **não somar** a contagem do job exclusivo à suíte completa. Parametrizado é técnica de dados, não um nível adicional de execução.

Relatórios: backend/target/surefire-reports e backend/target/site/jacoco. Documentação: [unitários](../testes-unitarios-backend.md) e [parametrizados](../testes-parametrizados.md).

## API caixa-preta — incorporada pelo PR #38

[RegrasCriticasHttpTest](../../../backend/src/test/java/br/com/aloca/http/RegrasCriticasHttpTest.java) usa HTTP real/PostgreSQL (Testcontainers) e tag http. [Documentação](../testes-api-http.md) e [PR #38](https://github.com/CarlaLDC/projeto-aloca/pull/38).

Na base integrada:

```bash
mvn -f backend/pom.xml -Dgroups=http test
```

A tag http seleciona a suíte de 32 casos incorporada. Não interpretar “zero testes selecionados” como aprovação.

## Frontend e E2E/BDD

Frontend: specs em src/app, incluindo login, formulários, disponibilidade, aprovações, materiais, relatórios e detalhes.

```bash
npm ci
npm run build
npm test -- --watch=false --browsers=ChromeHeadless
```

E2E integrado: [feature](../../../bdd/features/integracao.feature), [steps](../../../bdd/steps/integracao.cjs), [instruções](../../../bdd/README.md). Em terminais distintos, na raiz:

```bash
# Banco descartável (PostgreSQL na memória do contêiner, porta 5433)
docker compose -f backend/docker-compose.yml --profile e2e up -d postgres-e2e

# Terminal 1
mvn -f backend/pom.xml spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://localhost:5433/aloca_e2e"

# Terminal 2
npm ci
npm start -- --proxy-config proxy.conf.json

# Terminal 3
npm install --prefix bdd
npx --prefix bdd playwright install chromium
cd bdd
npm test
```

O runner padrão descobre features/**/*.feature, support/**/*.cjs e steps/**/*.cjs. Alguns cenários usam navegador, outros chamadas de API; o conjunto não deve ser descrito como inteiramente UI.

BDD de funcionalidade nova: [feature](../../../bdd/features/aprovacao-inativos.feature) e [evidências antes/depois](../../../bdd/EVIDENCIAS-APROVACAO-INATIVOS.md). Com apenas a API na porta 8080:

```bash
npm install --prefix bdd
cd bdd
npx cucumber-js --config aprovacao-inativos.cjs
```

## Performance e modalidades ainda sem suíte incorporada

| Item | Localização | Uso e pendência |
|---|---|---|
| **JMeter pesquisa, reservas e disputas (PE-01/PE-02/PE-03)** | [testes/carga](../../../testes/carga/README.md), workflow `carga-jmeter.yml` | Incorporado: pesquisa de disponibilidade com 50 usuários, criar/alterar/cancelar com massa isolada + 150 disputas simultâneas; JTL, dashboard e prova por disputa em [RELATORIO-CARGA](RELATORIO-CARGA.md) |
| JMeter API leitura | [README da branch](https://github.com/CarlaLDC/projeto-aloca/blob/qa/performance-jmeter/testes/performance/README.md) | Login/GETs; fora de develop; não cobre reservas concorrentes sob carga |
| JMeter SPA | Mesma branch, plano aloca-spa-load-test.jmx | Mede entrega de rotas, não ações client-side nem regras de reserva |
| **WireMock (EX-01..04)** | `NotificacaoExternaClientTest`, `NotificacaoExternaApiTest` | Incorporado: sucesso, tempo limite, 4xx/5xx sem expor dados, resposta quebrada e destinatário ausente ([RELATORIO-WIREMOCK](RELATORIO-WIREMOCK.md)) |
| Manual, recuperação e instalação | PLANO-TESTES MA/RE/IN | Executar e anexar registro; não há resultado presumido |
| SonarCloud / dependências | [Projeto SonarCloud](https://sonarcloud.io/dashboard?id=CarlaLDC_projeto-aloca) | Vincular análise ao SHA da release; dependências precisam de relatório próprio |

Não copiar o relatório antigo da branch JMeter como descrição da arquitetura atual: ele contém referências ao frontend demonstrativo anterior. O plano atual diferencia esse histórico dos testes backend já existentes.

## Workflows e artefatos

| Workflow / job | Artefato |
|---|---|
| CI integrado — unitários | backend-unitarios |
| CI integrado — backend completo | backend-testcontainers-jacoco |
| Verificação backend — qualidade funcional | qualidade-funcional-relatorios |
| BDD integrado — Angular + API + PostgreSQL | bdd-integrado-evidencias |
| BDD — ciclo de aprovação de cadastros | bdd-aprovacao-inativos-RUN_ID |
| API HTTP | backend-api-http |
| Angular | Logs do build/Karma no job; não presumir artefato específico |

No GitHub: PR → Checks → job → execução; ou Actions → workflow → execução → Artifacts. Identificar SHA e conclusão; baixar ZIP, extrair e abrir HTML ou consultar XML/JSON/TXT. Artefato presente em execução com falha não significa aprovação, pois a publicação pode usar if: always().
