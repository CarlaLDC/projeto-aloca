> **Revisão de 12/11/2026:** proposta histórica preservada. Para o estado implementado em develop, consulte [ADR-0005](ADR-0005-arquitetura-implementada.md) e [arquitetura atual](../arquitetura.md). Este texto não comprova funcionalidades implementadas.

# ADR-0003: Validação, versionamento e promoção de modelos

- **Estado:** Proposto; objetivo do modelo, métricas e autorização de promoção pendentes.
- **Data:** 2026-10-27.
- **Decisores necessários:** responsável pelo produto/modelo, segurança e operação.

## Contexto

A restrição da solicitação exige treinar/retreinar diariamente com dados novos, mas não define previsão/saída, algoritmo, baseline, métrica, limiar, consumidor online ou obrigação de publicar o modelo. A arquitetura lógica propõe candidato imutável, validação, registry e rollback para evitar que um candidato pior substitua uma versão válida.

## Forças

- Não publicar candidato sem evidência de qualidade aprovada.
- Preservar a versão anterior para continuidade e investigação.
- Reproduzir a avaliação associando código, configuração e referência do conjunto de dados.
- Evitar inventar métricas ou promoção automática sem mandato de negócio.

## Alternativas

1. Treinar e armazenar artefato sem promoção: menor risco de impacto online, mas só é válido se a finalidade for treinamento/experimento.
2. Promoção manual após revisão: maior controle, com custo operacional e atraso.
3. Promoção automática após gates objetivos: maior automação, mas só após definição e validação dos critérios.
4. Sem registry dedicado, usando armazenamento versionado com manifesto: menor dependência, governança menos rica; produto ainda não escolhido.

## Decisão proposta

Separar os estados de candidato, validado e promovido. Manter artefatos imutáveis e metadados suficientes para rastrear a execução. Não promover automaticamente até que finalidade do modelo, métrica, baseline, limiar, aprovador e uso online sejam aprovados. Manter referência à versão previamente válida e definir rollback antes de habilitar qualquer consumidor online.

Registry, estratégia de versionamento, gates, promoção manual/automática e rollback são decisões **sem requisito explícito**, pendentes de validação. A decisão não declara que um modelo será usado nas reservas.

## Consequências

- Um candidato reprovado não substitui a versão aprovada.
- O batch pode concluir treinamento sem publicar um modelo, dependendo do commit point de ADR-0002.
- Promoção e rollback precisam de evidência, autoridade e procedimento definidos antes de uso operacional.
- Armazenamento de versões, métricas e manifestos tem custo e implicações de retenção.

## Riscos

- Sem critério de avaliação, “pior” e “válido” não são operacionais.
- Métricas inadequadas podem aprovar regressão de negócio ou rejeitar um modelo útil.
- Uma referência ativa alterada sem atomicidade pode tornar rollback ambíguo.
- Snapshots de dados para reprodutibilidade podem reter dados pessoais além do necessário.

## Requisitos e evidências

- **Requisitos:** restrição explícita da solicitação; RNF-08 como alinhamento para rastreabilidade, não como requisito de qualidade de ML.
- **Cenários:** AT-07, AT-08, AT-09, AT-11.
- **Evidências propostas:** relatório de validação candidato versus baseline aprovado; teste de candidato reprovado mantendo versão anterior; teste de registro de metadados; teste de rollback após promoção autorizada. Métricas e limiares continuam a definir.
