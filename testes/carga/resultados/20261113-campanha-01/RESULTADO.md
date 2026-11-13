# Resultado da carga 20261113-campanha-01

**Decisão automática: APROVADO** (critérios abaixo; gerado por `analisar_resultados.py`).

Massa isolada `CARGA-20261113-campanha-01`: 50 usuários, 190 recursos e 90 professores criados só para esta execução.

## Critérios

| Critério | Resultado | Obtido |
|---|---|---|
| Uma única reserva aceita por disputa (JTL + MySQL) | ✅ | 150/150 disputas válidas; violações: [] |
| Nenhuma dupla reserva | ✅ | 0 disputas com mais de uma aceita |
| Todas as disputas com 10 tentativas simultâneas | ✅ | 150/150 |
| Disputas executadas = disputas planejadas | ✅ | 150/150 |
| Taxa de erro inesperado total < 1.0% | ✅ | 0.0% |
| Nenhuma resposta 5xx ou falha de conexão | ✅ | 0 |
| P95 'PE-02 Alterar reserva' <= 3000 ms (regime) | ✅ | 18 ms |
| P95 'PE-02 Cancelar reserva' <= 3000 ms (regime) | ✅ | 14 ms |
| P95 'PE-02 Consultar detalhe' <= 3000 ms (regime) | ✅ | 9 ms |
| P95 'PE-02 Criar reserva' <= 3000 ms (regime) | ✅ | 15 ms |
| P95 'PE-03 Disputa MATERIAL' <= 3000 ms (regime) | ✅ | 1624 ms |
| P95 'PE-03 Disputa PROFESSOR' <= 3000 ms (regime) | ✅ | 1606 ms |
| P95 'PE-03 Disputa SALA' <= 3000 ms (regime) | ✅ | 145 ms |
| PE-02: criadas no JTL = persistidas no MySQL | ✅ | 6245 x 6245 |
| PE-02: canceladas com auditoria (>=3 eventos) | ✅ | 0 sem histórico completo |

## Totais da execução

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTAL | 26481 | 40.13 | 0.0% | 42.8 | 10 | 17 | 43 | 1247 | 2172 | 200: 18736, 201: 6395, 409: 1350 |
| PE-02 (ciclo) | 24971 | 37.84 | 0.0% | 10.8 | 10 | 15 | 17 | 26 | 120 | 200: 18726, 201: 6245 |
| PE-03 (disputas) | 1500 | 3.2 | 0.0% | 575.1 | 368 | 1442 | 1586 | 1697 | 2172 | 201: 150, 409: 1350 |

## Por operação — execução completa

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-02 Alterar reserva | 6231 | 9.5 | 0.0% | 14.2 | 13 | 18 | 20 | 30 | 57 | 200: 6231 |
| PE-02 Cancelar reserva | 6217 | 9.49 | 0.0% | 10.4 | 10 | 13 | 15 | 22 | 50 | 200: 6217 |
| PE-02 Consultar detalhe | 6238 | 9.5 | 0.0% | 6.9 | 6 | 9 | 10 | 16 | 35 | 200: 6238 |
| PE-02 Criar reserva | 6245 | 9.49 | 0.0% | 11.2 | 10 | 14 | 17 | 25 | 77 | 201: 6245 |
| PE-02 Login | 40 | 0.68 | 0.0% | 94.1 | 92 | 100 | 112 | 120 | 120 | 200: 40 |
| PE-03 Disputa MATERIAL | 500 | 1.08 | 0.0% | 828.0 | 839 | 1547 | 1624 | 1705 | 1916 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.08 | 0.0% | 818.2 | 835 | 1545 | 1606 | 1756 | 2172 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.08 | 0.0% | 79.0 | 77 | 134 | 145 | 198 | 315 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.23 | 0.0% | 89.5 | 90 | 92 | 93 | 93 | 93 | 200: 10 |

## Por operação — regime (descartados os primeiros 60 s)

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-02 Alterar reserva | 5936 | 9.9 | 0.0% | 13.7 | 13 | 17 | 18 | 24 | 46 | 200: 5936 |
| PE-02 Cancelar reserva | 5935 | 9.9 | 0.0% | 10.1 | 10 | 13 | 14 | 19 | 50 | 200: 5935 |
| PE-02 Consultar detalhe | 5933 | 9.9 | 0.0% | 6.7 | 6 | 8 | 9 | 14 | 35 | 200: 5933 |
| PE-02 Criar reserva | 5930 | 9.89 | 0.0% | 10.8 | 10 | 13 | 15 | 20 | 39 | 201: 5930 |
| PE-03 Disputa MATERIAL | 500 | 1.08 | 0.0% | 828.0 | 839 | 1547 | 1624 | 1705 | 1916 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.08 | 0.0% | 818.2 | 835 | 1545 | 1606 | 1756 | 2172 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.08 | 0.0% | 79.0 | 77 | 134 | 145 | 198 | 315 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.23 | 0.0% | 89.5 | 90 | 92 | 93 | 93 | 93 | 200: 10 |

“Erro inesperado” exclui o 409 das perdedoras de cada disputa, que é a resposta correta; qualquer 5xx, timeout, 409 fora de disputa ou asserção violada conta como erro.

## Comprovação: uma única reserva aceita por disputa

- Disputas: 150 (MATERIAL: 50, PROFESSOR: 50, SALA: 50).
- Tentativas: 1500; aceitas (201): 150; recusadas (409): 1350; outros: 0.
- MySQL: 150 reservas persistidas para 150 disputas; em cada uma, a única linha persistida é a mesma atividade que recebeu 201 no JTL.
- Espalhamento entre o primeiro e o último envio de cada disputa: mediana 1 ms, máximo 6 ms (Synchronizing Timer libera todas as threads juntas).
- Disputas violando a regra: **0**. Detalhe por disputa em `disputas.csv`.

## Consistência do PE-02 no banco

- Criadas (201 no JTL): 6245; persistidas: 6245.
- Canceladas (JTL): 6217; canceladas no banco: 6217; não canceladas: 28 (iterações interrompidas pelo fim do tempo).
- Canceladas sem histórico completo (criação, alteração, cancelamento): 0.
