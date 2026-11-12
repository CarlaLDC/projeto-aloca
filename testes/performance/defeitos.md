# Registro de defeitos e impedimentos

Defeito de produto só é confirmado após reprodução. Lacunas verificadas por inspeção estática são registradas como gaps, não como falhas de runtime reproduzidas.

| ID | Tipo | Descrição | Severidade | Estado / ação |
|---|---|---|---|---|
| IMP-001 | Impedimento | API/backend e URL de homologação não documentados para operações reais de reserva. | Bloqueador | Aberto; solicitar API, contrato e ambiente autorizado. |
| IMP-002 | Impedimento | Credenciais por perfil e massa sintética não documentadas. | Alto | Aberto; solicitar contas e dados de teste. |
| IMP-003 | Impedimento | Sem execução JMeter; JTL/dashboard/capturas inexistentes. | Alto | Aberto; executar após liberação do ambiente. |
| GAP-004 | Lacuna de cobertura/implementação observada estaticamente | Histórico visual é local ao detalhe; aprovação/rejeição e retirada/devolução alteram estado em signals sem evento de auditoria compartilhado. | Alto | Aberto; centralizar auditoria e cobrir AUD-01..10. |
| GAP-005 | Lacuna de evidência/implementação observada estaticamente | Evento de edição/cancelamento não contém estado anterior/novo explícitos, usa ator fixo derivado do solicitante e não persiste no servidor. | Alto | Aberto; completar evento e comprovar persistência. |

## Defeitos de runtime reproduzidos
Nenhum reproduzido nesta etapa. GAP-004/005 são lacunas observadas por leitura estática, não falhas de runtime reproduzidas.

## Modelo para novos registros
ID; requisito; ambiente/commit; pré-condições/massa; passos; esperado; obtido; severidade; evidência (JTL/log/screenshot sem dados sensíveis); estado/responsável.
