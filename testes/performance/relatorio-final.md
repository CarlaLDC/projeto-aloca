# Relatório final — Testes de desempenho

- Data: 2026-11-12
- Repositório: https://github.com/CarlaLDC/projeto-aloca
- Branch: qa/performance-jmeter
- Status: preparação documental concluída; execução de carga de negócio pendente.

## 1. Resumo executivo
O repositório inspecionado contém aplicação Angular e rotas de interface. Não foi localizada API/backend documentado, URL de homologação, credenciais ou massa de teste. O JMX criado exercita apenas entrega HTTP de páginas SPA; não mede pesquisa real, gravação de reservas, persistência ou concorrência.

**Parecer: inconclusivo / execução pendente.** Não há base para declarar RNF-01 aprovado ou reprovado.

## 2. Critérios
RNF-01 define até 50 usuários simultâneos, p95 de pesquisa de disponibilidade <= 2 s e p95 de operações comuns <= 3 s. RF-06/RNF-02 exigem exatamente uma solicitação aceita entre duas solicitações conflitantes simultâneas.

## 3. Configuração preparada
Apache JMeter 5.6.x; 50 usuários; ramp-up 60 s; duração 600 s; rotas login, disponibilidade, nova reserva, minhas reservas, aprovações, administração e relatórios. O JMX verifica HTTP 200 e limites individuais de 2.000/3.000 ms. O p95 deve ser verificado no Aggregate Report/HTML.

## 4. Resultados
| Métrica | Meta | Resultado | Situação |
|---|---:|---|---|
| Usuários simultâneos | 50 | Não executado | Pendente |
| p95 disponibilidade | <=2.000 ms | Sem medição | Inconclusivo |
| p95 demais operações | <=3.000 ms | Sem medição | Inconclusivo |
| Taxa de erro HTTP | 0% esperado | Sem medição | Inconclusivo |
| Reserva conflitante concorrente | Exatamente 1 aceita | Não executado | Bloqueado |

Nenhum JTL, dashboard HTML, screenshot ou log de execução foi produzido. Nenhuma latência, throughput ou taxa de erro foi estimada.

## 5. Defeitos/impedimentos
IMP-001: ambiente/API não documentados. IMP-002: credenciais e massa sintética ausentes. IMP-003: execução e evidências JMeter pendentes. Defeitos de produto confirmados: nenhum nesta etapa.

## 6. Evidências versionadas
- [Plano JMeter](./jmeter/aloca-spa-load-test.jmx)
- [RTM](./RTM.md)
- [Registro de defeitos/impedimentos](./defeitos.md)
- [Requisitos funcionais](../../src/docs/requisitos-funcionais.md)
- [Requisitos não funcionais](../../src/docs/requisitos-nao-funcionais.md)

Estes links comprovam planejamento, não execução.

## 7. Conclusão
A documentação está preparada para revisão, mas o ciclo de desempenho não pode ser encerrado como aprovado. Para fechar, disponibilizar ambiente/API de homologação, credenciais e massa sintética; executar carga sobre endpoints reais, validar p95/erros, testar concorrência e anexar JTL/dashboard/logs vinculados ao commit e ambiente.
