# Testes automatizados BDD — Aloca

Cenários em português (Gherkin), executados pelo Cucumber e Playwright em Chromium.
Os passos interagem com o frontend real; cada cenário recebe um contexto de navegador novo.

## Executar

Use Node.js 22. Na raiz do projeto:

```bash
npm ci
npm install --prefix bdd
cd bdd
npx playwright install --with-deps chromium
cd ..
npm start
```

Em outro terminal, na raiz:

```bash
npm test --prefix bdd
```

Abra `bdd/reports/cucumber.html` para visualizar cenários, passos e capturas de falhas.
O arquivo JSON também fica em `bdd/reports/`.
Para outro servidor, defina `BASE_URL`. Para acompanhar o navegador, defina `HEADED=true`.

## Cobertura

- Login: campos obrigatórios, e-mail inválido, acesso indisponível e recuperação de senha.
- Nova reserva: atividade obrigatória, data passada, término anterior ou igual ao início, revisão, retorno e conclusão local.
- Detalhe: código inexistente, bloqueio de edição/cancelamento de reservas encerradas, alteração e histórico, motivo obrigatório e cancelamento com histórico.

São 16 cenários executáveis, considerando as linhas dos esquemas.

## Limites da versão atual

Esta branch parte da main, que contém uma demonstração Angular sem backend integrado.
Login não autentica; criação, alteração e cancelamento não persistem no servidor.
Os testes verificam esse comportamento explicitamente. Não comprovam segurança da API,
permissões por perfil, conflitos entre reservas, persistência, aprovação, retirada ou devolução.
Esses cenários precisam ser implementados com a integração real; não foram substituídos por mocks que retornam sucesso.

## GitHub Actions

O workflow `BDD automatizado` executa em pushes desta branch, pull requests e acionamento manual.
O artefato `bdd-relatorios` contém HTML e JSON, com capturas incorporadas quando houver falhas.
A instalação de dependências BDD é isolada em `bdd/`; a aplicação mantém seu package-lock original.
