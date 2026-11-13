# Relatório de integração externa com WireMock — EX-01 a EX-04

| Item | Valor |
|---|---|
| Execução | 13/11/2026, `mvn clean verify` no back-end integrado (develop + as 9 branches de back-end), PostgreSQL 17 via Testcontainers |
| Resultado da suíte | **502 testes, 0 falhas, 0 erros**; JaCoCo 95,8% das linhas e 85,6% dos ramos (meta do build: 80/70) |
| Testes WireMock | **21 de 21 aprovados**: 15 no cliente (`NotificacaoExternaClientTest`, sem Spring) e 6 de ponta a ponta pela API (`NotificacaoExternaApiTest`, PostgreSQL real) |
| Ferramenta | WireMock 3.13.2 (`wiremock-standalone`, escopo de teste), porta dinâmica; nenhuma chamada a serviço real |
| Evidências | [evidencias-wiremock/](evidencias-wiremock/) — relatórios Surefire (XML) das duas classes |

## Integração testada e contrato

`NotificacaoExternaClient` envia cada notificação do sistema ao serviço de e-mail da instituição: `POST` JSON
(`para`, `nome`, `assunto`, `mensagem`, `codigoReserva`) com `Authorization: Bearer <token>`. Fica desligada sem
`aloca.notificacao.url`; nesse caso a notificação é só registrada no sistema (RF-11).

| Situação | Política |
|---|---|
| 2xx | Aceito. O corpo da resposta não faz parte do contrato e é ignorado |
| 4xx / 5xx | Falha registrada, sem nova tentativa; a reserva não é afetada |
| Tempo esgotado (`aloca.notificacao.timeout`, padrão 3 s) | Falha registrada; o pedido espera no máximo o tempo limite |
| Resposta quebrada (vazia, conexão reiniciada, dados aleatórios) | Falha registrada |
| Destinatário ausente | Não envia |
| Log | Só o código da reserva e o status HTTP ou o tipo da falha. Nunca o corpo da resposta externa, o token nem o e-mail |

O envio ocorre depois que a transação é gravada (`@TransactionalEventListener`), então a chamada HTTP não segura as
travas do banco e nenhuma falha externa desfaz a reserva.

## Casos do plano e resultado

| Caso | Teste | Resultado |
|---|---|---|
| **EX-01** resposta válida | `enviaANotificacaoComoJsonComOToken` (cabeçalhos e corpo JSON exatos); pela API: `reservaCriadaNotificaOSolicitantePeloServicoExterno`, `solicitacaoRestritaAvisaOResponsavel` | ✅ |
| **EX-02** tempo limite | `servicoLentoEstouraOTempoLimiteSemTravar` (serviço de 1,5 s, limite de 300 ms); pela API: `servicoLentoNaoDesfazAReserva` (serviço de 3 s, limite de 1 s: 201, reserva `APROVADA` e notificação interna gravada) | ✅ |
| **EX-03** 4xx/5xx sem expor dados | `erroDoServicoExternoEhTratadoSemExcecao` (400, 401, 404, 500, 503); `erroExternoNaoExpoePayloadNemCredencialNoLog`; pela API: `servicoExternoForaDoArNaoImpedeAReserva` | ✅ |
| **EX-04** resposta inválida e campos ausentes | `respostaDeSucessoComCorpoInvalidoEhAceita`; `respostaQuebradaEhTratadaSemExcecao` (EMPTY_RESPONSE, CONNECTION_RESET_BY_PEER, RANDOM_DATA_THEN_CLOSE); `notificacaoSemDestinatarioNaoEhEnviada`; pela API: `respostaQuebradaNaoImpedeAReserva` | ✅ |
| Sem efeito colateral | `reservaRecusadaPorConflitoNaoEnviaNotificacao`; `semUrlConfiguradaNaoChamaOServico` | ✅ |

## Defeitos encontrados pelos testes (vermelho → verde)

Os testes novos foram escritos antes da correção. No código anterior,
`mvn test -Dtest=NotificacaoExternaClientTest` terminou com **15 testes, 5 falhas e 1 erro**. Depois da correção,
os 15 passaram.

| ID | Achado no código anterior | Correção |
|---|---|---|
| DEF-INT-001 | Em 4xx/5xx o log gravava a mensagem da exceção, que traz o corpo da resposta externa, e o e-mail do destinatário (EX-03) | O log registra só a reserva e o status ou tipo da falha |
| DEF-INT-002 | Resposta com dados aleatórios gerava `IllegalArgumentException` (status −1), que escapava do tratamento por não ser `RestClientException` (EX-04) | Qualquer falha de execução vira falha tratada |
| DEF-INT-003 | Notificação sem destinatário era enviada ao serviço externo (EX-04) | Não envia e registra o aviso |

## Como executar

```bash
mvn -f backend/pom.xml test -Dtest='NotificacaoExterna*Test'
```

Docker é necessário só para `NotificacaoExternaApiTest` (PostgreSQL via Testcontainers). As duas classes também rodam
no `mvn verify` do CI.

## Limites

O contrato foi definido pela equipe para o serviço de e-mail da instituição; não há especificação externa publicada
para validar o formato. Não há nova tentativa automática nem fila: uma notificação externa que falha fica registrada
só no sistema e no log.
