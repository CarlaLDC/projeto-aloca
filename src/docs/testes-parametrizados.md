# Testes parametrizados — reservas

A classe [ReservaParametrizadaApiTest](../../backend/src/test/java/br/com/aloca/ReservaParametrizadaApiTest.java) usa JUnit Jupiter: `@ParameterizedTest`, `@MethodSource` e `@CsvSource`.

## Matriz de casos

| Grupo | Dados e fronteiras | Invocações | Verificações |
|---|---|---:|---|
| Sobreposição | 9 intervalos para cada alvo: sala, material e professor | 27 | HTTP 409/201; tipo e minutos do conflito; persistência |
| Término inválido | Dois horários iguais e dois invertidos | 4 | HTTP 400, mensagem e ausência de nova reserva |
| Funcionamento | Abertura, fechamento, período completo e limites excedidos | 6 | HTTP 201/400, mensagem e persistência |
| **Total** | **3 métodos parametrizados** | **37** | **API com aplicação e banco reais** |

A reserva de referência ocupa 10h–12h. Os casos incluem intervalo igual, contido, envolvente, sobreposição em cada extremidade, dois consecutivos e dois separados. Para material/professor, a segunda reserva usa outra sala, isolando o alvo do conflito.

Cada invocação cria seus próprios cadastros e usa um dia futuro exclusivo obtido da base de testes. As expectativas de status e minutos são dados explícitos da tabela, não calculadas pela função de produção.

## Ambiente e execução

A classe herda `ApiTesteBase`: Spring Boot + MockMvc e PostgreSQL 17 em Testcontainers. MockMvc despacha a requisição para a aplicação no processo; não substitui a persistência nem os serviços por mocks. Exige Java 21, Maven e Docker.

```bash
mvn -f backend/pom.xml -Dtest=ReservaParametrizadaApiTest test
```

Para a suíte completa e JaCoCo:

```bash
mvn -f backend/pom.xml clean verify
```

Os workflows existentes de backend e CI integrado descobrem a classe automaticamente. Não foi adicionado workflow duplicado. Os relatórios ficam em `backend/target/surefire-reports/`; no Actions, são publicados nos artefatos `backend-testcontainers-jacoco` e `qualidade-funcional-relatorios`.

## Rastreabilidade

- RF-04: criação válida e persistência; tentativa inválida sem criar reserva.
- RF-05 e regras críticas de tempo: intervalo final posterior, sobreposição e consecutividade de sala, professor e material.
- RF-13: erro de horário com mensagem compreensível.
- Estratégia obrigatória de qualidade: testes parametrizados com JUnit.

Este incremento testa comportamento existente; não altera regras de negócio nem substitui o teste paralelo de dupla reserva ou o ciclo BDD documentado separadamente. O resultado da execução deve ser consultado no PR correspondente.

## Resultado verificado em 12/11/2026

- PR: [#35](https://github.com/CarlaLDC/projeto-aloca/pull/35).
- Commit da implementação dos testes: [1277b5c](https://github.com/CarlaLDC/projeto-aloca/commit/1277b5c3ceb0f2dedd777cfd3423438e89ff3208).
- Execução: [Verificação backend — qualidade funcional](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43329164079).
- Classe parametrizada: **37 testes, zero falhas, zero erros e zero ignorados**.
- Suíte completa: **82 testes, zero falhas, zero erros e zero ignorados**; BUILD SUCCESS.
- [Artefato com Surefire e JaCoCo](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43329164079/artifacts/13994411546): extraia e consulte o arquivo `br.com.aloca.ReservaParametrizadaApiTest.txt` ou o XML correspondente em surefire-reports. O artefato tem retenção até 10/02/2027.

O resultado corresponde ao commit indicado; este registro documental posterior não altera o código dos testes.
