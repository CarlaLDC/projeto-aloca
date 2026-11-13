# Relatório de análise estática — SonarQube Cloud

Data do registro: 13/11/2026 (America/Sao_Paulo).

## Natureza e fontes da evidência

Este documento registra os resultados exibidos pelo SonarQube Cloud (SonarCloud) na captura de tela fornecida pela equipe às 19h55 de 13/11/2026. É uma transcrição documental dos indicadores observados, e não um PDF exportado oficialmente pela plataforma ou uma extração integral de sua API.

- Projeto: `CarlaLDC_projeto-aloca`.
- [Painel do SonarCloud](https://sonarcloud.io/project/overview?id=CarlaLDC_projeto-aloca).
- Captura de origem: “Captura de Tela 2026-11-13 às 19.55.34.png”, fornecida pela equipe na conversa de preparação deste relatório. A imagem não está incorporada neste arquivo.
- O painel indicava “Last analysis 4 minutes ago” e projeto privado no momento da captura. O acesso ao link pode exigir autenticação e permissão.
- O painel é dinâmico: os valores abaixo representam a captura, não resultados de futuras análises.

## Resultados exibidos no painel

| Indicador | Resultado observado |
|---|---|
| Quality Gate | Passed — All conditions passed |
| Issues, código total, estados Open e Confirmed | 1 |
| Duplicações, código total | 0,4% |
| Cobertura, código total | No data to display yet — sem dados disponíveis |
| Avisos da análise | 2 |
| Linhas de código exibidas | 7,1 mil |

A captura não mostra o conteúdo da issue, a severidade, a regra, o arquivo afetado nem o texto dos dois avisos. Esses detalhes permanecem pendentes de consulta no painel. Os indicadores de segurança parcialmente fora da área visível não foram transcritos.

## Execução do GitHub Actions conferida

- [Workflow SonarCloud, execução #40](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43522495873).
- Branch: `main`.
- Commit da execução: `39ee537a3ea29be6d9089eb0e6648d2dd384f8f9`.
- Evento: push após merge do PR #65.
- Início: 13/11/2026 às 19h50min16s; término registrado: 19h53min12s (horário de São Paulo).
- Conclusão global da execução: success.

| Etapa | Conclusão consultada na API do GitHub |
|---|---|
| Front-end: testes e cobertura (lcov) | success |
| Back-end: testes e cobertura (Testcontainers + JaCoCo) | success |
| Análise SonarCloud | skipped |
| Quality Gate | skipped |

**A execução verde não comprova o envio de cobertura ao SonarCloud nem a verificação do Quality Gate pelo CI.** As duas etapas correspondentes foram puladas. O status Passed documentado acima vem do painel mostrado pela equipe, não da etapa Quality Gate desta execução.

Também não foi possível confirmar, apenas pela captura, o SHA e a branch da análise exibida no SonarCloud. O commit acima identifica a execução do GitHub e não deve ser atribuído automaticamente à análise do painel.

## Configuração e limites da cobertura

O workflow `.github/workflows/sonar.yml` condiciona o scanner e o Quality Gate a `vars.SONAR_CI_ANALISE == 'true'`. Nesta execução, essa condição não habilitou as etapas.

O arquivo `sonar-project.properties` configura as seguintes entradas para análise por CI:

- Código de produção: `src/app` e `backend/src/main/java`.
- Cobertura Java: `backend/target/site/jacoco/jacoco.xml`.
- Cobertura TypeScript: `coverage/projeto-aloca/lcov.info`.
- Há exclusões explícitas de cobertura para configuração, rotas e configuração do Angular.

Esses caminhos configurados não comprovam que os relatórios foram importados. O painel capturado continua sem dados de cobertura. Ausência de dados não equivale a 0% e não comprova atendimento às metas de 80% de linhas e 70% de ramos do RNF-06.

O escopo acima descreve a configuração do repositório para CI; não é comprovação do escopo efetivamente processado pela análise exibida no painel.

## Pendências para completar a evidência

1. Consultar e registrar a issue e os dois avisos no SonarCloud.
2. Confirmar branch, revisão e horário da análise do painel.
3. Para importar cobertura por CI, desativar Automatic Analysis no SonarCloud, conferir o segredo SONAR_TOKEN e definir a variável de repositório SONAR_CI_ANALISE como true, conforme as instruções do workflow.
4. Executar novamente o workflow e confirmar que scanner e Quality Gate não foram pulados.
5. Conferir a cobertura importada e preservar a nova evidência com seu commit e data.

Este registro comprova os indicadores visíveis no painel e o resultado das etapas consultadas no GitHub. O Passed deve ser interpretado segundo as condições configuradas no SonarCloud, sem substituir as demais evidências de qualidade e os requisitos acadêmicos.
