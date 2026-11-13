# Roteiro da apresentação e demonstração — Aloca 1.0.0-rc.1

Duração sugerida: **20 minutos** (5 de contexto, 10 de demonstração, 5 de qualidade). Cada parte indica quem fala;
dividam entre os integrantes.

## Antes de começar (preparação, 10 min antes)

1. Banco e API (PostgreSQL local, veja o README):

   ```bash
   docker compose -f backend/docker-compose.yml up -d
   ```

   ```bash
   cd backend && mvn spring-boot:run
   ```

2. Front, em outro terminal na raiz: `npm start` e abrir http://localhost:4200.
3. Deixar abertas em abas: o sistema, o Swagger (http://localhost:8080/swagger-ui.html), o GitHub (Actions e o PR #57)
   e o dashboard da carga (`testes/carga/resultados/20261113-campanha-03/dashboard/index.html`).
4. Para recomeçar com os dados de demonstração limpos: `docker compose -f backend/docker-compose.yml down -v` e subir de
   novo.

5. **Data da demonstração (D):** use um dia **daqui a 7 dias** (se cair num domingo, o dia seguinte). Os dados de
   demonstração ocupam os próximos 5 dias a partir de hoje (por exemplo, o Laboratório 02 tem um workshop das 14:00 às
   17:00 amanhã); com D livre, os passos abaixo funcionam exatamente como descritos. Roteiro ensaiado pela API em
   13/11/2026 com os dados de demonstração recém-criados.

Usuários (senha `aloca123`):

| Perfil | E-mail |
|---|---|
| Solicitante | ana.martins@aloca.edu.br |
| Solicitante | mariana.costa@aloca.edu.br |
| Professor | andre.lima@aloca.edu.br |
| Responsável | roberto.almeida@aloca.edu.br |
| Administrador | patricia.souza@aloca.edu.br |

## 1. Contexto (5 min)

- **Problema:** reservas de salas, laboratórios, professores e materiais feitas sem controle geram dupla reserva e
  conflito de horário.
- **Solução:** Angular 20 + API Spring Boot 4.1.2 (Java 21) + PostgreSQL 17. Uma reserva junta sala, professor e
  materiais; o sistema recusa qualquer sobreposição e registra cada mudança de estado.
- **Arquitetura** (`src/docs/arquitetura.md`): front → API REST com JWT → serviços → PostgreSQL; módulo de materiais
  reage a eventos das reservas.

## 2. Demonstração (10 min)

| # | Quem entra | Passo | O que mostrar |
|---|---|---|---|
| 1 | Ana (Solicitante) | Visão geral → Disponibilidade no dia D, filtrar "Laboratório" e capacidade 30 | Busca por tipo e capacidade; professores por competência ("Robótica") |
| 2 | Ana | Nova reserva: **Sala 204**, dia D 09:00–11:00, professor Lucas Oliveira, Notebook 08 | Reserva **aprovada automaticamente** (recursos sem restrição) e código RES-… |
| 3 | Mariana (Solicitante) | Nova reserva: **Sala 204**, dia D 10:00–12:00 | **Conflito (409)**: tela de conflito com a sobreposição de 60 min e **alternativas** sugeridas |
| 4 | Ana | Nova reserva: **Laboratório de informática 02** (restrito), dia D 14:00–16:00 | Fica **Solicitada**, aguardando aprovação |
| 5 | Roberto (Responsável) | Aprovações → aprovar a solicitação da Ana | Estado muda para Aprovada; notificação para a Ana |
| 6 | Ana | Minhas reservas → detalhe da reserva do laboratório | **Histórico de auditoria**: Solicitada → Aprovada, com ator (Roberto) e data |
| 7 | Patrícia (Administradora) | Administração → bloquear a **Sala 204** no dia D das 08:00 às 12:00 | A reserva aprovada da Ana é **cancelada com auditoria** e aviso (bloqueio de manutenção) |
| 8 | Patrícia | Relatórios → período de hoje até o dia D → exportar CSV | Utilização, horas e conflitos |
| 9 | (opcional) | Swagger → `POST /api/auth/login` e `GET /api/reservas/minhas` | API documentada e protegida por token |

Se algo falhar na hora, mostrar o mesmo comportamento pelos testes: `ReservaApiTest`, `CicloDeVidaApiTest` e
`BloqueioAfetaReservasApiTest`.

## 3. Qualidade e testes (5 min)

| Tema | O que mostrar | Onde |
|---|---|---|
| Pirâmide de testes | 502 testes no back-end (unitários, parametrizados, integração com PostgreSQL real, caixa-preta HTTP), 102 no front, BDD | Actions → CI; `SUITE-TESTES.md` |
| Cobertura | Back-end 95,9% / 85,8%; front 81,04% / 70,58%; meta 80/70 **obrigatória** no CI | Artefatos JaCoCo e Karma |
| Concorrência | 10 pedidos simultâneos pela mesma sala → exatamente 1 aceito (150/150 disputas sob carga) | `RELATORIO-CARGA.md`, dashboard |
| Carga (RNF-01) | 50 usuários: pesquisa P95 6–8 ms (meta 2 s), reservas P95 ≤ 117 ms (meta 3 s), 0% de erro | dashboard da campanha 03 |
| Defeito encontrado pela carga | DEF-PERF-001: 409 de professor/material de 1,2 s para 90 ms | `defeitos.md` |
| TDD/BDD | Bloqueio de manutenção (TDD + BDD) e aprovação com cadastro inativo (BDD antes/depois) | `bdd/EVIDENCIAS-*.md` |
| WireMock | Serviço externo de e-mail: sucesso, timeout, 4xx/5xx e resposta quebrada; 3 defeitos encontrados | `RELATORIO-WIREMOCK.md` |
| Rastreabilidade | Requisito → risco → teste → evidência; auditoria AUD-01 a AUD-15 | `RTM.md` |
| Análise estática e CI | SonarCloud com Quality Gate; workflows em todo PR; revisão por pares | PR #57 (checks), `REVISAO-PARES.md` |

## Perguntas prováveis

- **Por que Spring Boot 4.1.2 e não 3.x?** Versão atual, com Java 21; aceita pelo professor (NC-001 no registro de defeitos).
- **Como garantem que não há dupla reserva?** Travamento pessimista das linhas de sala, materiais e professor na mesma
  transação, testado com disputas paralelas (integração, HTTP e carga).
- **O que ficou de fora?** Registro manual de responsividade (há verificação automatizada em 360/1280 px), 16 avisos
  moderados do npm só em ferramenta de desenvolvimento e carga em um único nó.
