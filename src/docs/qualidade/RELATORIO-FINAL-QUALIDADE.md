# Relatório final de qualidade — Aloca

## 1. Identificação e parecer

| Campo | Valor |
|---|---|
| Projeto | Organização de Recursos — Aloca |
| Data da avaliação | 13/11/2026 (America/Sao_Paulo) |
| Repositório | https://github.com/CarlaLDC/projeto-aloca |
| Base avaliada | develop `f8dc43c51944c60c9a2ffa41cede77eba3db704b` |
| Método | Consolidação documental, inspeção da árvore e consulta de resultados no GitHub; nenhuma nova execução local nesta avaliação |
| Parecer | **Aprovado com restrições como consolidação das campanhas existentes; homologação integral do SHA final ainda inconclusiva** |
| Revisão deste relatório | Pendente de integrante independente; assistência automatizada na elaboração |

A aplicação possui evidências de testes funcionais, autorização, persistência, concorrência, auditoria, integração externa, interface e carga. As campanhas documentadas atingem as metas de cobertura e não observaram dupla reserva nas disputas executadas. Entretanto, resultados de commits diferentes não equivalem a uma campanha única do candidato final. O encerramento da entrega exige os resultados do SHA selecionado, triagem dos relatórios de segurança e aceite das restrições abaixo.

Este é o relatório geral de qualidade solicitado pelo enunciado. [RELATORIO-TESTES](RELATORIO-TESTES.md) consolida testes; [RELATORIO-CARGA](RELATORIO-CARGA.md) trata desempenho. O [relatório legado de performance](../../../testes/performance/relatorio-final.md) pertence a um snapshot anterior e não descreve a situação atual.

## 2. Escopo e critérios

Avaliação orientada aos 14 itens do enunciado, regras críticas e entregáveis. A numeração abaixo é **a do enunciado**, pois os IDs internos diferem: interface está em RF-13/RNF-05, documentação em RNF-09 e RF-14 interno é a agenda do professor.

“Evidência documentada” significa resultado registrado em campanha identificada; não certifica todos os cenários nem atribui uma execução histórica ao SHA atual. “Pendente” indica ausência de comprovação suficiente nesta avaliação.

## 3. Atendimento funcional

| Item do enunciado | Evidência principal | Avaliação e limites |
|---|---|---|
| 1. Autenticação e autorização | AutenticacaoTest, UsuariosApiTest, RegrasCriticasHttpTest; [RTM](../../../RTM.md), [candidato](CANDIDATO.md) | Evidência documentada de perfis, posse, revogação de sessão e recusa de acesso indevido |
| 2. Cadastro/consulta de salas, professores e materiais | AdminRecursosTest; RTM | Evidência documentada dos cadastros e restrição administrativa |
| 3. Pesquisa por tipo, capacidade, localização, competência e disponibilidade | DisponibilidadeApiTest, QualidadeFuncionalApiTest; candidato | API e capacidade na interface verificadas nos cenários registrados; não certifica toda combinação de filtros |
| 4. Criar, alterar e cancelar reservas | ReservaApiTest, suíte HTTP e navegador | Evidência documentada de fluxo, revalidação e acesso somente às próprias reservas |
| 5. Sobreposição, inclusive professor | ReservaParametrizadaApiTest e suíte HTTP | Evidência documentada para sala, material, professor, fronteiras e consecutividade |
| 6. Proteção simultânea | ConcorrenciaReservaTest, suíte HTTP e JMeter PE-03 | 150/150 disputas da campanha 03 com uma única aceita e confirmada no PostgreSQL |
| 7. Aprovação de restritos | AprovacaoApiTest e BDD de inativos | Perfil responsável e revalidação cobertos; desativação simultânea à aprovação não certificada |
| 8. Manutenção | ReservaApiTest, BloqueioAfetaReservasApiTest | Bloqueio da criação/aprovação e efeitos em reservas cobertos |
| 9. Retirada/devolução | MaterialApiTest e HTTP | Protocolo, histórico, perfil e recusa de duplicidade cobertos |
| 10. Histórico auditável | CicloDeVidaApiTest, ReinicioPersistenciaTest; AUD-01 a AUD-15 na RTM | Transições e persistência verificadas nas campanhas; auditoria administrativa tem descrição, sem snapshot completo antes/depois |
| 11. Notificação | NotificacaoExternaClientTest e NotificacaoExternaApiTest | 21 testes WireMock documentados, inclusive falhas externas; [relatório](RELATORIO-WIREMOCK.md) |
| 12. Utilização, carga horária e conflitos evitados | RTM e testes de relatórios nela vinculados | Evidência documentada no escopo dos casos; não representa validação analítica de qualquer volume/período |
| 13. Interface responsiva e erros claros | Navegador em 360×900 e 1280×900; tratamento seguro de erros | Evidência automatizada e inspeção parcial registradas; avaliação manual completa pendente |
| 14. Documentação da API/fluxos | [README](../../../README.md), OpenAPI/Swagger configurado no POM, documentos de requisitos | Entregáveis presentes; confirmar acesso ao Swagger na demonstração final |

