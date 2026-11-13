# Relatório consolidado de testes — evidências e pendências

Revisão documental: 12/11/2026. Base do inventário: develop `611c9402c458d395126f014e720fc45a27ceeef8`. Este relatório consolida campanhas diferentes; **não é uma execução única nem uma homologação da release**.

## Resultados verificáveis

As conclusões das três execuções Actions abaixo foram consultadas nesta revisão. As contagens constam dos relatórios/documentos das entregas correspondentes.

| Evidência | Commit do incremento testado | Resultado | Incorporação na base |
|---|---|---|---|
| [CI 43330605457](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43330605457) | 833c0e6a4a3de351e2c3cd5076423a1fa13232e3 | 42 unitários isolados; 126 backend no merge temporário; execução aprovada | Unitários e integração incorporados |
| [CI 43332721433](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43332721433) | be004b330ad5b99189d71c0c1577c8f4576dfce4 | 32 HTTP; 158 backend no merge temporário; execução aprovada | PR #38 aberto, ainda fora da base |
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
| Concorrência | Teste MySQL existente; disputa HTTP de três alvos comprovada no #38 |
| HTTP caixa-preta | Aprovada na branch do #38; incorporação pendente |
| Frontend e BDD/E2E | Workflows existentes; vincular ao SHA candidato e preservar logs/capturas pertinentes |
| BDD funcionalidade nova | Ciclo antes/depois comprovado; não confundir com regressão do #33 ainda aberto |
| Testcontainers | Presente nas integrações MySQL |
| WireMock | Suíte não localizada no snapshot; não atendido por notificação simulada |
| Performance | Planos em outra branch; sem JTL/dashboard de carga de negócio consolidado |
| Segurança dinâmica | Casos de perfil/posse/entrada/erros cobertos; não equivalem a pentest |
| SonarCloud / dependências | Necessário relatório do SHA candidato e itens abertos; zero crítico não certificado por este documento |
| Cobertura JaCoCo | Artefatos existentes; percentuais devem ser extraídos do candidato, não de números antigos do README |
| Manual responsivo, instalação e reinício | Roteiros definidos no plano; evidência da campanha ainda pendente |
| RTM e defeitos | Precisam ser reconciliados com testes/SHAs atuais; documento de outra branch não fecha automaticamente a RTM |

## Decisão desta consolidação

**Plano unificado concluído; aprovação global da release inconclusiva.** Há evidência positiva nas modalidades automatizadas indicadas, mas WireMock, carga de negócio, recuperação/manual e análises da release ainda precisam de resultados. O #38 precisa ser incorporado para a suíte HTTP fazer parte da base integrada.

Este documento não altera retroativamente o parecer de um relatório antigo. A próxima campanha deve usar [REGISTRO-EXECUCAO](REGISTRO-EXECUCAO.md), executar o [plano](PLANO-TESTES.md) no SHA candidato e atualizar esta decisão com resultados reais.

## Próximas ações do pacote de qualidade

1. Incorporar entregas aprovadas e selecionar SHA da release.
2. Executar modalidades obrigatórias pendentes e preservar evidências.
3. Extrair linhas/branches do JaCoCo e análise SonarCloud/dependências desse SHA.
4. Fechar RTM crítica e registro de defeitos com requisito → risco → caso → evidência.
5. Emitir decisão final de qualidade e revisar por pares.
