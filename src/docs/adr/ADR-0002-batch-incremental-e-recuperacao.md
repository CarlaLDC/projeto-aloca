> **Revisão de 12/11/2026:** proposta histórica preservada. Para o estado implementado em develop, consulte [ADR-0005](ADR-0005-arquitetura-implementada.md) e [arquitetura atual](../arquitetura.md). Este texto não comprova funcionalidades implementadas.

# ADR-0002: Processamento batch incremental e recuperação

- **Estado:** Proposto; contrato da fonte, semântica de novidade e política operacional pendentes.
- **Data:** 2026-10-27.
- **Decisores necessários:** responsável pela fonte de dados, operações e arquitetura de dados/ML.

## Contexto

A restrição explícita desta solicitação determina execução diária ao final do dia para treinar/retreinar usando somente dados novos. Ainda não há identificador RF/RNF próprio, fonte, formato, definição de “novo”, fuso, janela, escala ou política de falha. A arquitetura já propõe watermark/checkpoint, ledger, idempotência e lock, mas seus detalhes não foram aprovados.

## Forças

- Processar apenas o delta autorizado, sem afirmar que dados antigos podem integrar o treino.
- Evitar perda, repetição não controlada e confirmação duplicada após falhas ou disparos repetidos.
- Permitir detectar quais itens foram considerados e explicar o resultado.
- Limitar retenção e duplicação de dados sensíveis.

## Alternativas

1. Timestamp de atualização como watermark: simples se a fonte garantir ordenação e precisão; pode perder eventos atrasados ou empatados.
2. Offset/sequence monotônico fornecido pela fonte: facilita checkpoint, se existir e for estável.
3. Cursor composto por timestamp e identificador estável: alternativa quando não há sequence, sujeita à garantia de ordenação e à política de atraso.
4. Varredura com janela de atraso e deduplicação: tolera atraso, mas relê dados; só é compatível com “somente dados novos” se a identidade do registro e a elegibilidade forem formalmente definidas.

## Decisão proposta

Usar conceitualmente um watermark por fonte, ledger de execução e chave idempotente por recorte/configuração. Adquirir exclusão mútua antes de ler o checkpoint. Persistir o estado antes e depois; avançar checkpoint apenas no commit point acordado, sem promover ou declarar sucesso para artefato incompleto. O mecanismo de lock, lease, fencing, ledger e armazenamento fica **sem requisito e pendente de escolha**.

A definição do commit point não está decidida: alternativas a validar são (a) dados lidos e validados, mesmo se candidato for rejeitado, ou (b) candidato validado e artefato durável. A decisão deve garantir que registros rejeitados por qualidade não bloqueiem silenciosamente a sequência nem sejam marcados como usados sem política explícita.

## Consequências

- Um retry pode reconhecer o mesmo recorte sem criar uma confirmação lógica duplicada.
- Jobs sobrepostos devem aguardar, ser ignorados ou enfileirados; a política ainda está pendente.
- A fonte precisa expor identificador/ordenação suficiente, ou será necessário definir deduplicação.
- Recuperação, tentativas, atrasos, duração máxima e alertas continuam sem limites aprovados.

## Riscos

- Watermark incorreto omite dados atrasados ou reprocessa dados antigos.
- Lock sem expiração/fencing pode bloquear para sempre ou permitir escritor obsoleto.
- Checkpoint adiantado pode perder treino/artefato; checkpoint tardio pode repetir custo.
- Não avançar para sempre após dados inválidos pode bloquear dados posteriores; quarentena/skip exige regra de negócio ainda ausente.

## Requisitos e evidências

- **Requisitos:** restrição explícita da solicitação (batch diário, somente dados novos); RNF-04 por analogia para durabilidade; RNF-08 para rastreabilidade.
- **Cenários:** AT-04, AT-05, AT-06, AT-07, AT-09.
- **Evidências propostas:** teste com recorte novo e antigo; execução repetida com mesma chave; dois disparos simultâneos; falha antes e depois do treino; fonte indisponível; dados fora do esquema; comparação de checkpoint antes/depois. Limites temporais e contagens de dados esperadas permanecem a definir.