## 4. Regras críticas e limitadores de nota

| Regra | Base de comprovação | Resultado consolidado |
|---|---|---|
| Fim posterior ao início | Parametrizados e HTTP | Intervalos iguais/invertidos recusados nos casos documentados |
| Não sobrepor sala/material/professor | Parametrizados, integração, HTTP | Conflitos recusados; fronteiras válidas cobertas |
| Uma única aceita em simultaneidade | Testcontainers, HTTP, PE-03 | 150 vencedoras e 1.350 recusas esperadas em 150 disputas da campanha 03 |
| Manutenção impede reserva | ReservaApiTest e BloqueioAfetaReservasApiTest | Evidência documentada |
| Somente responsável aprova restritos | AprovacaoApiTest e HTTP | Evidência documentada de autorização e preservação do histórico na recusa |
| Ciclo SOLICITADA → APROVADA → EM_USO → CONCLUÍDA e alternativas | CicloDeVidaReservaTest e CicloDeVidaApiTest | Matriz de 49 pares e auditoria das transições cobertas conforme RTM |
| Reserva iniciada preservada | Casos de proteção em uso na RTM | Evidência documentada de recusa e preservação |
| Toda mudança de estado auditada | AUD-01 a AUD-15, reinício | Campanha específica: 125 testes, zero falhas, no develop `34b7bcb` |

Não foi identificada nos resultados consolidados uma ocorrência de dupla reserva ou falha de autorização. Isso não prova ausência universal de defeitos. Aplicação executável e pipeline possuem evidência histórica; confirmar ambos no candidato final, pois são limitadores de nota.

## 5. Estratégia de testes e metas

| Modalidade/meta | Resultado disponível | Origem e ressalva |
|---|---|---|
| Unitários isolados | Campanha específica com 42 invocações | [RELATORIO-TESTES](RELATORIO-TESTES.md); incluídos nas suítes maiores |
| Parametrizados | 37 invocações na campanha específica | [guia](../testes-parametrizados.md); não somar a totais de outras campanhas |
| Integração/Testcontainers | PostgreSQL 17 nas integrações atuais | RTM; documentos antigos usam MySQL/H2 |
| API caixa-preta | 32 casos HTTP na campanha específica | [matriz HTTP](../testes-api-http.md) |
| BDD/TDD | Falha comportamental anterior e 3 cenários/27 passos aprovados depois | [evidência original](../../../bdd/EVIDENCIAS-APROVACAO-INATIVOS.md); falhas de preparação são distinguidas da falha de regra |
| E2E | Navegador, Angular e API; candidato em dois viewports | CANDIDATO e RELATORIO-TESTES; BDD só de API não é E2E de navegador |
| WireMock | 21 testes aprovados | Relatório específico; 3 defeitos encontrados e corrigidos |
| Backend: linhas ≥80%, branches ≥70% | **95,9% / 85,8%; 502 testes** documentados para develop `31afdd4` | RELATORIO-TESTES; percentuais históricos, não medição nova do SHA avaliado |
| Frontend: linhas ≥80%, branches ≥70% | **81,04% / 70,58%; 102 testes** documentados | RELATORIO-TESTES; metas obrigatórias no CI |
| Evidência bruta histórica de cobertura | 94,63% linhas / 79,16% branches | [JSON original](evidencias-candidato/cobertura.json), checkout `26a21f6`; não substitui métricas posteriores |
| 100% dos requisitos críticos na RTM | Riscos, testes e evidências mapeados | Matriz contém snapshots/adendos de épocas diferentes; percentual de aceite do SHA final não recalculado |
| Zero bugs/vulnerabilidades críticas | **Não certificado globalmente nesta avaliação** | Exige análise do conteúdo dos relatórios, triagem e vínculo ao candidato; workflow verde isolado é insuficiente |

