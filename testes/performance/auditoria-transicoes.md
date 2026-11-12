# Plano de testes de auditoria de transições — RF-10

## Objetivo e critério geral
Comprovar que toda mudança de estado registra, de forma persistente e correlacionável: identificador da entidade, estado anterior, estado novo, ator autenticado, instante, ação/origem e referência da operação. A trilha deve ser consultável após reinício.

O código Angular contém histórico demonstrativo em reservation-data.ts. reservation-detail.ts acrescenta eventos locais para edição e cancelamento. Esses eventos vivem em memória do componente, usam o solicitante como ator fixo e não contêm campos explícitos de estado anterior/novo. Aprovação/rejeição e movimentação alteram signals locais sem anexar evento. Histórico visual não equivale a auditoria completa/persistente.

## Casos de teste

| ID | Transição/ação | Pré-condição e passos | Resultado esperado | Evidência a anexar | Situação |
|---|---|---|---|---|---|
| AUD-01 | Criar reserva: inexistente -> Solicitada | Criar reserva válida com usuário autenticado | Evento de criação com ator, timestamp, estado anterior “inexistente” e novo “Solicitada” | teste automatizado, resposta API e consulta de auditoria | Bloqueado: criação é simulação local |
| AUD-02 | Aprovar: Solicitada -> Aprovada | Responsável aprova solicitação pendente | Evento registra antes/depois, ator Responsável e instante | teste automatizado + evento persistido | Lacuna: approvals.ts não escreve auditoria |
| AUD-03 | Rejeitar: Solicitada -> Rejeitada | Responsável rejeita com justificativa | Evento registra antes/depois, ator, instante e motivo | teste automatizado + evento persistido | Lacuna: approvals.ts não escreve auditoria |
| AUD-04 | Cancelar: Solicitada/Aprovada -> Cancelada | Solicitante autorizado cancela com motivo | Evento registra antes/depois, ator autenticado, timestamp e motivo; persiste após recarregar | spec, consulta de auditoria e teste de reinício | Parcial: evento local; sem persistência e ator autenticado |
| AUD-05 | Editar reserva | Alterar dados válidos sem mudar status | Evento registra ator, instante e campos alterados/diff seguro | spec + registro de auditoria | Parcial: evento local genérico, sem diff |
| AUD-06 | Concluir: Aprovada -> Concluída | Encerrar reserva após uso | Evento de conclusão com estado anterior/novo e ator | teste de integração + trilha persistida | Bloqueado: transição operacional não localizada |
| AUD-07 | Retirar material: Aguardando retirada -> Em uso | Responsável registra retirada | Evento inclui material, reserva, data/hora, ator e antes/depois | teste automatizado + trilha persistida | Lacuna: withdrawals-returns.ts só altera signal |
| AUD-08 | Devolver material: Devolução pendente -> Devolvido | Responsável registra devolução | Evento inclui material, reserva, data/hora, ator e antes/depois | teste automatizado + trilha persistida | Lacuna: withdrawals-returns.ts só altera signal |
| AUD-09 | Tentar excluir reserva em uso | Tentar exclusão por perfil autorizado | Exclusão recusada; tentativa registrada conforme política | teste de autorização e log | Bloqueado: endpoint/regra não localizado |
| AUD-10 | Integridade após reinício | Executar AUD-01..09, reiniciar serviço e consultar trilha | Eventos confirmados permanecem íntegros e ordenáveis | teste de integração e consulta/export | Bloqueado: backend/persistência não localizados |

## Evidência automatizada disponível
reservation-detail.spec.ts cobre cancelamento e acréscimo de descrição ao histórico local, edição válida com acréscimo de evento local e edição inválida sem alteração do estado/histórico. Isso cobre somente o componente de detalhe.

## Lacunas para fechar RF-10
1. Centralizar escrita de auditoria em serviço/API transacional compartilhada por criação, aprovação, rejeição, edição, cancelamento, conclusão, retirada e devolução.
2. Registrar estado anterior/novo, ator autenticado, instante confiável e motivo/diff.
3. Persistir trilha fora da memória do navegador e permitir consulta por reserva/material.
4. Testar todas as transições e persistência após reinício.
5. Anexar logs/IDs de correlação sem dados pessoais desnecessários.

**Parecer:** auditoria parcial na demonstração Angular; RF-10 não está totalmente comprovado até execução de AUD-01..10 no backend.
