# Resultado da carga 20261113-campanha-02

**Decisão automática: APROVADO** (critérios abaixo; gerado por `analisar_resultados.py`).

Massa isolada `CARGA-20261113-campanha-02`: 50 usuários, 190 recursos e 90 professores criados só para esta execução.

Metas (RNF-01): P95 da pesquisa de disponibilidade (PE-01) <= 2000 ms; demais operações (PE-02, PE-03) <= 3000 ms; logins fora do critério.

## Critérios

| Critério | Resultado | Obtido |
|---|---|---|
| PE-01 executado com 50 usuários pesquisando | ✅ | 31298 pesquisas |
| Uma única reserva aceita por disputa (JTL + PostgreSQL) | ✅ | 150/150 disputas válidas; violações: [] |
| Nenhuma dupla reserva | ✅ | 0 disputas com mais de uma aceita |
| Todas as disputas com 10 tentativas simultâneas | ✅ | 150/150 |
| Disputas executadas = disputas planejadas | ✅ | 150/150 |
| Taxa de erro inesperado total < 1.0% | ✅ | 0.0% |
| Nenhuma resposta 5xx ou falha de conexão | ✅ | 0 |
| P95 'PE-01 Pesquisa professores' <= 2000 ms (regime) | ✅ | 7 ms |
| P95 'PE-01 Pesquisa recursos do dia' <= 2000 ms (regime) | ✅ | 8 ms |
| P95 'PE-01 Pesquisa salas filtradas' <= 2000 ms (regime) | ✅ | 8 ms |
| P95 'PE-02 Alterar reserva' <= 3000 ms (regime) | ✅ | 19 ms |
| P95 'PE-02 Cancelar reserva' <= 3000 ms (regime) | ✅ | 16 ms |
| P95 'PE-02 Consultar detalhe' <= 3000 ms (regime) | ✅ | 10 ms |
| P95 'PE-02 Criar reserva' <= 3000 ms (regime) | ✅ | 12 ms |
| P95 'PE-03 Disputa MATERIAL' <= 3000 ms (regime) | ✅ | 1249 ms |
| P95 'PE-03 Disputa PROFESSOR' <= 3000 ms (regime) | ✅ | 1163 ms |
| P95 'PE-03 Disputa SALA' <= 3000 ms (regime) | ✅ | 110 ms |
| PE-02: criadas no JTL = persistidas no PostgreSQL | ✅ | 6230 x 6230 |
| PE-02: canceladas com auditoria (>=3 eventos) | ✅ | 0 sem histórico completo |

## Totais da execução

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTAL | 57774 | 43.28 | 0.0% | 18.5 | 6 | 14 | 17 | 490 | 1612 | 200: 50044, 201: 6380, 409: 1350 |
| PE-01 (pesquisa) | 31298 | 47.48 | 0.0% | 5.5 | 5 | 7 | 8 | 10 | 42 | 200: 31298 |
| PE-02 (ciclo) | 24916 | 37.76 | 0.0% | 10.6 | 10 | 16 | 17 | 23 | 141 | 200: 18686, 201: 6230 |
| PE-03 (disputas) | 1500 | 3.55 | 0.0% | 417.9 | 269 | 1050 | 1167 | 1322 | 1612 | 201: 150, 409: 1350 |

## Por operação — execução completa

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-01 Login | 50 | 0.85 | 0.0% | 83.6 | 83 | 91 | 93 | 94 | 94 | 200: 50 |
| PE-01 Pesquisa professores | 10421 | 15.86 | 0.0% | 4.6 | 4 | 6 | 7 | 9 | 29 | 200: 10421 |
| PE-01 Pesquisa recursos do dia | 10445 | 15.85 | 0.0% | 5.8 | 6 | 7 | 8 | 10 | 42 | 200: 10445 |
| PE-01 Pesquisa salas filtradas | 10432 | 15.86 | 0.0% | 6.0 | 6 | 8 | 8 | 11 | 35 | 200: 10432 |
| PE-02 Alterar reserva | 6216 | 9.48 | 0.0% | 14.6 | 14 | 18 | 20 | 26 | 135 | 200: 6216 |
| PE-02 Cancelar reserva | 6208 | 9.48 | 0.0% | 11.6 | 11 | 15 | 16 | 19 | 121 | 200: 6208 |
| PE-02 Consultar detalhe | 6222 | 9.47 | 0.0% | 6.8 | 7 | 9 | 10 | 13 | 38 | 200: 6222 |
| PE-02 Criar reserva | 6230 | 9.46 | 0.0% | 8.8 | 8 | 12 | 14 | 21 | 82 | 201: 6230 |
| PE-02 Login | 40 | 0.69 | 0.0% | 90.3 | 89 | 94 | 98 | 141 | 141 | 200: 40 |
| PE-03 Disputa MATERIAL | 500 | 1.2 | 0.0% | 625.9 | 624 | 1169 | 1249 | 1382 | 1612 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.2 | 0.0% | 568.7 | 562 | 1069 | 1163 | 1299 | 1594 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.2 | 0.0% | 59.2 | 56 | 100 | 110 | 252 | 389 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.22 | 0.0% | 86.9 | 86 | 91 | 94 | 94 | 94 | 200: 10 |

