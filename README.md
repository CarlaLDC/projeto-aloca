# Aloca

Sistema de reservas de salas, professores e materiais sem conflito de horário.

- **Front-end:** Angular 20 (pasta raiz, código em `src/`)
- **Back-end:** Java 21 + Spring Boot 4.1 + H2 (dev) ou MySQL (perfil `mysql` e testes com Testcontainers) (pasta `backend/`)

## Como rodar

Pré-requisitos: **Java 21**, **Maven 3.9+**, **Node 22** e npm.

**1. Back-end** (porta 8080):

```bash
cd backend
mvn spring-boot:run
```

Na primeira execução o banco é criado em `backend/data/` e preenchido com dados de demonstração (datas relativas ao dia de hoje). Para recomeçar do zero, pare o servidor e apague a pasta `backend/data/`.

**2. Front-end** (porta 4200), em outro terminal na raiz do projeto:

```bash
npm install
npm start
```

Abra http://localhost:4200. O `proxy.conf.json` encaminha as chamadas `/api` para o back-end.

### Usuários de demonstração

Todos com a senha **`aloca123`** (as mesmas personas de `src/docs/personas`):

| E-mail | Perfil | Tela inicial |
|---|---|---|
| ana.martins@aloca.edu.br | Solicitante | Visão geral |
| mariana.costa@aloca.edu.br | Solicitante | Visão geral |
| roberto.almeida@aloca.edu.br | Responsável | Aprovações |
| patricia.souza@aloca.edu.br | Administrador | Relatórios |
| andre.lima@aloca.edu.br | Professor | Visão geral (agenda própria) |

### Console do banco

http://localhost:8080/h2-console · JDBC URL `jdbc:h2:file:./data/aloca` · usuário `sa` · sem senha.

### Rodar com MySQL (mesmo banco dos testes)

```bash
cd backend
docker compose up -d
mvn spring-boot:run -Dspring-boot.run.profiles=mysql
```

## API por tela

Todas as rotas (exceto o login) exigem o cabeçalho `Authorization: Bearer <token>`. Erros voltam como `{"mensagem": "...", "campos": {...}}`.

| Tela | Método e rota | Perfis | O que faz |
|---|---|---|---|
| 01 · Entrar | `POST /api/auth/login` | todos | Devolve o token JWT (8 h) e os dados do usuário |
| | `GET /api/auth/me` | todos | Usuário do token |
| | `POST /api/auth/recuperar-senha` | todos | Orientação de recuperação (feita pelo suporte da instituição) |
| 02 · Visão geral | `GET /api/painel` | todos | Reservas de hoje, recursos disponíveis, pendências e agenda do dia (inclui a agenda do professor, RF-14) |
| 03 · Disponibilidade | `GET /api/disponibilidade?data=&tipo=&busca=&capacidadeMinima=` | todos | Recursos com status, próximo horário livre, ocupações e janelas livres do dia |
| 04 · Nova reserva | `GET /api/reservas/opcoes` | todos | Salas, professores e materiais ativos |
| | `POST /api/reservas` | Solicitante, Responsável, Administrador | Cria a reserva: **201** se gravada, **409** com conflitos e alternativas |
| 05 · Conflito de horário | (corpo do **409** acima) | | Conflitos encontrados e até 3 alternativas livres |
| 06 · Minhas reservas | `GET /api/reservas/minhas` | todos | Reservas do usuário (e, para professor, aquelas em que está alocado) |
| 07 · Detalhe e auditoria | `GET /api/reservas/{codigo}` | dono, professor alocado, Responsável, Administrador | Dados e histórico completo |
| | `PUT /api/reservas/{codigo}` | dono | Altera e verifica conflitos de novo (409 se houver) |
| | `POST /api/reservas/{codigo}/cancelamento` | dono | Cancela com motivo obrigatório |
| 08 · Aprovações | `GET /api/aprovacoes` | Responsável | Pendentes, contadores do dia e checagem de disponibilidade |
| | `POST /api/aprovacoes/{codigo}/aprovar` | Responsável | Aprova (gera as movimentações dos materiais) |
| | `POST /api/aprovacoes/{codigo}/rejeitar` | Responsável | Rejeita com justificativa obrigatória |
| 09 · Retiradas e devoluções | `GET /api/movimentacoes?status=&busca=` | Responsável | Movimentações e contadores |
| | `POST /api/movimentacoes/{codigo}/retirada` | Responsável | Registra a retirada |
| | `POST /api/movimentacoes/{codigo}/devolucao` | Responsável | Registra a devolução e gera o protocolo |
| 10 · Administração | `GET/POST /api/admin/recursos`, `PUT /api/admin/recursos/{id}`, `POST .../{id}/desativar` e `/ativar` | Administrador | Cadastro de salas, laboratórios e equipamentos (RF-02) |
| | `GET/POST /api/admin/professores`, `PUT .../{id}`, `POST .../{id}/desativar` e `/ativar` | Administrador | Cadastro de professores |
| | `GET/POST /api/admin/bloqueios`, `DELETE /api/admin/bloqueios/{id}` | Administrador | Bloqueios de manutenção (RF-08) |
| 11 · Relatórios | `GET /api/relatorios?inicio=&fim=` | Responsável, Administrador | Utilização, carga horária e conflitos evitados por recurso e professor |
| | `GET /api/relatorios/csv?inicio=&fim=` | Responsável, Administrador | Mesmo relatório em CSV (`;`, abre no Excel) |
| Topo das telas | `GET /api/notificacoes`, `POST /api/notificacoes/lidas` | todos | Notificações (o envio é simulado, como permite o RF-11) |

