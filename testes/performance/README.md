# Testes de desempenho e rastreabilidade — Projeto Aloca

Esta pasta contém JMeter, RTM completa, matriz de auditoria, defeitos/impedimentos e relatório final.

## Estado real
- **RTM:** RF-01..RF-14 e RNF-01..RNF-09 relacionados a risco, teste e referência/evidência. A estrutura documental está completa; a execução de todos os testes críticos não está comprovada.
- **Auditoria RF-10:** detalhe da reserva acrescenta eventos locais em edição/cancelamento; aprovação/rejeição e retirada/devolução não registram evento no código inspecionado. Os eventos atuais não registram explicitamente estado anterior/novo nem persistem no servidor. Ver [auditoria-transicoes.md](./auditoria-transicoes.md).
- **JMeter:** o plano executa GETs repetidos às rotas Angular SPA. Reserva/aprovação/movimentação são simuladas em memória e não há API/backend documentado; JMeter HTTP não consegue acionar corretamente esses eventos client-side. Não inventar POSTs/URLs.

## Executar carga das rotas SPA
Com Apache JMeter 5.6.x e ambiente de teste autorizado:

    mkdir -p resultados
    jmeter -n -t testes/performance/jmeter/aloca-spa-load-test.jmx -Jprotocol=http -Jhost=localhost -Jport=4200 -l resultados/aloca-spa.jtl -e -o resultados/html

Plano: 50 usuários, ramp-up 60 s, duração 600 s; sete rotas SPA em loop. Mede apenas entrega HTTP/servidor estático/Angular dev server, não operações de negócio. Verificar p95 no Aggregate Report/HTML.

## Para JMeter sobre reservas reais
Fornecer API/base URL de homologação, endpoints e payloads, autenticação de teste, massa sintética, política de limpeza e autorização. Com isso, modelar pesquisa, criação, edição, cancelamento, aprovação/rejeição e requisições concorrentes para o mesmo recurso/horário.

Não executar em produção sem autorização e janela aprovada. Não usar dados pessoais reais na massa de carga.

## Evidências
JMX, RTM e documentos são artefatos de planejamento/inspeção. Não há JTL, dashboard HTML ou relatório de execução nesta entrega.
