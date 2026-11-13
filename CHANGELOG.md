# Histórico de versões

## 1.0.0-rc.1 — release candidate (13/11/2026)

Primeira versão candidata à entrega do Aloca: sistema de reservas de salas, laboratórios, professores e materiais
sem conflito de horário. Base: `develop` com os PRs até o #59.

### Funcionalidades (RF-01 a RF-14)

- **Autenticação e perfis** (Solicitante, Professor, Responsável, Administrador) com JWT de 8 h; proteção contra força
  bruta (5 tentativas em 15 min); mudar perfil, senha ou situação encerra as sessões abertas.
- **Disponibilidade:** recursos e horários livres do dia, com filtros por tipo, texto e capacidade; professores por
  competência.
- **Reservas:** criar, consultar, alterar e cancelar; conflito de sala, professor ou material recusado com **409** e até
  três alternativas; travamento pessimista garante **uma única reserva** por disputa simultânea.
- **Ciclo de vida com auditoria:** Solicitada → Aprovada → Em uso → Concluída, com rejeição, cancelamento e não
  comparecimento; matriz única de estados (a entidade recusa transição inválida) e histórico com estado anterior,
  estado novo, ator e data em todas as transições.
- **Aprovação** de recursos restritos pelo Responsável; **não comparecimento** de 15 min após o início até o fim.
- **Materiais:** retirada e devolução, com veto a cancelamento e ausência quando o material foi retirado.
- **Bloqueio de manutenção:** cancela as reservas aprovadas do período (com auditoria e aviso), mantém as pendentes e
  é recusado se houver reserva em uso.
- **Administração:** recursos, professores, usuários (com vínculo ao cadastro de professor) e bloqueios.
- **Relatórios** de utilização, horas e conflitos com exportação CSV; painel por perfil; notificações internas e
  envio a serviço externo de e-mail (opcional).
- **API documentada** no Swagger (`/swagger-ui.html`).

### Qualidade (develop `e9a6b46`)

| Item | Resultado |
|---|---|
| Back-end (`mvn verify`, PostgreSQL 17 via Testcontainers) | **502 testes**, 0 falhas; JaCoCo **95,9% linhas / 85,8% ramos** (meta 80/70 obrigatória no build) |
| Front-end (Karma) | **102 testes**; **81,04% linhas / 70,58% ramos** (meta 80/70 obrigatória no CI) |
| BDD (Cucumber + Playwright, API real) | 3 funcionalidades, 14 casos executáveis, aprovados no CI |
| Integração externa (WireMock, EX-01 a EX-04) | 21 testes; 3 defeitos encontrados e corrigidos |
| Carga (JMeter, 50 usuários, PostgreSQL) | PE-01 pesquisa P95 6–8 ms (meta 2 s); reservas P95 ≤ 117 ms (meta 3 s); 150/150 disputas com uma única reserva; 0% de erro |
| Auditoria | Todas as 12 transições permitidas + criação verificadas pela API; matriz completa (49 pares) em teste unitário |
| Análise estática | SonarCloud com Quality Gate aprovado |
| Dependências | `npm audit --omit=dev`: 0; análise do back-end com OSV-Scanner no CI (PR #60) |

### Correções relevantes

- **DEF-PERF-001:** 409 de disputa por professor/material deixou de varrer todas as salas (P95 de ~1,2 s para 90 ms).
- **DEF-INT-001 a 003:** notificação externa não expõe o corpo da resposta nem o e-mail no log; resposta corrompida e
  destinatário ausente tratados.
- Login aceita a requisição mesmo com token expirado guardado no navegador.
- Sonar: pasta de evidências do reinício restrita (path traversal); analisador de carga com argumentos validados.

### Mudanças que exigem atenção

- **Banco somente PostgreSQL 17** (MySQL e H2 foram retirados). Local: `docker compose -f backend/docker-compose.yml up -d`
  ou PostgreSQL instalado com usuário e banco `aloca` (veja o README).
- **Spring Boot 4.1.2 / Java 21**: o enunciado pede 3.x; versão aceita pelo professor em 13/11/2026 (NC-001).
- Em produção, configurar `ALOCA_JWT_SEGREDO`, `ALOCA_DB_*` e desligar os dados de demonstração (`aloca.dados-demo=false`).

### Pacote de evidências

| Evidência | Onde |
|---|---|
| Plano, suíte e relatório de testes | `src/docs/qualidade/PLANO-TESTES.md`, `SUITE-TESTES.md`, `RELATORIO-TESTES.md` |
| RTM (requisito → risco → teste → evidência) | `RTM.md` |
| Registro de defeitos | `src/docs/defeitos.md` |
| Carga JMeter | `src/docs/qualidade/RELATORIO-CARGA.md` e `testes/carga/resultados/` (JTL e dashboard) |
| WireMock | `src/docs/qualidade/RELATORIO-WIREMOCK.md` e `evidencias-wiremock/` |
| Revisão por pares | `src/docs/qualidade/REVISAO-PARES.md` |
| Relatórios de cada execução do CI | GitHub Actions → artefatos (Surefire, JaCoCo, cobertura do front, BDD, carga, dependências) |
| Roteiro da demonstração | `src/docs/qualidade/ROTEIRO-DEMONSTRACAO.md` |

### Limitações conhecidas

- Registro manual de responsividade em 360/1280 px pendente (RNF-05; a verificação automatizada existe).
- 16 avisos moderados do npm apenas em ferramentas de desenvolvimento (`ng add`/`ng update`); correção exige Angular CLI 22.
- Carga medida em uma única máquina e um único nó da API.
