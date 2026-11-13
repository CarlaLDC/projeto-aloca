# Relatório consolidado de testes — evidências e pendências

Revisão documental: 12/11/2026. Base do inventário: develop `0d3ef6a6e2cb56facc8475ba696ef3dbee55a295`. Este relatório consolida campanhas diferentes; **não é uma execução única nem uma homologação da release**.

## Resultados verificáveis

As conclusões das três execuções Actions abaixo foram consultadas nesta revisão. As contagens constam dos relatórios/documentos das entregas correspondentes.

| Evidência | Commit do incremento testado | Resultado | Incorporação na base |
|---|---|---|---|
| [CI 43330605457](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457) | 833c0e6a4a3de351e2c3cd5076423a1fa13232e3 | 42 unitários isolados; 126 backend no merge temporário; execução aprovada | Unitários e integração incorporados |
| [CI 43332721433](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433) | be004b330ad5b99189d71c0c1577c8f4576dfce4 | 32 HTTP; 158 backend no merge temporário; execução aprovada | HTTP incorporada pelo PR #38 |
| [BDD 43327149247](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43327149247) | 704c33979dd1b41606ce007ebd4be31854f127a5 | 3 cenários / 27 passos aprovados | Incremento do #34 incorporado |

As 42 invocações unitárias estão incluídas na contagem completa de 126; os 32 HTTP estão incluídos na de 158. As contagens não devem ser somadas entre campanhas. Actions executa o merge temporário do PR; o SHA head não é necessariamente o checkout do job.

Parametrizados: [documentação com execução](../testes-parametrizados.md), 37 invocações aprovadas em campanha específica. BDD antes/depois: [registro original](../../../bdd/EVIDENCIAS-APROVACAO-INATIVOS.md) e JSONs versionados. As falhas técnicas de preparação/asserção estão declaradas nesse registro; só a falha real de comportamento é usada como evidência de desenvolvimento guiado.

## Artefatos para consulta

| Pacote | Link |
|---|---|
| Unitários isolados | [backend-unitarios](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457/artifacts/13993474881) |
| Backend completo/JaCoCo (126) | [backend-testcontainers-jacoco](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457/artifacts/13993778749) |
| HTTP real (32) | [backend-api-http](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433/artifacts/13995432987) |
| Backend completo/JaCoCo (158) | [backend-testcontainers-jacoco](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433/artifacts/13996680426) |
| BDD aprovado | [artefato de aprovação](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43327149247/artifacts/13992124950) |
| BDD falha antes da regra | [artefato de falha](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43326822779/artifacts/13993051792) |

A retenção registrada para esses artefatos vai até 10/02/2027. Os JSONs do ciclo BDD estão preservados no Git. Baixar/preservar os demais relatórios no pacote de entrega antes da expiração, sem tokens ou dados reais.

## Cobertura por modalidade e lacunas

| Modalidade | Situação da evidência |
|---|---|
| Unitários isolados | Resultado específico aprovado e classes incorporadas |
| Parametrizados e integração | Suítes e campanhas aprovadas disponíveis; repetir para o SHA candidato |
| Concorrência | ConcorrenciaReservaTest com PostgreSQL; disputa HTTP de três alvos comprovada no #38; 150/150 disputas sob carga (campanha 03) |
| HTTP caixa-preta | Aprovada e incorporada pelo #38 |
| Frontend e BDD/E2E | Front: 102 testes, 81,04% linhas / 70,58% ramos, meta 80/70 obrigatória no CI; BDD integrado com PostgreSQL aprovado no CI, incluindo os 5 cenários de aprovação/bloqueio do #33 |
| BDD funcionalidade nova | Ciclo antes/depois comprovado; os cenários de regressão do #33 foram incorporados e aprovados no CI |
| Testcontainers | PostgreSQL 17 em todas as integrações (`PostgresTestcontainer`) |
| WireMock | EX-01 a EX-04 aprovados: 21 testes, 3 defeitos encontrados e corrigidos (DEF-INT-001 a 003) ([RELATORIO-WIREMOCK](RELATORIO-WIREMOCK.md)) |
| Performance | PE-01/PE-02/PE-03 aprovados no PostgreSQL ([RELATORIO-CARGA](RELATORIO-CARGA.md)): 50 usuários, 43,4 req/s, P95 da pesquisa 6–8 ms, P95 total 17 ms, P99 61 ms, 0% erro inesperado, 150/150 disputas com uma única aceita; DEF-PERF-001 corrigido e validado |
| Segurança dinâmica | Casos de perfil/posse/entrada/erros cobertos; não equivalem a pentest |
| SonarCloud / dependências | SonarCloud integrado; Quality Gate aprovado nos PRs #54, #55, #57 e #58 (o #56 reprovou e foi corrigido pelo #57); `npm audit --omit=dev` = 0 (16 avisos moderados só em ferramentas de desenvolvimento). Pendente: análise de dependências do back-end |
| Cobertura JaCoCo | develop `31afdd4`: 502 testes, 95,9% linhas / 85,8% ramos; meta 80/70 obrigatória no `mvn verify` |
| Manual responsivo, instalação e reinício | Reinício: verificado (`ReinicioPersistenciaTest`). Responsivo: automatizado em 360/1280 px no candidato; falta o registro manual. Instalação: instruções com PostgreSQL no README |
| RTM e defeitos | Reconciliados em 13/11/2026: RTM com carga, WireMock e auditoria de todas as transições (AUD-01 a AUD-15); defeitos DEF-PERF-001, DEF-INT-001 a 003 validados e NC-001 encerrado |

## Decisão desta consolidação

**Aprovado com restrições documentadas (develop `31afdd4`, 13/11/2026).** Todas as modalidades automatizadas do plano têm evidência aprovada: unitários, parametrizados, integração com PostgreSQL, caixa-preta HTTP, concorrência, auditoria de todas as transições, reinício, WireMock, BDD, front com cobertura 80/70 e carga PE-01/02/03. Restrições: registro manual de responsividade (RNF-05), análise de dependências do back-end (RNF-07) e revisão do PR #28. A suíte HTTP faz parte da base integrada pelo #38.

Este documento não altera retroativamente o parecer de um relatório antigo. A próxima campanha deve usar [REGISTRO-EXECUCAO](REGISTRO-EXECUCAO.md), executar o [plano](PLANO-TESTES.md) no SHA candidato e atualizar esta decisão com resultados reais.

## Próximas ações do pacote de qualidade

1. Incorporar entregas aprovadas e selecionar SHA da release.
2. Executar modalidades obrigatórias pendentes e preservar evidências.
3. Extrair linhas/branches do JaCoCo e análise SonarCloud/dependências desse SHA.
4. Fechar RTM crítica e registro de defeitos com requisito → risco → caso → evidência.
5. Emitir decisão final de qualidade e revisar por pares.
