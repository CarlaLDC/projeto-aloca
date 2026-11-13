# Testes unitários isolados do backend

## Escopo

As classes em `backend/src/test/java/br/com/aloca/unit` usam JUnit Jupiter e a tag `unitario`. Não herdam ApiTesteBase, não usam SpringBootTest/MockMvc/Testcontainers e não abrem conexão com banco.

Serviços são construídos com `new`; Mockito substitui repositórios, notificações, mapeador e publicador de eventos apenas dentro destes testes unitários. Reserva, Recurso, Usuario, HistoricoReserva e Movimentacao são objetos reais. Um relógio fixo em America/Sao_Paulo evita depender do horário da máquina.

| Classe | Casos | Regras cobertas |
|---|---:|---|
| ReservaUnitTest | 6 | Aprovação por restrição da sala ou de qualquer material; auditoria preservando ator, instante e sequência de estados |
| ReservaServiceUnitTest | 19 | Término válido, passado, início ultrapassado, material duplicado; fronteiras do ciclo; janela de ausência; perfil e veto de material em uso |
| MovimentacaoServiceUnitTest | 17 | Estados que permitem retirada; atraso; estados encerrados; devolução inválida; veto e cancelamento seletivo |
| **Total** | **42** | **Regras executadas sem infraestrutura** |

Os testes verificam resultados de negócio e preservação de estado/histórico em recusas. Não são testes de getters/setters. As datas são fixas, e as dependências externas não executam operações reais.

## Executar somente unitários — sem Docker

```bash
mvn -f backend/pom.xml -Dgroups=unitario test
```

Pré-requisitos: Java 21 e Maven. Dependências Maven precisam estar disponíveis ou acessíveis. O filtro groups seleciona a tag JUnit e exclui testes de integração.

## Executar a suíte completa

```bash
mvn -f backend/pom.xml clean verify
```

A suíte completa inclui os unitários e as integrações existentes, que exigem Docker/PostgreSQL em Testcontainers. Ela gera Surefire e JaCoCo como antes.

## CI

O workflow de CI integrado contém um job exclusivo de unitários, executando o comando com groups=unitario e publicando o artefato backend-unitarios. Esse job não inicia Docker nem banco. O gate considera também esse job. Os jobs completos continuam verificando integração com persistência real.

## Rastreabilidade e limites

- RF-04 e regras de tempo: rejeitar criação inválida antes de acessar persistência.
- RF-07: recurso restrito exige aprovação mesmo quando a restrição está em um material.
- RF-09: retirada/devolução e atraso de material.
- RF-10: auditoria das transições, proteção da reserva em uso, cancelamento seletivo.
- RF-01: regra de perfil para registrar ausência no serviço; não substitui autorização JWT/HTTP.
- Estratégia de qualidade: unitários isolados com JUnit e mocks somente nas dependências externas.

As consultas de sobreposição, locks, transações, persistência de auditoria e entrega real dos eventos continuam sob responsabilidade dos testes de integração e BDD. Mockito não é usado como evidência de concorrência ou de integridade do banco. Nenhuma regra de produção foi alterada nesta entrega.

## Evidência de execução — 12/11/2026

PR: [#37](https://github.com/CarlaLDC/projeto-aloca/pull/37). Commit dos testes: [833c0e6](https://github.com/CarlaLDC/projeto-aloca/commit/833c0e6a4a3de351e2c3cd5076423a1fa13232e3).

[Execução do CI integrado](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457):

| Execução | Resultado verificado |
|---|---|
| Job Backend — regras unitárias sem banco | 42 testes; zero falhas, erros e ignorados; BUILD SUCCESS |
| ReservaServiceUnitTest | 19 casos aprovados |
| ReservaUnitTest | 6 casos aprovados |
| MovimentacaoServiceUnitTest | 17 casos aprovados |
| Suíte backend completa no merge temporário do PR | 126 testes; zero falhas, erros e ignorados; BUILD SUCCESS |
| Gate integrado | Aprovado |

Relatórios: [somente unitários](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457/artifacts/13993474881) e [suíte completa / JaCoCo](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457/artifacts/13993778749). Os artefatos expiram em 10/02/2027. Nos XMLs/TXTs Surefire, procure as três classes do pacote br.com.aloca.unit.

O job exclusivo executou somente essas três classes. Os imports de tipos Spring usados pelos serviços não significam inicialização de contexto; não há SpringBootTest nem conexão com infraestrutura nesse job. O resultado acima pertence à execução e ao commit indicados, anteriores a este registro documental.
