# Registro de execução de testes

Modelo de campanha; preencher somente com fatos observados. Manter um registro por execução/ambiente. Duplicar para um arquivo com identificador próprio quando executar, sem substituir este modelo.

## Identificação

| Campo | Preenchimento |
|---|---|
| ID / data / executor | A preencher |
| Branch / SHA head / SHA checkout | A preencher |
| Modalidade / casos / requisitos | A preencher |
| Ambiente e versões | Java, Maven, Node, SO, banco, Docker, navegador/JMeter quando aplicável |
| Configuração e massa | IDs sintéticos, fuso, perfis, volume e limpeza; sem credenciais |
| Comando exato | A preencher |
| Workflow / run / job | Links a preencher ou execução local identificada |
| Início / fim / duração | A preencher |

## Resultado

| Métrica | Valor observado |
|---|---|
| Total / aprovados / falhas / erros / ignorados | A preencher; cenários/passos separados de testes JUnit |
| Cobertura de linhas / branches | Valor e escopo / ferramenta / SHA, quando aplicável |
| Bugs/vulnerabilidades por gravidade | Relatório e itens abertos, quando aplicável |
| Carga | Usuários, ramp-up, duração, think time, P95 por operação, throughput, erros esperados/inesperados |
| Persistência/concorrência | Resultado observável e correlação das disputas, quando aplicável |
| Manual | Viewports, navegador, caso e capturas, quando aplicável |

## Evidências e defeitos

| Caso / requisito | Esperado | Obtido / status | Evidência | Defeito / correção / reteste |
|---|---|---|---|---|
| A preencher | A preencher | Não executado | A preencher | A preencher |

Guardar Surefire/Cucumber/JTL/HTML/logs/capturas pertinentes e registrar nome, link, SHA e expiração. Não considerar presença de artefato como sucesso. Para arquivo copiado, preservar o original e opcionalmente registrar hash.

## Decisão e limites

- Decisão: aprovado / aprovado com restrições / reprovado / inconclusivo.
- Critérios do plano satisfeitos e não satisfeitos: a preencher.
- Bloqueios e casos não executados: a preencher.
- Riscos residuais, responsável e aceite explícito: a preencher.
- Revisor e data: a preencher, sem presumir revisão.
- Próxima execução/regressão necessária: a preencher.
