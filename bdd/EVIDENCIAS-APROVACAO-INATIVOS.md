# BDD comprovado — aprovação após desativação

Data: 12/11/2026. PR: [#34](https://github.com/CarlaLDC/projeto-aloca/pull/34).

## Comportamento novo

Uma solicitação restrita é criada com sala, material e professor ativos. Se o Administrador desativar qualquer um desses cadastros antes da decisão, o painel deve informar o impedimento e a aprovação deve responder HTTP 409, preservando estado SOLICITADA e histórico. Após a reativação, o painel volta a indicar disponibilidade válida e o Responsável consegue aprovar com auditoria.

Essa revalidação não existia na base develop `0b6aca8ac015e824d2f3be80208f805de0c1c8d8`. A aprovação já conferia conflitos de horários e manutenção, mas não cadastros desativados posteriormente. O incremento acrescenta uma regra de negócio; não se limita a escrever testes para o comportamento existente.

## Método BDD utilizado

1. **Descoberta:** comparar requisitos de cadastros e aprovação com o código; identificar a diferença entre cadastro ativo na criação e cadastro ativo na decisão.
2. **Formulação:** escrever exemplos Dado/Quando/Então para sala, material e professor, antes da implementação.
3. **Automação:** transformar os exemplos em cenários Cucumber que usam API, autenticação e persistência reais.
4. **Execução inicial:** observar a aplicação não atendendo ao comportamento especificado.
5. **Implementação guiada:** acrescentar a revalidação na função compartilhada pelo painel e pela aprovação.
6. **Documentação viva:** executar novamente, preservar relatórios e vincular requisitos, commits e resultados.

Os exemplos foram definidos a partir do pedido e dos requisitos fornecidos. A revisão por pares continua sendo uma etapa de aprovação da equipe, feita pelo PR; não se presume que já ocorreu.

## Cenário executável

Fonte completa: [aprovacao-inativos.feature](features/aprovacao-inativos.feature). Automação: [aprovacao-inativos.cjs](steps/aprovacao-inativos.cjs).

```gherkin
Dado uma solicitação restrita com sala, material e professor ativos
Quando o administrador desativa o "<alvo>" da solicitação
Então o painel de aprovações informa a desativação do "<alvo>"
Quando o responsável tenta aprovar a solicitação com cadastro desativado
Então a API recusa com 409 e preserva a solicitação e sua auditoria
Quando o administrador reativa o "<alvo>" da solicitação
Então o painel informa que a solicitação pode ser aprovada
Quando o responsável aprova a solicitação revalidada
Então a reserva fica aprovada com auditoria da decisão
```

Exemplos: sala, material e professor. São três cenários, nove passos de negócio por cenário, totalizando 27 passos; hooks de preparação e encerramento não entram nesse total.

## Linha do tempo verificável

| Etapa | Commit / execução | Evidência |
|---|---|---|
| Especificação e automação, sem mudar produção | [b061bf8](https://github.com/CarlaLDC/projeto-aloca/commit/b061bf81d665674e3d7a4484b1c81a61ccfbd9d3) | Feature, steps, configuração Cucumber e workflow |
| Ajuste do login do teste, ainda sem regra nova | [cbf4878](https://github.com/CarlaLDC/projeto-aloca/commit/cbf4878b07f9a4b9a2e392c2f417e82fce427727) | Omitir Bearer quando não há token |
| Comportamento não atendido | [Execução 43326822779](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43326822779) | 3 cenários falharam: o painel retornou disponibilidadeValidada=true após desativação, mas o exemplo exige false |
| Implementação de negócio | [f4b2777](https://github.com/CarlaLDC/projeto-aloca/commit/f4b27779d4ec995db2839d0aa51383fbff85a9b2) | ReservaService.problemas passa a incluir cadastros inativos |
| Ajuste de contrato na asserção de auditoria | [704c339](https://github.com/CarlaLDC/projeto-aloca/commit/704c33979dd1b41606ce007ebd4be31854f127a5) | A API do histórico retorna os rótulos Solicitada/Aprovada; o estado principal continua SOLICITADA/APROVADA |
| Comportamento atendido | [Execução 43327149247](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43327149247) | 3 cenários aprovados; 27 passos aprovados |

A execução inicial `43326687096` falhou no login porque o teste enviava Bearer null. Ela **não** é usada como prova de comportamento ausente. A execução intermediária `43327002872` validou bloqueio, preservação e reativação, mas falhou na asserção de rótulos da auditoria. Essa correção técnica é declarada na linha do tempo; não houve mudança do Gherkin nem remoção de verificações para obter aprovação.

Na execução de falha válida, os passos após a primeira asserção do painel ficaram ignorados pelo Cucumber: não se atribui a essa execução uma observação do endpoint de aprovação que ela não realizou. Na execução aprovada, todos esses passos foram efetivamente executados.

O Actions testa o merge temporário do PR: head de falha `cbf4878...` e head aprovado `704c339...`. O checkout aprovado foi `3187765166ab5b5971082dc3b72720fed0cfd025`. Os artefatos preservam checkout-sha.txt, github-sha.txt e run-id.txt.

## Relatórios preservados

| Fase | Relatório original Cucumber no repositório | Artefato com HTML/JSON e metadados |
|---|---|---|
| Antes da regra | [antes.json](evidencias/aprovacao-inativos/antes.json) | [Baixar evidências da falha](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43326822779/artifacts/13993051792) |
| Após a regra | [depois.json](evidencias/aprovacao-inativos/depois.json) | [Baixar evidências aprovadas](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43327149247/artifacts/13992124950) |

Os JSONs são cópias dos relatórios reais baixados dos artefatos, sem reconstrução dos resultados. Os artefatos têm retenção até 10/02/2027; os JSONs versionados permanecem no histórico do repositório.

Para visualizar: abra o link da execução → Artifacts → baixe o ZIP → extraia → abra aprovacao-inativos.html. O vermelho inicial é intencional como evidência; o resultado aprovado está em outra execução.

## Rastreabilidade

A numeração segue o enunciado fornecido.

| Requisito / objetivo | Risco tratado | Exemplo e asserção | Implementação / evidência |
|---|---|---|---|
| RF-02 — cadastros e consulta | Aprovar sala/material/professor desativado após solicitação | Três linhas de Exemplos; desativar e reativar via API administrativa | ReservaService.problemas; relatórios antes/depois |
| RF-07 — aprovação de restritos | Responsável confirmar uma alocação que deixou de ser válida | Painel indica impedimento; POST aprovação responde 409 | AprovacaoService.aprovar já usa a função compartilhada |
| RF-10 — histórico auditável | Tentativa recusada mudar estado ou registrar decisão inválida | SOLICITADA e histórico idêntico após 409; transição auditada após aprovação válida | Reserva.mudarStatus; relatório depois |
| Semana 41 — aprovação/bloqueio por BDD | Apenas apresentar testes adicionados depois da regra | Cenários/execução com falha registrados antes do commit da regra | Linha do tempo acima |
| TDD ou BDD — funcionalidade nova | Ausência de evidência de desenvolvimento guiado pela técnica escolhida | Incremento novo formulado e automatizado antes da implementação | BDD, commits e execuções reais vinculados |

O bloqueio novo é por **desativação de cadastro**. Manutenção temporal (RF-08) e autorização por perfil (RF-01) continuam existentes; não são apresentados como funcionalidades criadas neste ciclo.

## Implementação

Fonte: [ReservaService.java](../backend/src/main/java/br/com/aloca/servico/ReservaService.java), método problemas.

A função agora acumula impedimentos de sala/material/professor inativos junto com conflitos e manutenção. Ela é usada pela listagem de aprovações e pela confirmação transacional já existente em AprovacaoService. Não foram adicionados endpoints, dependências nem tabelas. A reativação remove o impedimento sem cancelar automaticamente a solicitação.

## Reproduzir

Pré-requisitos: Java 21, Maven, Node 22 e API com dados de demonstração em banco descartável. Não executar contra dados reais: cada cenário cria cadastros e reserva exclusivos.

Desde a troca do banco para PostgreSQL (PR #45), o H2 não faz mais parte do projeto. Banco descartável local (PostgreSQL na memória do contêiner, porta 5433), na raiz:

```bash
docker compose -f backend/docker-compose.yml --profile e2e up -d postgres-e2e
```

Terminal 1:

```bash
cd backend
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.datasource.url=jdbc:postgresql://localhost:5433/aloca_e2e"
```

Terminal 2:

```bash
npm install --prefix bdd
cd bdd
npx cucumber-js --config aprovacao-inativos.cjs
```

A configuração dedicada usa Playwright APIRequestContext e não requer navegador nem Angular. O workflow usa Java 21, Node 22 e PostgreSQL real descartável (serviço `postgres:17-alpine`), sem mocks; as execuções históricas citadas abaixo (`cbf4878` e `704c339`) usaram H2 em memória. Cada cenário cria recursos próprios para não depender de IDs compartilhados.

Para reproduzir a falha histórica, use o commit cbf4878; para reproduzir a execução aprovada, use 704c339. Não é necessário desfazer a implementação na branch de trabalho atual.

## Limites

O ciclo histórico comprovou este incremento BDD em API/persistência H2; hoje o mesmo workflow roda com PostgreSQL. Não certifica cobertura total, performance ou concorrência. Não introduz garantia de atomicidade entre uma desativação administrativa simultânea e a aprovação: o cenário cobre desativação concluída antes da decisão. Os testes PostgreSQL/Testcontainers e demais checks do PR devem ser considerados separadamente. Não houve merge automático.