> A tela **10 · Administração** está disponível em `/administracao`, com guard para Administrador.

## Regras implementadas

- **Conflitos (RF-05):** a mesma sala, o mesmo material ou o mesmo professor não podem estar em reservas que se sobrepõem. Reservas consecutivas (uma termina às 11:00 e a outra começa às 11:00) são permitidas. Contam como ocupadas as reservas *Solicitada*, *Aprovada* e *Em uso*.
- **Reservas simultâneas (RF-06):** antes de checar conflitos, a transação trava as linhas da sala, dos materiais e do professor (`SELECT ... FOR UPDATE`). Dois pedidos ao mesmo tempo são processados um depois do outro, e o segundo é recusado. O teste `ConcorrenciaReservaTest` comprova isso 5 vezes seguidas.
- **Aprovação (RF-07):** se a sala ou algum material for restrito, a reserva fica *Solicitada* até o Responsável decidir. Sem recurso restrito, ela é aprovada automaticamente. O Administrador **não** aprova (persona 3).
- **Manutenção (RF-08):** recurso bloqueado não pode ser reservado no período.
- **Histórico (RF-10):** toda mudança registra usuário, data e hora, estado anterior e novo. Reservas em uso não podem ser alteradas nem canceladas.
- **Ciclo automático:** a cada minuto, reservas aprovadas passam a *Em uso* no início e a *Concluída* no fim.
- **Materiais (RF-09):** o módulo de materiais reage a eventos das reservas (`ReservaEventos`). A aprovação de uma reserva com materiais cria as movimentações. *Devolução pendente* aparece quando o material está em uso depois do horário previsto.
- **Persistência (RNF-04):** o H2 grava em arquivo, então os dados continuam após reiniciar o servidor.

## Testes

**Ambiente realista:** os testes de integração não usam mocks nem banco em memória. Cada execução sobe um MySQL real em contêiner com Testcontainers (`BancoMySqlTeste`), então persistência, travas (`SELECT ... FOR UPDATE`) e o teste de concorrência rodam no mesmo banco usado no perfil `mysql`. No GitHub Actions o Docker já vem instalado no runner `ubuntu-latest`.

```bash
# Back-end: 43 testes de integração + relatório de cobertura em backend/target/site/jacoco
# Exige Docker: os testes sobem um MySQL 8.4 em contêiner (Testcontainers).
cd backend && mvn verify

# Front-end: 23 testes (Karma)
npm test -- --watch=false --browsers=ChromeHeadless
```

Cobertura atual do back-end: **93,9% das linhas e 75,1% dos ramos** (meta do RNF-06: 80% e 70%).

O GitHub Actions (`.github/workflows/main.yml`) roda os dois conjuntos a cada push.

## Estrutura do back-end

```
backend/src/main/java/br/com/aloca/
├── api/            controllers REST (um por tela) e DTOs
├── config/         dados de demonstração (um arquivo por módulo) e hash de senha
├── dominio/        entidades JPA (Reserva, Recurso, Professor, Bloqueio, Movimentacao...)
├── repositorio/    consultas ao banco (conflitos, travas)
├── seguranca/      login JWT e permissões por perfil
└── servico/        regras de negócio
```

## Configuração

Em `backend/src/main/resources/application.properties`:

- `aloca.jwt.segredo`: segredo do token. **Troque em produção** com a variável `ALOCA_JWT_SEGREDO` (mínimo de 32 caracteres).
- `aloca.dados-demo=false`: não cria os dados de demonstração.
- `aloca.fuso-horario`: padrão `America/Sao_Paulo`.

## Diagramas e decisões de arquitetura

Consulte [a arquitetura da implementação atual](src/docs/arquitetura.md) para contexto, contêineres, componentes e sequências de autenticação, criação e concorrência de reservas, aprovação, alteração/cancelamento e materiais. O documento identifica o commit inspecionado, as fontes no código e as diferenças em relação ao enunciado.

As propostas anteriores foram preservadas em [arquitetura-historica.md](src/docs/arquitetura-historica.md). A [ADR-0005](src/docs/adr/ADR-0005-arquitetura-implementada.md) registra as decisões observadas no código.

## Defeitos, correções e regressões

Consulte o [registro priorizado](src/docs/defeitos.md), com causas, commits, testes de regressão, evidências históricas e pendências de entrega. Novas ocorrências devem usar o [template de defeito](.github/ISSUE_TEMPLATE/defeito.md).
