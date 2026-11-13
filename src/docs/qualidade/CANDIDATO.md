# Candidato — usuários, professor solicitante e evidências

Este incremento fecha seis itens de implementação/verificação. Os resultados só são aprovados quando os respectivos workflows terminarem com sucesso.

## Usuários e perfis

Administrador: `GET/POST /api/admin/usuarios`, `PUT /api/admin/usuarios/{id}`, `PATCH /api/admin/usuarios/{id}/ativo` (`ativo`) e `PUT /api/admin/usuarios/{id}/senha` (`senha`). Cadastro exige nome, e-mail único, unidade, perfis e senha de 8 a 72 caracteres (máximo 72 bytes UTF-8). E-mail é normalizado; hash/senha não são retornados. Desativação preserva reservas e auditoria, em vez de apagar a pessoa.

A aba **Usuários e perfis** permite cadastrar, editar, ativar/desativar e redefinir senha. As operações geram auditoria. O administrador não pode retirar seu próprio acesso administrativo; a verificação é transacional, com trava nos usuários. Não há operação de exclusão física.

Cada token contém uma versão de sessão. Edição, ativação/desativação e redefinição de senha invalidam tokens anteriores; a autenticação consulta usuário ativo, versão e perfis atuais em cada requisição. A UI exige novo login quando a própria conta é alterada. Tokens emitidos antes deste incremento deixam de ser aceitos: entre novamente.

## Professor como solicitante

PROFESSOR consulta sua agenda e pode criar, alterar e cancelar suas próprias reservas, sujeito às mesmas regras de tempo/conflito/manutenção. Não pode aprovar recursos restritos, administrar cadastros ou modificar reservas alheias. O campo de professor alocado à atividade continua distinto do solicitante que criou a reserva.

## Capacidade

Disponibilidade oferece capacidade mínima inteira não negativa. O filtro é enviado à API como `capacidadeMinima`, combinado com data e com filtros visuais de tipo/nome/localização. Limpar filtros remove a capacidade mínima.

## Evidências reproduzíveis

- `UsuariosApiTest`: perfis, cadastro inválido/duplicado, ausência de senha/hash na resposta, revogação de tokens após edição/desativação/troca de senha, proteção do próprio administrador e ciclo do professor solicitante. Banco PostgreSQL/Testcontainers na suíte completa.
- `scripts/persistencia-reinicio.py`: inicia o JAR com **PostgreSQL** (`ALOCA_DB_URL`; no workflow, um serviço descartável), cria recurso/reserva, cancela com histórico, encerra o processo, inicia outro PID sobre o mesmo banco e compara integralmente a reserva e o histórico por HTTP. Até o PR #45 a verificação usava H2 em arquivo; os resultados abaixo são dessa execução.
- `bdd/candidato-responsivo.cjs`: navegador Chromium real em **360×900 e 1280×900**, login como professor, pesquisa com capacidade, formulário/criação/cancelamento pela interface, verificação HTTP de cancelamento e administração de usuários. Preserva capturas e medidas de largura. Não substitui avaliação humana de acessibilidade/usabilidade.
- `scripts/resumo-cobertura.py`: extrai contadores do XML JaCoCo, associa SHA checkout, GITHUB_SHA e run-id, grava percentuais e reprova abaixo de **80% linhas / 70% branches**.

Workflow: **Candidato - persistência, responsividade e cobertura**. No run → Artifacts → `candidato-evidencias-<run-id>`: Surefire, JaCoCo HTML/XML, `cobertura.json`, `persistencia-reinicio.json`, `responsividade.json`, PNGs, logs e identificação do checkout. Artefatos são publicados mesmo em falha; consulte cada status, não apenas a existência do ZIP. No Actions de PR, checkout pode ser o merge temporário; os metadados explicitam isso.

## Execução local

Java 21 JDK, Maven, Node 22, Python 3 e Docker para MySQL/Testcontainers:

```bash
mvn -B -f backend/pom.xml clean verify
python3 scripts/resumo-cobertura.py
python3 scripts/persistencia-reinicio.py
npm ci
npm ci --prefix bdd
npx --prefix bdd playwright install --with-deps chromium
```

Para o navegador, iniciar API em banco descartável na porta 8080 e frontend na 4200 (proxy `/api`), então `node bdd/candidato-responsivo.cjs`. O workflow mostra os comandos completos e encerra ambos os processos ao final. Não executar os scripts de preparação de dados contra ambientes compartilhados.

## Limites

A verificação de reinício usa PostgreSQL desde o PR #45 (os resultados registrados abaixo são da execução anterior, com H2 em arquivo e suíte MySQL). Os dois viewports não representam todos os dispositivos. A cobertura só vale para o checkout registrado no artefato. WireMock, SonarCloud, teste de carga e divergência de versões exigidas pelo enunciado não são resolvidos por este incremento.

## Resultado da campanha verificada

- PR: [#43](https://github.com/CarlaLDC/projeto-aloca/pull/43).
- Candidato executável (head): `895d8e7ac9bbd9615e94ed680762a002baedf9d1`.
- Checkout real do merge temporário: `26a21f63727461dbb491e14ee9d118d5df32819a`.
- [Campanha 43340838626](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43340838626): **aprovada**.
- [Artefato original](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43340838626/artifacts/13999950492): `candidato-evidencias-43340838626`, SHA-256 `9e9a8a21202f23eab9abaa4ff45878a8a7b3bfdbcf47304fc4cdda6cb7172176`, retenção registrada até 11/02/2027.

| Verificação | Resultado observado |
|---|---|
| Backend completo MySQL/Testcontainers | **164 testes**, zero falhas, erros e ignorados |
| JaCoCo linhas | **1340/1416 = 94,63%**, meta 80% atingida |
| JaCoCo branches | **376/475 = 79,16%**, meta 70% atingida |
| Reinício H2 em arquivo | **Aprovado**: processos distintos, reserva cancelada e cinco eventos do histórico integralmente iguais; recurso preservado |
| Responsividade | **12 registros aprovados**: seis etapas por viewport (360×900 e 1280×900), incluindo pesquisa, formulário, criação, diálogo, cancelamento e usuários |
| Overflow horizontal da página | Conteúdo medido de 360/1280 px respectivamente, sem excesso nas etapas registradas; tabela administrativa tem rolagem interna |
| Gestão de usuários pela interface | Criação com perfil Professor, edição, desativação e reativação aprovadas nos dois viewports |

JSONs e metadados originais baixados do artefato estão preservados em [evidencias-candidato](evidencias-candidato/). Não foram reconstruídos. Capturas PNG, Surefire e JaCoCo completo estão no ZIP original. As capturas locais correspondentes foram inspecionadas visualmente para formulário/cancelamento em 360 px, cancelamento em 1280 px e administração em 360 px; não se presume revisão humana de todos os dispositivos.

O [CI integrado 43340838623](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43340838623) também aprovou frontend, unitários isolados, HTTP, backend e gate. O [BDD integrado 43340838436](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43340838436) e o [BDD de inativos 43340838664](https://github.com/CarlaLDC/projeto-aloca/actions/runs/43340838664) foram aprovados.

Esta campanha valida os seis itens no escopo descrito. Não homologa globalmente os demais requisitos do projeto. Commits posteriores somente de documentação não alteram o candidato executável acima; alterações de código exigem nova campanha.