A cobertura supera as metas nas campanhas registradas. A qualidade também é sustentada por asserções de estado, persistência, auditoria, conflitos e recusa de acesso. Cobertura, sozinha, não constitui aceite.

## 6. Performance e robustez

Campanha principal: `20261113-campanha-03`, JMeter 5.6.3, Java 21, PostgreSQL 17.11 em contêiner descartável, 4 vCPU/15 GiB, API/JMeter/banco na mesma máquina. Perfil de 50 usuários por fase, ramp-up de 60 s, 600 s de medição após aquecimento; massa sintética isolada.

| Métrica | Resultado |
|---|---:|
| Amostras / throughput total | 57.914 / 43,38 req/s |
| P95 / P99 total | 17 ms / 61 ms |
| P95 pesquisa | 6–8 ms (meta ≤2.000 ms) |
| P95 criar/consultar/alterar/cancelar | 12 / 9 / 19 / 16 ms (meta ≤3.000 ms) |
| P95 disputa sala/professor/material | 117 / 87 / 90 ms |
| Erros inesperados / 5xx | 0,0% / 0 |
| Disputas com única aceita | 150/150 |
| Criações PE-02 confirmadas no banco | 6.252/6.252 |

JTL, dashboard, parâmetros, ambiente, resumo e correlação de disputas estão em [resultados da campanha](../../../testes/carga/resultados/20261113-campanha-03/). HTTP 409 esperado é resultado correto da disputa, não falha inesperada. O teste não mede renderização Angular, múltiplos nós, rede de produção ou infraestrutura externa. Repetir a campanha completa no SHA final antes de extrapolar o parecer.

## 7. CI, SonarCloud, segurança e versões

Há workflows de backend, Angular, qualidade integrada, BDD, candidato, carga, SonarCloud e dependências em [.github/workflows](../../../.github/workflows/).

Resultados consultados diretamente no GitHub nesta avaliação:
- [Dependências 43507170524](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43507170524): sucesso no SHA `e6735ea`, incluindo análise Maven/OSV e publicação dos relatórios.
- [Angular 43507170541](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43507170541): sucesso no mesmo SHA.
- [SonarCloud 43505529626](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43505529626): workflow aprovado no SHA `c6809b8`. O relatório de testes também registra Quality Gate aprovado em PRs históricos.
- No SHA `f8dc43c`, os runs de dependências, candidato, qualidade integrada, backend, Angular, SonarCloud e carga ainda estavam em andamento/pendentes na consulta. Não foram classificados como aprovados antecipadamente.

**Correção de pendência documental:** a análise de dependências do backend já existe e executou com sucesso; a pendência antiga de inexistência da análise está superada. Porém, o workflow de dependências **não reprova por vulnerabilidades encontradas**: tolera avisos e exige a geração dos arquivos. É necessário ler os relatórios e fazer triagem para certificar zero críticas. Não foi extraída nova contagem de vulnerabilidades nesta avaliação. Os 16 avisos moderados de desenvolvimento e zero no `npm audit --omit=dev` são registros históricos do RELATORIO-TESTES.

O [POM](../../../backend/pom.xml) declara Java 21 e Spring Boot 4.1.2. O [registro de defeitos](../defeitos.md) documenta aceite do professor para Boot 4.1.2 em 13/11/2026; o aceite não foi obtido novamente nesta avaliação. A RTM registra uso histórico de Jupiter 6.0.3, enquanto o enunciado exige JUnit 5. Confirmar a versão resolvida no candidato e registrar aceite específico ou adequação; o aceite de Spring Boot não implica automaticamente aceite de JUnit.

## 8. Defeitos e revisão por pares

O [inventário de defeitos](../defeitos.md) distingue produto, automação e conformidade e apresenta causa, correção, regressão e limites.

