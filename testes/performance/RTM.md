# RTM — Matriz completa de rastreabilidade

**Escopo:** RF-01 a RF-14 e RNF-01 a RNF-09, conforme `src/docs/requisitos-funcionais.md` e `src/docs/requisitos-nao-funcionais.md`.

**Legenda de status:** `Parcial` = há implementação/artefato de interface, mas a comprovação é incompleta; `Pendente` = teste não executado; `Bloqueado` = depende de API/ambiente/dados; `Não comprovado` = não há evidência suficiente. Links de código são referências para inspeção, não evidência de teste aprovado.

## Matriz RF

| Requisito | Risco se falhar | Teste / ID | Evidência / referência | Status |
|---|---|---|---|---|
| RF-01 Acesso por perfil | Alto — acesso indevido a funções/dados | CT-01: matriz de permissão por Solicitante, Responsável, Administrador e Professor | [rotas](../../src/app/app.routes.ts); teste de autorização de API ainda inexistente | Bloqueado |
| RF-02 Cadastro/consulta/alteração/desativação de recursos | Alto — dados incorretos ou recurso indisponível alocado | CT-02: CRUD de sala, professor e material; campos obrigatórios; persistência | [administração](../../src/app/features/admin/admin.ts) (estado local demonstrativo); API/persistência não disponível | Parcial |
| RF-03 Pesquisa por filtros e disponibilidade | Médio — usuário recebe recurso incompatível/indisponível | CT-03: filtros por tipo, capacidade, localização, competência e disponibilidade; carga SPA-02/API-PESQ | [disponibilidade](../../src/app/features/availability/availability.ts) usa fixtures locais; [JMX](./jmeter/aloca-spa-load-test.jmx) cobre GET da rota, não consulta real | Parcial |
| RF-04 Criar, alterar e cancelar reserva própria | Alto — reserva incorreta ou alteração não autorizada | CT-04a criação; CT-04b edição; CT-04c cancelamento; validar dono e confirmação | [nova reserva](../../src/app/features/new-reservation/new-reservation.ts); [detalhe](../../src/app/features/reservation-detail/reservation-detail.ts); testes [reservation-detail.spec](../../src/app/features/reservation-detail/reservation-detail.spec.ts). Ações são simulações locais | Parcial |
| RF-05 Impedir horário inválido/sobreposição; permitir consecutivas | Alto — dupla alocação | CT-05a término posterior; CT-05b sobreposição sala; CT-05c professor; CT-05d material; CT-05e horários consecutivos | [nova reserva](../../src/app/features/new-reservation/new-reservation.ts) valida somente término posterior; regra de conflito no servidor não localizada | Parcial |
| RF-06 Evitar duas reservas simultâneas conflitantes | Crítico — dupla reserva do mesmo recurso | CT-06: disparar duas requisições concorrentes com mesma sala/recurso e intervalo; exatamente uma aceita | [RF-06](../../src/docs/requisitos-funcionais.md); [ADR-0001](../../src/docs/adr/ADR-0001-consistencia-reservas.md); API transacional não localizada | Bloqueado |
| RF-07 Aprovação de recurso restrito | Alto — uso antes da aprovação ou decisão incorreta | CT-07a aprovar; CT-07b rejeitar com motivo; CT-07c impedir uso enquanto pendente | [approvals.ts](../../src/app/features/approvals/approvals.ts) + [DemoAuditLog](../../src/app/core/demo-audit-log.ts): eventos locais de aprovação/rejeição; sem persistência/autenticação | Parcial |
| RF-08 Bloquear recurso em manutenção/indisponibilidade | Alto — alocação de recurso indisponível | CT-08 criar, consultar e remover bloqueio; tentar reservar durante o período | [administration.ts](../../src/app/features/admin/admin.ts) possui fixtures e remoção local de bloqueio; integração de reserva não localizada | Parcial |
| RF-09 Registrar retirada/devolução de materiais | Alto — perda de rastreabilidade de patrimônio | CT-09a retirada; CT-09b devolução; conferir item, reserva, data/hora e ator | [withdrawals-returns.ts](../../src/app/features/withdrawals-returns/withdrawals-returns.ts) + [DemoAuditLog](../../src/app/core/demo-audit-log.ts): eventos locais com antes/depois e timestamp | Parcial |
| RF-10 Histórico de estados e mudanças; impedir apagar reserva em uso | Crítico — auditoria incompleta e impossibilidade de responsabilização | CT-10a criação; CT-10b aprovação; CT-10c rejeição; CT-10d edição; CT-10e cancelamento; CT-10f conclusão; CT-10g retirada; CT-10h devolução; CT-10i tentativa de exclusão em uso. Conferir ator, instante, estado anterior/novo | [DemoAuditLog](../../src/app/core/demo-audit-log.ts), integrado a criação, aprovação, rejeição, edição, cancelamento, retirada e devolução; [Matriz](./auditoria-transicoes.md) | Parcial — trilha somente em memória, sem ator autenticado/persistência; conclusão e exclusão sem fluxo |
| RF-11 Mensagens de confirmação/recusa e motivo | Médio — usuário não entende resultado da operação | CT-11 sucesso/erro para criar, editar, cancelar, aprovar, rejeitar e conflitos | mensagens demonstrativas em [approvals.ts](../../src/app/features/approvals/approvals.ts) e [reservation-detail.ts](../../src/app/features/reservation-detail/reservation-detail.ts); integração pendente | Parcial |
| RF-12 Relatórios de utilização/carga horária/conflitos evitados | Médio — decisão gerencial baseada em dados incorretos | CT-12 validar totais por período/recurso; carga de relatório | [reports.ts](../../src/app/features/reports/reports.ts); JMX SPA-07 mede somente GET da página | Parcial |
| RF-13 Interface responsiva e documentação | Médio — fluxo indisponível em dispositivos | CT-13a viewport 360 px; CT-13b 1280 px; percorrer fluxos principais | rotas em [app.routes.ts](../../src/app/app.routes.ts); testes visuais/screenshot ainda não anexados | Pendente |
| RF-14 Agenda do professor restrita à própria agenda | Alto — exposição/alteração indevida de agenda | CT-14 consultar agenda do próprio professor e tentar acessar agenda alheia/alterar reserva | rota/feature específica de agenda não localizada na árvore da aplicação | Não comprovado |

