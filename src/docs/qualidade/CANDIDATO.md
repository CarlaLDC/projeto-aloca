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

- `UsuariosApiTest`: perfis, cadastro inválido/duplicado, ausência de senha/hash na resposta, revogação de tokens após edição/desativação/troca de senha, proteção do próprio administrador e ciclo do professor solicitante. Banco MySQL/Testcontainers na suíte completa.
- `scripts/persistencia-reinicio.py`: inicia JAR com H2 **em arquivo**, cria recurso/reserva, cancela com histórico, encerra o processo, inicia outro PID sobre o mesmo banco e compara integralmente a reserva e o histórico por HTTP. A massa e o arquivo do banco são temporários; nenhum banco do usuário é usado.
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

A verificação de reinício usa H2 em arquivo; a suíte de integração verifica MySQL separadamente. Os dois viewports não representam todos os dispositivos. A cobertura só vale para o checkout registrado no artefato. WireMock, SonarCloud, teste de carga e divergência de versões exigidas pelo enunciado não são resolvidos por este incremento.
