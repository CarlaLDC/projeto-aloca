# Relatório final — Testes de desempenho e rastreabilidade

- Data: 2026-11-12
- Repositório: https://github.com/CarlaLDC/projeto-aloca
- Branch: qa/performance-jmeter
- Status: RTM completa em estrutura; auditoria parcial; execução JMeter de negócio pendente.

## 1. Resumo executivo
A RTM foi ampliada para RF-01..RF-14 e RNF-01..RNF-09, incluindo risco, teste, referência/evidência e status. Também foi criada uma matriz de transições de auditoria RF-10.

A inspeção do código indica aplicação Angular com dados/estados demonstrativos em memória. Foi criado `DemoAuditLog`, integrado à criação, aprovação, rejeição, edição, cancelamento, retirada e devolução. Os eventos incluem estado anterior/novo e timestamp, mas são mantidos apenas em memória e usam ator demonstrativo não autenticado. Não há persistência no servidor; conclusão operacional continua ausente.

O JMX executa GETs repetidos às rotas SPA. Não foi localizada API/backend de reserva documentado. Portanto, não há como afirmar que JMeter executou operações reais de reserva.

**Parecer geral: parcialmente preparado; critérios críticos ainda sem comprovação de execução.**

## 2. Resultados e limites
| Item | Meta | Resultado | Situação |
|---|---|---|---|
| RTM | todos os RF/RNF com risco, teste e evidência | RF-01..14 e RNF-01..09 mapeados | Estrutura documental concluída; evidência de execução pendente |
| Auditoria | toda transição com ator, timestamp, estado anterior/novo e persistência | eventos locais adicionados para criação, aprovação, rejeição, edição, cancelamento, retirada e devolução; sem persistência/ator autenticado | Parcial; RF-10 não aprovado |
| JMeter SPA | 50 usuários, rotas HTTP | plano parametrizado; não executado | Pendente |
| JMeter reserva real | operações reais de negócio | API/ambiente não documentados | Bloqueado |
| p95 disponibilidade | <=2.000 ms | sem medição | Inconclusivo |
| p95 demais operações | <=3.000 ms | sem medição | Inconclusivo |
| Concorrência | exatamente uma reserva aceita | não executado | Bloqueado |

Não há JTL, dashboard HTML, screenshots ou logs de execução nesta entrega. Nenhuma latência, throughput ou taxa de erro foi inventada.

## 3. Plano JMeter
50 usuários, ramp-up 60 s, duração 600 s, loop das rotas /login, /disponibilidade, /nova-reserva, /minhas-reservas, /aprovacoes, /administracao e /relatorios. O JMX mede resposta HTTP de páginas; não mede ações client-side nem API de reserva. Os limites individuais não substituem a análise p95.

## 4. Auditoria RF-10
Consulte [auditoria-transicoes.md](./auditoria-transicoes.md). Foram mapeados criação, aprovação, rejeição, cancelamento, edição, conclusão, retirada, devolução, tentativa de exclusão em uso e persistência após reinício. A trilha compartilhada local agora cobre criação, aprovação, rejeição, edição, cancelamento, retirada e devolução. Continua sem persistência, ator autenticado e fluxo de conclusão.

## 5. Impedimentos/lacunas
- IMP-001: API/backend e URL de homologação não documentados.
- IMP-002: credenciais por perfil e massa sintética ausentes.
- IMP-003: JTL/dashboard de execução inexistentes.
- GAP-004: eventos dos fluxos principais existem apenas no DemoAuditLog em memória; sem API transacional/persistência/ator autenticado.
- GAP-005: ator demonstrativo não autenticado e ausência de persistência; diff detalhado de edição e transição de conclusão pendentes.

## 6. Evidências e documentos
- [RTM completa](./RTM.md)
- [Plano de transições de auditoria](./auditoria-transicoes.md)
- [Registro de defeitos e impedimentos](./defeitos.md)
- [JMX SPA](./jmeter/aloca-spa-load-test.jmx)
- [Instruções](./README.md)

Estes artefatos comprovam planejamento e inspeção estática, não execução de carga nem auditoria de ponta a ponta.

## 7. Conclusão
A estrutura da RTM foi fechada. O aceite técnico continua pendente para requisitos críticos, principalmente RF-06/RNF-02 (concorrência), RF-10 (auditoria completa/persistente) e RNF-01 (carga sobre operações reais). Para concluir, disponibilizar API e ambiente autorizados, dados sintéticos e credenciais; completar o mecanismo de auditoria para todas as transições; executar testes funcionais/de integração e JMeter; anexar JTL, dashboard, logs e resultados.
