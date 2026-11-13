# API em caixa-preta — HTTP real

A classe [RegrasCriticasHttpTest](../../backend/src/test/java/br/com/aloca/http/RegrasCriticasHttpTest.java) inicia a aplicação em porta aleatória e envia requisições por socket usando java.net.http.HttpClient. Não usa MockMvc, mocks ou acesso direto a serviços, repositórios ou entidades para preparar/verificar casos.

SpringBootTest/Import são usados somente pelo harness para iniciar servidor e PostgreSQL 17/Testcontainers (MySQL 8.4 até a troca de banco no PR #45). O cliente conhece contratos HTTP/JSON, credenciais de demonstração e IDs retornados pela própria API. Persistência e locks são reais. Os dados de cada caso usam cadastros exclusivos, criados pela API administrativa.

## Matriz de cobertura

| Grupo | Casos | Verificações |
|---|---:|---|
| Autenticação ausente/inválida e login seguro | 3 | 401, mensagem JSON segura e sem distinguir conta inexistente de senha errada |
| Matriz de perfis | 7 | Permissões de administração, aprovações, movimentações, relatórios e consulta do professor |
| Entradas inválidas | 6 | Intervalo igual/invertido, passado, atividade vazia, sala ausente e JSON malformado; 400 sem nova reserva |
| Sobreposição e fronteiras | 3 | Sala/material/professor; 409 com minutos e tipo; consecutividade e outro dia aceitos |
| Concorrência HTTP | 3 | Duas requisições sincronizadas por barreira; exatamente 201 e 409; uma reserva persistida por disputa |
| Aprovação por perfil indevido | 2 | 403 sem modificar solicitação ou auditoria |
| Aprovação válida | 1 | Pendente → aprovada, ator/data/estados, notificação e segunda decisão impedida |
| Rejeição | 1 | Justificativa obrigatória, estado/motivo e liberação da agenda |
| Manutenção | 2 | Bloqueio na criação e na aprovação posterior; preservação de histórico; remoção libera |
| Alteração/cancelamento | 1 | Propriedade, motivo obrigatório, auditoria e liberação do período |
| Retirada/devolução | 1 | Autorização, sequência válida, duplicatas negadas, proteção contra alteração/cancelamento com material retirado |
| Relatórios | 1 | Horas, reservas e conflitos do recurso; exportação CSV |
| Recurso inexistente | 1 | 404 com mensagem segura |
| **Total** | **32** | **Requisições e respostas pela API real** |

A quantidade de casos não é a quantidade de requisições: cada caso valida um fluxo com múltiplas chamadas e consultas de confirmação. O teste de concorrência cobre três disputas simultâneas, uma por tipo de alvo. Não equivale a carga sustentada ou a todas as interleavings possíveis.

## Executar

Pré-requisitos: Java 21, Maven e Docker. Não é necessário iniciar Angular nem a API manualmente; o harness cria servidor e banco descartáveis.

```bash
mvn -f backend/pom.xml -Dgroups=http test
```

Suíte completa:

```bash
mvn -f backend/pom.xml clean verify
```

A tag http permite selecionar a suíte. Surefire publica o resultado em backend/target/surefire-reports; JaCoCo continua no pipeline existente. Os unitários continuam selecionáveis pela tag unitario, sem subir esta aplicação.

## Rastreabilidade

| Requisito | Evidência HTTP |
|---|---|
| RF-01 | Tokens, matriz de perfil, aprovação indevida e propriedade da reserva |
| RF-02 | Cadastros reais de recursos e docentes usados pelos casos |
| RF-04 | Criar, alterar, cancelar e consultar o resultado persistido |
| RF-05 | Sala, professor e material compartilhados com sobreposição/consecutividade |
| RF-06 | Disputa simultânea pela porta HTTP, um aceite, um conflito e consulta posterior |
| RF-07 | Aprovação obrigatória por responsável e rejeição documentada |
| RF-08 | Manutenção antes da criação e entre solicitação/aprovação |
| RF-09 | Retirada, devolução, protocolo e proteção contra duplicidade |
| RF-10 | Histórico com ator/instante/estados; recusas sem mudanças |
| RF-11 | Notificação simulada da aprovação consultada pela API |
| RF-12 | Totais observáveis do recurso e CSV |
| RF-13 | Mensagens compreensíveis em erros de API; não avalia responsividade visual |

## Limites

A suíte cobre regras críticas por HTTP, mas não substitui testes de interface, expiração temporal completa de JWT, varredura de vulnerabilidades, carga JMeter ou todas as buscas e combinações de dados. A proteção da reserva em uso é exercitada com material já retirado; a evolução pelo agendador permanece coberta pelas suítes existentes. Não há alteração de regra de produção neste incremento.

## Evidência verificada — 12/11/2026

PR: [#38](https://github.com/CarlaLDC/projeto-aloca/pull/38). Commit testado: [be004b3](https://github.com/CarlaLDC/projeto-aloca/commit/be004b330ad5b99189d71c0c1577c8f4576dfce4).

[Execução do CI integrado](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433):

| Verificação | Resultado |
|---|---|
| Job API — caixa-preta HTTP e MySQL | **32 testes**, zero falhas, erros e ignorados; BUILD SUCCESS |
| Suíte backend completa | **158 testes**, zero falhas, erros e ignorados; BUILD SUCCESS |
| Job de unitários isolados, frontend e gate | Aprovados |

Relatórios: [artefato exclusivo HTTP](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433/artifacts/13995432987) e [backend completo com JaCoCo](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433/artifacts/13996680426). Extraia o ZIP e consulte o TXT/XML de br.com.aloca.http.RegrasCriticasHttpTest em surefire-reports. A retenção dos artefatos vai até 10/02/2027.

A primeira execução teve 31 casos aprovados e uma falha no teste de relatório: a consulta incluía somente o dia futuro da reserva, mas o conflito é contabilizado pela data da tentativa. O commit be004b3 ajustou o período para incluir ambas as datas. As asserções de uma reserva, uma hora e um conflito foram mantidas. Não houve mudança de produção para obter o resultado aprovado.

A contagem de 158 corresponde à suíte completa da revisão testada; o presente registro documental posterior não altera o código executável.