## Matriz RNF

| Requisito | Risco se falhar | Teste / ID | Evidência / referência | Status |
|---|---|---|---|---|
| RNF-01 p95 disponibilidade <=2 s; demais operações <=3 s; até 50 usuários | Alto — degradação e indisponibilidade | PERF-01 carga sustentada; PERF-02 picos; medir p95, throughput, erros e recursos | [JMX SPA](./jmeter/aloca-spa-load-test.jmx); sem JTL/dashboard e sem API de negócio | Pendente |
| RNF-02 exatamente uma solicitação aceita em conflito concorrente | Crítico — inconsistência de alocação | CT-06 repetido com concorrência; validar resultado no banco/API | [ADR-0001](../../src/docs/adr/ADR-0001-consistencia-reservas.md); backend não localizado | Bloqueado |
| RNF-03 autorização, validação, mensagens sem detalhes internos; zero alta/crítica aberta | Crítico — acesso indevido/exposição | SEC-01 testes por perfil; SEC-02 validação; SEC-03 revisão de mensagens e análise de segurança | implementação de API e relatório de segurança não fornecidos | Pendente |
| RNF-04 persistência após reinício | Alto — perda de reserva/histórico | CT-15 gravar, reiniciar serviço e conferir reserva + trilha | persistência/backend não localizado | Bloqueado |
| RNF-05 uso em celular/desktop e mensagens claras | Médio — falha de usabilidade | CT-13 viewports 360/1280 e fluxos pesquisa/reserva/cancelamento | screenshots/resultados visuais não anexados | Pendente |
| RNF-06 >=80% linhas e >=70% branches | Médio — regressões não detectadas | QA-01 executar cobertura no pipeline e arquivar relatório | package.json usa Angular/Karma; relatório de cobertura não fornecido; critério JaCoCo do requisito precisa ser compatibilizado com stack Angular | Pendente |
| RNF-07 zero bugs críticos e zero vulnerabilidades críticas abertas | Crítico — defeitos graves na entrega | QA-02 triagem de defeitos; SEC-03 relatório de segurança | [defeitos.md](./defeitos.md); sem varredura/triagem final anexada | Pendente |
| RNF-08 100% dos requisitos críticos com risco, teste e evidência | Alto — requisito sem verificação | QA-03 revisão desta RTM e links de artefatos | esta matriz + [auditoria-transicoes.md](./auditoria-transicoes.md); testes de execução/evidências ainda pendentes | Parcial |
| RNF-09 build e testes a cada PR | Médio — regressões introduzidas | CI-01 verificar workflow em PR e anexar run | [.github/workflows/main.yml](../../.github/workflows/main.yml); execução deste branch não verificada | Pendente |

## Cobertura de auditoria por transição de estado

| Entidade | Transição | Requisito | Teste | Situação atual observada no código |
|---|---|---|---|---|
| Reserva | inexistente -> Solicitada | RF-04/RF-10 | AUD-01 | criação demonstrativa; não grava trilha compartilhada |
| Reserva | Solicitada -> Aprovada | RF-07/RF-10 | AUD-02 | status local em approvals; sem evento de auditoria |
| Reserva | Solicitada -> Rejeitada | RF-07/RF-10 | AUD-03 | status local em approvals; sem evento de auditoria |
| Reserva | Solicitada/Aprovada -> Cancelada | RF-04/RF-10 | AUD-04 | evento acrescentado no histórico local do detalhe; sem persistência |
| Reserva | dados alterados (status preservado) | RF-04/RF-10 | AUD-05 | evento local “Alterada · simulação”; não registra diff/estado anterior-novo |
| Reserva | Aprovada -> Concluída | RF-10 | AUD-06 | existe fixture Concluída; transição operacional não localizada |
| Material | Aguardando retirada -> Em uso | RF-09/RF-10 | AUD-07 | estado local muda em withdrawals-returns; sem evento de auditoria |
| Material | Devolução pendente -> Devolvido | RF-09/RF-10 | AUD-08 | estado local muda e gera protocolo demonstrativo; sem evento de auditoria |

Ver passos, resultados esperados e evidências em [auditoria-transicoes.md](./auditoria-transicoes.md).

## Conclusão da RTM
A matriz agora relaciona todos os RF-01..RF-14 e RNF-01..RNF-09 a riscos, testes e referências/evidências. **Isso fecha a estrutura documental da RTM, mas não comprova 100% dos requisitos críticos executados.** A cobertura local de eventos foi ampliada; persistência e identidade autenticada continuam sem implementação. RF-06, RF-10, RNF-01, RNF-02, RNF-03 e RNF-04 continuam sem evidência de execução suficiente. Não declarar aceite até anexar resultados reais.
