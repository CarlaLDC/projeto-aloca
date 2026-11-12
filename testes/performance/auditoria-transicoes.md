# Plano de testes de auditoria de transições — RF-10

## Objetivo e critério geral
Comprovar que toda mudança de estado registra, de forma persistente e correlacionável: identificador da entidade, estado anterior, estado novo, ator autenticado, instante, ação/origem e referência da operação. A trilha deve ser consultável após reinício.

O código Angular possui agora `DemoAuditLog`, serviço compartilhado em memória da sessão, integrado à criação, aprovação, rejeição, edição, cancelamento, retirada e devolução. Cada evento contém ID, entidade, estado anterior/novo, ação, timestamp ISO e descrição. O ator é explicitamente marcado como demonstração não autenticada. O serviço não persiste no servidor e os eventos desaparecem ao recarregar a SPA; não equivale a auditoria de produção.

## Casos de teste

| ID | Transição/ação | Pré-condição e passos | Resultado esperado | Evidência a anexar | Situação |
|---|---|---|---|---|---|
| AUD-01 | Criar reserva: inexistente -> Solicitada | Criar reserva válida com usuário autenticado | Evento de criação com ator, timestamp, estado anterior “inexistente” e novo “Solicitada” | teste automatizado, resposta API e consulta de auditoria | Bloqueado: criação é simulação local |
| AUD-02 | Aprovar: Solicitada -> Aprovada | Responsável aprova solicitação pendente | Evento registra antes/depois, ator Responsável e instante | teste automatizado + evento persistido | Parcial: evento compartilhado em memória; ator não autenticado e sem persistência |
| AUD-03 | Rejeitar: Solicitada -> Rejeitada | Responsável rejeita com justificativa | Evento registra antes/depois, ator, instante e motivo | teste automatizado + evento persistido | Lacuna: approvals.ts não escreve auditoria |
| AUD-04 | Cancelar: Solicitada/Aprovada -> Cancelada | Solicitante autorizado cancela com motivo | Evento registra antes/depois, ator autenticado, timestamp e motivo; persiste após recarregar | spec, consulta de auditoria e teste de reinício | Parcial: evento compartilhado; sem persistência e ator autenticado |
| AUD-05 | Editar reserva | Alterar dados válidos sem mudar status | Evento registra ator, instante e campos alterados/diff seguro | spec + registro de auditoria | Parcial: evento compartilhado com antes/depois do status; diff de campos não detalhado |
| AUD-06 | Concluir: Aprovada -> Concluída | Encerrar reserva após uso | Evento de conclusão com estado anterior/novo e ator | teste de integração + trilha persistida | Bloqueado: transição operacional não localizada |
| AUD-07 | Retirar material: Aguardando retirada -> Em uso | Responsável registra retirada | Evento inclui material, reserva, data/hora, ator e antes/depois | teste automatizado + trilha persistida | Parcial: evento compartilhado em memória; ator não autenticado e sem persistência |
| AUD-08 | Devolver material: Devolução pendente -> Devolvido | Responsável registra devolução | Evento inclui material, reserva, data/hora, ator e antes/depois | teste automatizado + trilha persistida | Lacuna: withdrawals-returns.ts só altera signal |
| AUD-09 | Tentar excluir reserva em uso | Tentar exclusão por perfil autorizado | Exclusão recusada; tentativa registrada conforme política | teste de autorização e log | Bloqueado: endpoint/regra não localizado |
| AUD-10 | Integridade após reinício | Executar AUD-01..09, reiniciar serviço e consultar trilha | Eventos confirmados permanecem íntegros e ordenáveis | teste de integração e consulta/export | Bloqueado: backend/persistência não localizados |

## Evidência automatizada disponível
`demo-audit-log.spec.ts` cobre criação de evento com antes/depois/timestamp/ator demonstrativo e ordenação de múltiplos eventos. `reservation-detail.spec.ts` cobre edição/cancelamento no detalhe. Os testes foram adicionados, mas não executados neste ambiente.

## Lacunas para fechar RF-10
1. Substituir o serviço demonstrativo por gravação em API transacional e integrar conclusão;
2. Registrar estado anterior/novo, ator autenticado, instante confiável e motivo/diff.
3. Persistir trilha fora da memória do navegador e permitir consulta por reserva/material.
4. Testar todas as transições e persistência após reinício.
5. Anexar logs/IDs de correlação sem dados pessoais desnecessários.

**Parecer:** auditoria parcial na demonstração Angular; RF-10 não está totalmente comprovado até execução de AUD-01..10 no backend.