## Por operação — regime (descartados os primeiros 60 s de cada fase)

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-01 Pesquisa professores | 9935 | 16.56 | 0.0% | 4.6 | 4 | 6 | 7 | 9 | 29 | 200: 9935 |
| PE-01 Pesquisa recursos do dia | 9928 | 16.55 | 0.0% | 5.8 | 6 | 7 | 8 | 10 | 42 | 200: 9928 |
| PE-01 Pesquisa salas filtradas | 9937 | 16.57 | 0.0% | 6.0 | 6 | 8 | 8 | 11 | 35 | 200: 9937 |
| PE-02 Alterar reserva | 5923 | 9.88 | 0.0% | 14.4 | 14 | 18 | 19 | 23 | 135 | 200: 5923 |
| PE-02 Cancelar reserva | 5922 | 9.88 | 0.0% | 11.5 | 11 | 15 | 16 | 19 | 121 | 200: 5922 |
| PE-02 Consultar detalhe | 5913 | 9.86 | 0.0% | 6.7 | 6 | 9 | 10 | 13 | 38 | 200: 5913 |
| PE-02 Criar reserva | 5914 | 9.86 | 0.0% | 8.4 | 8 | 11 | 12 | 16 | 79 | 201: 5914 |
| PE-03 Disputa MATERIAL | 500 | 1.2 | 0.0% | 625.9 | 624 | 1169 | 1249 | 1382 | 1612 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.2 | 0.0% | 568.7 | 562 | 1069 | 1163 | 1299 | 1594 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.2 | 0.0% | 59.2 | 56 | 100 | 110 | 252 | 389 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.22 | 0.0% | 86.9 | 86 | 91 | 94 | 94 | 94 | 200: 10 |

“Erro inesperado” exclui o 409 das perdedoras de cada disputa, que é a resposta correta; qualquer 5xx, timeout, 409 fora de disputa ou asserção violada conta como erro.

## PE-01: pesquisa de disponibilidade

- 31298 pesquisas (47.48 req/s) depois de PE-02/PE-03, com a base já carregada pelas reservas da campanha; cada usuário pesquisa um dia com reservas das disputas.
- Em regime: P95 8 ms, P99 10 ms, máximo 42 ms; erro inesperado 0.0% (meta: P95 <= 2000 ms).

## Comprovação: uma única reserva aceita por disputa

- Disputas: 150 (MATERIAL: 50, PROFESSOR: 50, SALA: 50).
- Tentativas: 1500; aceitas (201): 150; recusadas (409): 1350; outros: 0.
- PostgreSQL: 150 reservas persistidas para 150 disputas; em cada uma, a única linha persistida é a mesma atividade que recebeu 201 no JTL.
- Espalhamento entre o primeiro e o último envio de cada disputa: mediana 1 ms, máximo 5 ms (Synchronizing Timer libera todas as threads juntas).
- Disputas violando a regra: **0**. Detalhe por disputa em `disputas.csv`.

## Consistência do PE-02 no banco

- Criadas (201 no JTL): 6230; persistidas: 6230.
- Canceladas (JTL): 6208; canceladas no banco: 6208; não canceladas: 22 (iterações interrompidas pelo fim do tempo).
- Canceladas sem histórico completo (criação, alteração, cancelamento): 0.
