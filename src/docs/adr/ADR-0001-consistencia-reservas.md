# ADR-0001: Consistência na confirmação de reservas

- **Estado:** Proposto; mecanismo físico pendente de decisão técnica.
- **Data:** 2026-10-27.
- **Decisores necessários:** responsáveis pela arquitetura, aplicação e persistência.

## Contexto

RF-05 proíbe sobreposição de sala, professor ou material e permite reservas consecutivas. RF-06 e RNF-02 exigem que, em duas solicitações conflitantes simultâneas, exatamente uma seja aceita. RNF-04 exige preservar reservas e histórico confirmados após reinício. O C4 atual tem Serviço de Reservas, adaptador de persistência e Persistência Operacional, mas a relação do adaptador com o armazenamento precisa ser explícita.

## Forças

- Evitar dupla alocação mesmo quando pedidos chegam ao mesmo tempo.
- Preservar a regra de intervalo e distinguir horários consecutivos de sobreposição.
- Não declarar sucesso antes da confirmação durável.
- Manter o mecanismo independente de tecnologia até conhecer a persistência aprovada.

## Alternativas

1. Validar disponibilidade na aplicação e gravar depois, sem controle atômico: simples, mas vulnerável à corrida; rejeitada como abordagem suficiente.
2. Aplicar controle atômico no limite de confirmação, com restrição, transação, lock ou mecanismo equivalente: atende ao objetivo, mas a escolha concreta depende da tecnologia.
3. Serializar todas as reservas em um único processo: alternativa simples para uma implantação estritamente única, mas não garante consistência se houver mais de uma instância.

## Decisão proposta

A validação final de disponibilidade e a gravação da reserva devem constituir uma operação atômica no limite da persistência ou de um coordenador exclusivo. A aplicação retorna sucesso somente após confirmação. Implementar a regra para sala, professor e cada material envolvido. Reservas consecutivas permanecem permitidas conforme RF-05.

O mecanismo físico (restrição de armazenamento, transação, lock ou combinação) fica **pendente**, pois nenhum requisito define banco, topologia ou tecnologia. Em falha ou conflito, retornar resultado não confirmado e mensagem compreensível conforme RF-11.

## Consequências

- O caminho feliz e o caminho concorrente ficam testáveis com o critério existente de uma aceitação em duas requisições conflitantes.
- A persistência escolhida deve suportar a garantia de atomicidade correspondente.
- Consultas de disponibilidade antes da confirmação são indicativas; a confirmação final é a autoridade.
- A decisão não define protocolo, banco, isolamento transacional ou estratégia de retry.

## Riscos

- Uma implementação apenas com leitura seguida de gravação pode permitir corrida.
- Um mecanismo de exclusão amplo pode reduzir throughput; efeito precisa ser medido.
- A semântica de reserva pendente de aprovação pode mudar quando o recurso passa a ficar bloqueado; RF-07 e RNF-02 não esclarecem esse momento.

## Requisitos e evidências

- **Requisitos:** RF-04, RF-05, RF-06, RF-07, RF-11, RF-10; RNF-02, RNF-04.
- **Cenários:** AT-01, AT-03.
- **Evidências:** teste de integração concorrente (duas solicitações conflitantes, exatamente uma aceita); testes de intervalo com sobreposição e consecutividade; teste de reinício com reserva confirmada. Ambiente e massa de dados devem ser registrados; não há métrica adicional inventada.
