# Revisão por pares — preparação e registro

## Situação

**Pendente de avaliação por integrante diferente do autor.** Este documento prepara a revisão, mas não constitui aprovação. Foi produzido com assistência automatizada. Merge, CI verde e autoavaliação não substituem a manifestação independente do revisor.

Base preparada: develop 29b97726952b3bd28337ff3c9bdb369cd18c96ec. A revisão deve indicar o SHA efetivamente inspecionado. Revisões dos PRs já mesclados podem ser retrospectivas; não declarar que ocorreram antes do merge.

## Escopo para o revisor

| PR | Autor registrado | Foco de revisão |
|---|---|---|
| [#27](https://github.com/CarlaLDC/projeto-aloca/pull/27) | CarlaLDC | BDD integrado: autenticação real, persistência, assertions e limites dos cenários |
| [#28](https://github.com/CarlaLDC/projeto-aloca/pull/28) | CarlaLDC | Administração, busca por competência, não comparecimento, permissões e mensagens |
| [#29](https://github.com/CarlaLDC/projeto-aloca/pull/29) | Camila-cvs | CI frontend/backend, Testcontainers, publicação de relatórios e significado do gate |
| [#43](https://github.com/CarlaLDC/projeto-aloca/pull/43) | CarlaLDC | Usuários/perfis, revogação de sessão, professor solicitante, capacidade e evidências do candidato |

O autor do PR não deve ser seu único revisor. Para #29 escolher integrante diferente de Camila-cvs; para #27/#28/#43 escolher integrante diferente de CarlaLDC. O checklist organiza a avaliação; não presume que todos os integrantes tenham participado dela.

## Procedimento

1. Abrir o PR e consultar Files changed, commits, conversas e resultados de checks.
2. Registrar o SHA e avaliar os pontos aplicáveis abaixo. Executar casos críticos quando necessário e registrar comando/ambiente/resultado reais.
3. Escrever a avaliação na própria conta do integrante: Files changed → Review changes → comentário com parecer e, quando disponível, Approve ou Request changes. Em PR já mesclado, registrar comentário retrospectivo identificado, caso a interface não ofereça review formal.
4. Relacionar achados a arquivo/linha, impacto e ação. Não marcar "sem achados" por ausência de tempo ou por confiar somente no CI.
5. Corrigir achados necessários, associar commit e permitir reavaliação. Preservar a avaliação inicial.
6. Atualizar a tabela de registro com link da review/comentário, data, revisor, SHA e resultado.

Para provar revisão por pares não basta mesclar o PR deste documento. É necessário avaliar as alterações de produto/testes listadas e preservar a manifestação independente.

## Checklist técnico

### Segurança e perfis

- [ ] Endpoints administrativos recusam solicitante, professor e responsável sem papel administrativo.
- [ ] Professor cria, altera e cancela apenas reservas próprias, sem poder aprovar recursos restritos.
- [ ] Alteração de perfil, senha e ativação invalida tokens anteriores; contas inativas não acessam API.
- [ ] Senha/hash não aparecem nas respostas e evidências; cadastro valida e-mail único, senha e perfis.
- [ ] Administrador não consegue remover seu próprio acesso administrativo.
- [ ] Gestão de usuários preserva histórico de reservas e registra ator autenticado na auditoria.

### Regras e testes

- [ ] Fim posterior ao início, manutenção e sobreposição de sala/material/professor são conferidos no servidor.
- [ ] Testes distinguem conflito sequencial de duas requisições realmente simultâneas.
- [ ] Testes de integração usam persistência real; mocks estão limitados aos unitários.
- [ ] Aprovação exige responsável e revalida cadastros/recursos antes da decisão.
- [ ] BDD comprova novo comportamento com falha anterior válida e execução posterior aprovada.
- [ ] Ausência, ciclo automático, cancelamento e materiais em uso preservam os estados/históricos esperados.

### Interface e evidências

- [ ] Capacidade mínima chega à API e filtra resultados coerentes com os demais controles.
- [ ] Pesquisa, reserva e cancelamento são utilizáveis nas capturas de 360 e 1280 px.
- [ ] Tabela administrativa com rolagem interna não impede acesso às ações em celular.
- [ ] Teste de reinício usa outro processo e o mesmo banco em arquivo, conferindo dados/histórico anteriores.
- [ ] JaCoCo é do checkout informado; percentuais históricos não foram atribuídos a outro commit.
- [ ] Run/artefato, SHA, contagens e resultados estão vinculados; presença de ZIP não é tratada como aprovação.
- [ ] Limites de H2/MySQL, viewports, SonarCloud, WireMock e carga estão declarados corretamente.

Referências: [RTM](../../../RTM.md), [candidato](CANDIDATO.md), [relatório de testes](RELATORIO-TESTES.md), [defeitos](../defeitos.md).

## Modelo de avaliação individual

Copiar e preencher **na conta do revisor**, com fatos observados:

- PR e SHA inspecionado:
- Revisor / data:
- Relação com a alteração (não sou o autor / coautoria a declarar):
- Escopo e arquivos examinados:
- Testes/evidências consultados:
- Execuções adicionais (comando, ambiente e resultado), ou "não executei":
- Achados (arquivo/linha, impacto, ação), ou "sem achados no escopo examinado", com limites:
- Parecer: aprovado no escopo / alterações solicitadas / inconclusivo:
- Restrições e riscos remanescentes:
- Revisão retrospectiva: sim/não:
- Link para review/comentário:

Não usar este modelo preenchido automaticamente como assinatura de outra pessoa.

## Registro

| PR | Revisor | SHA | Data | Link da avaliação | Parecer |
|---|---|---|---|---|---|
| #27 | ThiagoMarangoni-dev | `de28a30` | 13/11/2026 | [aprovação](https://github.com/CarlaLDC/projeto-aloca/pull/27#pullrequestreview-5884758423) | Aprovado |
| #28 | CarolinaMagalhaesLopes (solicitada) | Pendente | Pendente | Pendente | **Sem avaliação registrada**; PR já mesclado |
| #29 | CarlaLDC | `e6e2824` | 13/11/2026 | [aprovação](https://github.com/CarlaLDC/projeto-aloca/pull/29#pullrequestreview-5884716387) | Aprovado |
| #43 | CarolinaMagalhaesLopes e Camila-cvs | `d4c8189` | 13/11/2026 | [aprovação 1](https://github.com/CarlaLDC/projeto-aloca/pull/43#pullrequestreview-5884757782), [aprovação 2](https://github.com/CarlaLDC/projeto-aloca/pull/43#pullrequestreview-5884757785) | Aprovado |

Dados conferidos nas revisões do GitHub em 13/11/2026.

Somente alterar o status de revisão por pares na RTM após vincular avaliações reais. Uma aprovação com escopo restrito não equivale à homologação global da aplicação.


## Solicitação registrada

A indicação anterior foi substituída por Camila-cvs. O GitHub confirmou a remoção de CarolinaMagalhaesLopes dos PRs #27, #28, #29, #43 e #44 e a solicitação a Camila-cvs nos PRs #27, #28, #43 e #44. A pedido de Carla, o GitHub confirmou a solicitação de revisão do PR #29 para CarlaLDC, diferente da autora Camila-cvs. PRs mesclados ficam na lista Closed/Merged; as solicitações neles são retrospectivas, não comprovam revisão antes do merge. Nenhuma avaliação foi presumida.
