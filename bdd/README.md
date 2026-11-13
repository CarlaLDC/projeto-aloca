# BDD integrado — Aloca

Cucumber + Playwright testam o Angular ligado à API Spring Boot e a um banco PostgreSQL descartável por execução (não são mocks). Os cenários cobrem login JWT, autorização, disponibilidade, criação persistida e detecção de sobreposição HTTP 409.

## Execução
Requisitos: Java 21, Maven, Node 22, Chromium e Docker. Na raiz, suba o banco descartável dos testes (PostgreSQL na memória do contêiner, porta 5433):

```bash
docker compose -f backend/docker-compose.yml --profile e2e up -d postgres-e2e
```

Em terminais distintos:

1. `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:postgresql://localhost:5433/aloca_e2e"`
2. `npm ci && npm start -- --proxy-config proxy.conf.json`
3. `npm install --prefix bdd && npx --prefix bdd playwright install chromium && cd bdd && npm test`

O CI anexa relatórios Cucumber HTML/JSON e capturas em falhas. O banco é isolado (no CI, um serviço `postgres:17-alpine`), mas a aplicação precisa estar realmente em execução. Para zerar o banco local: `docker compose -f backend/docker-compose.yml --profile e2e down`.

**Limitações:** execução local e CI ainda precisam ser confirmadas; os cenários demonstram integração, mas não substituem testes de concorrência paralela nem cobertura total de regras de negócio. Credenciais são somente da base demonstrativa.

## Funcionalidade nova desenvolvida com BDD

O bloqueio da aprovação após desativação de sala, material ou professor foi especificado e executado antes da implementação. Consulte [o ciclo BDD comprovado](EVIDENCIAS-APROVACAO-INATIVOS.md), com commits, execução com falha, execução aprovada (3 cenários / 27 passos), relatórios originais versionados e requisitos correspondentes.

## Aprovação e bloqueio — evidências da técnica

Os [cenários de aprovação/manutenção](features/aprovacao-bloqueio.feature) cobrem cinco casos executáveis de autorização, aprovação, bloqueio e auditoria com API real. Consulte [EVIDENCIAS-BDD.md](EVIDENCIAS-BDD.md) para rastreabilidade, comandos, relatórios e a distinção entre cobertura de regressão e prova de desenvolvimento de uma funcionalidade nova guiado por BDD. A ordem histórica teste → implementação não foi comprovada para essas regras existentes.