| Grupo | Resultado registrado |
|---|---|
| DEF-001 | Aprovação revalida cadastros inativos; BDD antes/depois preservado |
| AUT-001/002 | Correções de autenticação BDD e asserção dependente de contagem global |
| DEF-PERF-001 | P95 do 409 de professor/material reduzido para 90 ms na campanha 03 |
| DEF-INT-001/002/003 | Logs externos, resposta corrompida e destinatário ausente corrigidos; WireMock aprovado |
| NC-001 | Divergência Boot encerrada pelo aceite documentado |
| IMP-003 | Resultado JMeter disponível; impedimento histórico superado |

Não se declara que todo defeito possível foi encontrado. O inventário deve acompanhar novas falhas do candidato.

O [registro de revisão](REVISAO-PARES.md) contém aprovações para #27, #29 e #43. A consulta direta encontrou também [aprovação de ThiagoMarangoni-dev no #28](https://github.com/CarlaLDC/projeto-aloca/pull/28#pullrequestreview-5897892731), superando a pendência descrita naquele documento. A revisão deste relatório e o aceite final ainda devem ser registrados por integrante independente; nenhuma assinatura foi presumida.

## 9. Entregáveis e avaliação por dimensão

| Dimensão / peso | Evidências | Limite para encerramento |
|---|---|---|
| Requisitos e regras / 15% | RTM, suítes e tabela funcional deste relatório | Reconciliar snapshots e candidato final |
| Estratégia/documentação / 15% | PLANO-TESTES, SUITE-TESTES, RELATORIO-TESTES | Consolidar resultados do candidato |
| Testes automatizados / 25% | Unitários, parametrizados, integração, HTTP, E2E, BDD, WireMock, concorrência | Confirmar execução e versão JUnit do candidato |
| CI/cobertura/análise / 15% | Actions, JaCoCo, cobertura frontend, SonarCloud | Preservar métricas e Quality Gate do SHA selecionado |
| Performance/segurança / 10% | JMeter, perfis, entrada/erros, OSV | Triagem de segurança e campanha completa final |
| Rastreabilidade/defeitos / 10% | RTM e inventário de defeitos | Fechar vínculo requisito → risco → teste → evidência do candidato |
| Organização/apresentação/autoria / 10% | Git, reviews, README, arquitetura/ADRs, [roteiro](ROTEIRO-DEMONSTRACAO.md) | Revisão deste relatório, pacote final e demonstração efetiva |

Presentes: repositório/histórico, instruções, documentação de API/fluxos, RTM, arquitetura, plano/suíte/relatórios, pipelines, carga com resultados, defeitos e roteiro de demonstração. O roteiro não comprova apresentação realizada. A existência dos workflows/relatórios não comprova pacote final exportado nem release publicada. Não foi atribuída nota estimada.

## 10. Restrições, ações e decisão de entrega

| Prioridade | Ação | Critério de encerramento | Responsável |
|---|---|---|---|
| P1 | Consolidar a campanha do SHA final | Runs obrigatórios concluídos, contagens, cobertura backend/frontend, BDD, reinício, viewports e carga vinculados ao mesmo candidato; preservar artefatos | Equipe |
| P1 | Triar segurança | Relatórios SonarCloud/OSV consultados, achados classificados, zero críticas comprovado ou impedimento registrado | Equipe |
| P1 | Confirmar JUnit | Dependência resolvida e conformidade JUnit 5 ou aceite explícito documentado | Equipe/professor |
| P2 | Completar avaliação manual | Data, avaliador, navegador, viewports, fluxos e resultado; registrar defeitos de usabilidade/acessibilidade | Equipe |
| P2 | Reconciliar documentação | RTM e revisão por pares com informações atuais; retirar ambiguidades dos adendos históricos | Equipe |
| P1 | Fechar entrega e revisão | Revisão independente deste relatório, SHA/tag do candidato, pacote durável de evidências e demonstração registrada | Equipe |

**Decisão final deste relatório:** o projeto apresenta base técnica e evidências suficientes para um parecer consolidado **aprovado com restrições**. A **homologação irrestrita da release permanece inconclusiva** até o fechamento das ações P1. Uma falha de autorização, dupla reserva, aplicação não executável ou pipeline ausente deve bloquear o aceite, conforme os limitadores do enunciado.

Este documento atende ao entregável “relatório final de qualidade” com transparência sobre o estado observado. Não constitui assinatura de professor/revisor nem substitui as evidências originais. Atualizar a decisão após a campanha final usando o [registro de execução](REGISTRO-EXECUCAO.md).
