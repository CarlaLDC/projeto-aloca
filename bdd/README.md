# BDD integrado — Aloca

Cucumber + Playwright testam o Angular ligado à API Spring Boot e a um banco H2 isolado por execução CI (não são mocks). Os cenários cobrem login JWT, autorização, disponibilidade, criação persistida e detecção de sobreposição HTTP 409.

## Execução
Requisitos: Java 21, Maven, Node 22, Chromium. Em terminais distintos:

1. `cd backend && mvn spring-boot:run -Dspring-boot.run.arguments="--spring.datasource.url=jdbc:h2:mem:bdd;DB_CLOSE_DELAY=-1"`
2. `npm ci && npm start -- --proxy-config proxy.conf.json`
3. `npm install --prefix bdd && npx --prefix bdd playwright install chromium && cd bdd && npm test`

O CI anexa relatórios Cucumber HTML/JSON e capturas em falhas. O banco é isolado, mas a aplicação precisa estar realmente em execução.

**Limitações:** execução local e CI ainda precisam ser confirmadas; os cenários demonstram integração, mas não substituem testes de concorrência paralela nem cobertura total de regras de negócio. Credenciais são somente da base demonstrativa.

## Funcionalidade nova desenvolvida com BDD

O bloqueio da aprovação após desativação de sala, material ou professor foi especificado e executado antes da implementação. Consulte [o ciclo BDD comprovado](EVIDENCIAS-APROVACAO-INATIVOS.md), com commits, execução com falha, execução aprovada (3 cenários / 27 passos), relatórios originais versionados e requisitos correspondentes.
