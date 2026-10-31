# Requisitos não funcionais

Estes requisitos definem metas de qualidade. Os tempos são limites propostos para os testes do projeto.

## Requisitos

- **RNF-01 - Tempo de resposta:** com até 50 usuários simultâneos, 95 de cada 100 pesquisas de disponibilidade devem responder em até 2 segundos. As demais operações comuns devem responder em até 3 segundos.
	- **Métrica:** tempo de resposta em segundos.
	- **Verificação:** teste de carga com JMeter e relatório salvo como evidência.

- **RNF-02 - Proteção contra conflito simultâneo:** duas solicitações para o mesmo recurso e horário não podem ser aceitas juntas.
	- **Métrica:** exatamente uma solicitação aceita no teste com duas solicitações conflitantes.
	- **Verificação:** teste automatizado de integração.

- **RNF-03 - Segurança:** todas as funções protegidas devem exigir usuário autenticado e respeitar seu perfil. Os dados devem ser validados e mensagens de erro não podem mostrar informações internas.
	- **Métrica:** 100% dos testes de acesso por perfil devem passar; zero vulnerabilidade crítica ou alta aberta na entrega.
	- **Verificação:** testes de autorização e validação, além da análise de segurança.

- **RNF-04 - Confiabilidade:** reservas e histórico devem continuar salvos depois que a aplicação reiniciar.
	- **Métrica:** nenhum dado confirmado pode ser perdido após reiniciar o serviço.
	- **Verificação:** teste de integração com banco de dados.

- **RNF-05 - Facilidade de uso:** as funções principais devem funcionar em celular e computador. As mensagens devem explicar o problema de forma clara.
	- **Métrica:** pesquisa, reserva e cancelamento testados em telas de 360 px e 1280 px.
	- **Verificação:** teste manual registrado.

- **RNF-06 - Cobertura de testes:** os testes devem cobrir pelo menos 80% das linhas e 70% dos branches.
	- **Métrica:** 80% de cobertura de linhas e 70% de cobertura de branches.
	- **Verificação:** relatório JaCoCo.

- **RNF-07 - Qualidade e segurança:** a entrega deve ter zero bugs críticos e zero vulnerabilidades críticas abertas.
	- **Métrica:** nenhum item crítico aberto.
	- **Verificação:** resultados do SonarCloud e da análise de dependências.

- **RNF-08 - Rastreabilidade:** todos os requisitos críticos devem estar relacionados a testes e evidências.
	- **Métrica:** 100% dos requisitos críticos registrados na RTM com risco, teste e evidência.
	- **Verificação:** revisão da RTM.

- **RNF-09 - Integração contínua:** compilação e testes devem ser executados a cada pull request.
	- **Métrica:** cada pull request deve ter resultado do pipeline.
	- **Verificação:** histórico de execução do GitHub Actions.

## Como medir os tempos

- Executar o teste no mesmo ambiente e com os mesmos dados.
- Registrar quantidade de usuários e duração do teste.
- Salvar o resultado do JMeter como evidência.
