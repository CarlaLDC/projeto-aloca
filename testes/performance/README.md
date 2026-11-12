# Testes de desempenho — Projeto Aloca

Preparação de teste JMeter, RTM, registro de impedimentos e relatório final.

## Estado
Preparação concluída; execução de carga de negócio pendente. A aplicação inspecionada é Angular e não foi localizada API/backend documentado, URL de homologação, credenciais ou massa de dados. O JMX incluído testa somente a entrega HTTP das rotas SPA; não valida consultas reais, persistência, criação de reservas ou concorrência.

## Executar teste SPA
Com Apache JMeter 5.6.x e aplicação autorizada disponível:

    mkdir -p resultados
    jmeter -n -t testes/performance/jmeter/aloca-spa-load-test.jmx -Jprotocol=http -Jhost=localhost -Jport=4200 -l resultados/aloca-spa.jtl -e -o resultados/html

Parâmetros: 50 usuários, ramp-up 60 s, duração 600 s. Disponibilidade: limite individual 2.000 ms; demais páginas: 3.000 ms. Conferir p95 no Aggregate Report/HTML; média não substitui p95. Assertions individuais não comprovam o percentil.

Não executar em produção sem autorização e janela aprovada. Para fechar RNF-01, é necessário ambiente/API de negócio, credenciais de teste e massa sintética; executar também cenário concorrente de duas reservas para mesmo recurso/horário.

## Evidências
O JMX e os documentos versionados comprovam planejamento/configuração. Não há JTL, dashboard HTML ou capturas de execução nesta entrega; não foram inventados resultados.
