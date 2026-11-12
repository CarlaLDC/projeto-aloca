# Registro de defeitos e impedimentos

Defeito de produto só é confirmado após reprodução. Lacunas verificadas por inspeção estática são registradas como gaps, não como falhas de runtime reproduzidas.

| ID | Tipo | Descrição | Severidade | Estado / ação |
|---|---|---|---|---|
| IMP-001 | Impedimento | API/backend e URL de homologação não documentados para operações reais de reserva. | Bloqueador | Aberto; solicitar API, contrato e ambiente autorizado. |
| IMP-002 | Impedimento | Credenciais por perfil e massa sintética não documentadas. | Alto | Aberto; solicitar contas e dados de teste. |
| IMP-003 | Impedimento | Sem execução JMeter; JTL/dashboard/capturas inexistentes. | Alto | Aberto; executar após liberação do ambiente. |
| GAP-004 | Lacuna de cobertura/implementação observada estaticamente | Eventos de criação, aprovação, rejeição, edição, cancelamento, retirada e devolução são adicionados ao DemoAuditLog compartilhado, porém somente em memória da sessão. | Alto | Aberto; migrar DemoAuditLog para API transacional persistente, cobrir conclusão e executar AUD-01..10. |
| GAP-005 | Lacuna de evidência/implementação observada estaticamente | DemoAuditLog inclui estado anterior/novo e timestamp, mas usa ator demonstrativo não autenticado e não persiste no servidor. | Alto | Aberto; integrar identidade autenticada, persistência e consulta da trilha. |

## Defeitos de runtime reproduzidos
Nenhum reproduzido nesta etapa. GAP-004/005 são lacunas observadas por leitura estática, não falhas de runtime reproduzidas.

## Modelo para novos registros
ID; requisito; ambiente/commit; pré-condições/massa; passos; esperado; obtido; severidade; evidência (JTL/log/screenshot sem dados sensíveis); estado/responsável.
