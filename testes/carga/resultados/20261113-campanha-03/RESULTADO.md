# Resultado da carga 20261113-campanha-03

**Decisão automática: APROVADO** (critérios abaixo; gerado por `analisar_resultados.py`).

Massa isolada `CARGA-20261113-campanha-03`: 50 usuários, 190 recursos e 90 professores criados só para esta execução.

Metas (RNF-01): P95 da pesquisa de disponibilidade (PE-01) <= 2000 ms; demais operações (PE-02, PE-03) <= 3000 ms; logins fora do critério.

## Critérios

| Critério | Resultado | Obtido |
|---|---|---|
| PE-01 executado com 50 usuários pesquisando | ✅ | 31361 pesquisas |
| Uma única reserva aceita por disputa (JTL + PostgreSQL) | ✅ | 150/150 disputas válidas; violações: [] |
| Nenhuma dupla reserva | ✅ | 0 disputas com mais de uma aceita |
| Todas as disputas com 10 tentativas simultâneas | ✅ | 150/150 |
| Disputas executadas = disputas planejadas | ✅ | 150/150 |
| Taxa de erro inesperado total < 1.0% | ✅ | 0.0% |
| Nenhuma resposta 5xx ou falha de conexão | ✅ | 0 |
| P95 'PE-01 Pesquisa professores' <= 2000 ms (regime) | ✅ | 6 ms |
| P95 'PE-01 Pesquisa recursos do dia' <= 2000 ms (regime) | ✅ | 8 ms |
| P95 'PE-01 Pesquisa salas filtradas' <= 2000 ms (regime) | ✅ | 8 ms |
| P95 'PE-02 Alterar reserva' <= 3000 ms (regime) | ✅ | 19 ms |
| P95 'PE-02 Cancelar reserva' <= 3000 ms (regime) | ✅ | 16 ms |
| P95 'PE-02 Consultar detalhe' <= 3000 ms (regime) | ✅ | 9 ms |
| P95 'PE-02 Criar reserva' <= 3000 ms (regime) | ✅ | 12 ms |
| P95 'PE-03 Disputa MATERIAL' <= 3000 ms (regime) | ✅ | 90 ms |
| P95 'PE-03 Disputa PROFESSOR' <= 3000 ms (regime) | ✅ | 87 ms |
| P95 'PE-03 Disputa SALA' <= 3000 ms (regime) | ✅ | 117 ms |
| PE-02: criadas no JTL = persistidas no PostgreSQL | ✅ | 6252 x 6252 |
| PE-02: canceladas com auditoria (>=3 eventos) | ✅ | 0 sem histórico completo |

## Totais da execução

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| TOTAL | 57914 | 43.38 | 0.0% | 8.7 | 6 | 14 | 17 | 61 | 291 | 200: 50162, 201: 6402, 409: 1350 |
| PE-01 (pesquisa) | 31361 | 47.59 | 0.0% | 5.2 | 5 | 7 | 8 | 9 | 32 | 200: 31361 |
| PE-02 (ciclo) | 24993 | 37.88 | 0.0% | 10.4 | 10 | 15 | 17 | 23 | 125 | 200: 18741, 201: 6252 |
| PE-03 (disputas) | 1500 | 4.81 | 0.0% | 49.8 | 47 | 87 | 99 | 145 | 291 | 201: 150, 409: 1350 |

## Por operação — execução completa

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-01 Login | 50 | 0.85 | 0.0% | 85.1 | 85 | 90 | 93 | 103 | 103 | 200: 50 |
| PE-01 Pesquisa professores | 10437 | 15.9 | 0.0% | 4.4 | 4 | 6 | 6 | 8 | 27 | 200: 10437 |
| PE-01 Pesquisa recursos do dia | 10473 | 15.89 | 0.0% | 5.5 | 5 | 7 | 8 | 10 | 32 | 200: 10473 |
| PE-01 Pesquisa salas filtradas | 10451 | 15.89 | 0.0% | 5.7 | 5 | 7 | 8 | 10 | 26 | 200: 10451 |
| PE-02 Alterar reserva | 6233 | 9.51 | 0.0% | 14.3 | 14 | 18 | 20 | 26 | 58 | 200: 6233 |
| PE-02 Cancelar reserva | 6222 | 9.51 | 0.0% | 11.4 | 11 | 15 | 16 | 20 | 35 | 200: 6222 |
| PE-02 Consultar detalhe | 6246 | 9.51 | 0.0% | 6.7 | 6 | 9 | 10 | 13 | 107 | 200: 6246 |
| PE-02 Criar reserva | 6252 | 9.5 | 0.0% | 8.5 | 8 | 12 | 14 | 20 | 70 | 201: 6252 |
| PE-02 Login | 40 | 0.69 | 0.0% | 90.4 | 87 | 101 | 107 | 125 | 125 | 200: 40 |
| PE-03 Disputa MATERIAL | 500 | 1.63 | 0.0% | 46.5 | 45 | 79 | 90 | 109 | 118 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.63 | 0.0% | 45.0 | 42 | 76 | 87 | 137 | 174 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.63 | 0.0% | 57.8 | 54 | 99 | 117 | 175 | 291 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.22 | 0.0% | 91.4 | 91 | 94 | 96 | 96 | 96 | 200: 10 |

## Por operação — regime (descartados os primeiros 60 s de cada fase)

| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |
|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|
| PE-01 Pesquisa professores | 9950 | 16.58 | 0.0% | 4.4 | 4 | 6 | 6 | 8 | 27 | 200: 9950 |
| PE-01 Pesquisa recursos do dia | 9954 | 16.59 | 0.0% | 5.5 | 5 | 7 | 8 | 9 | 32 | 200: 9954 |
| PE-01 Pesquisa salas filtradas | 9948 | 16.58 | 0.0% | 5.6 | 5 | 7 | 8 | 10 | 26 | 200: 9948 |
| PE-02 Alterar reserva | 5943 | 9.91 | 0.0% | 14.1 | 14 | 18 | 19 | 23 | 58 | 200: 5943 |
| PE-02 Cancelar reserva | 5938 | 9.9 | 0.0% | 11.3 | 11 | 15 | 16 | 19 | 30 | 200: 5938 |
| PE-02 Consultar detalhe | 5941 | 9.91 | 0.0% | 6.6 | 6 | 9 | 9 | 12 | 107 | 200: 5941 |
| PE-02 Criar reserva | 5934 | 9.9 | 0.0% | 8.1 | 8 | 11 | 12 | 16 | 31 | 201: 5934 |
| PE-03 Disputa MATERIAL | 500 | 1.63 | 0.0% | 46.5 | 45 | 79 | 90 | 109 | 118 | 201: 50, 409: 450 |
| PE-03 Disputa PROFESSOR | 500 | 1.63 | 0.0% | 45.0 | 42 | 76 | 87 | 137 | 174 | 201: 50, 409: 450 |
| PE-03 Disputa SALA | 500 | 1.63 | 0.0% | 57.8 | 54 | 99 | 117 | 175 | 291 | 201: 50, 409: 450 |
| PE-03 Login | 10 | 2.22 | 0.0% | 91.4 | 91 | 94 | 96 | 96 | 96 | 200: 10 |

“Erro inesperado” exclui o 409 das perdedoras de cada disputa, que é a resposta correta; qualquer 5xx, timeout, 409 fora de disputa ou asserção violada conta como erro.

## PE-01: pesquisa de disponibilidade

- 31361 pesquisas (47.59 req/s) depois de PE-02/PE-03, com a base já carregada pelas reservas da campanha; cada usuário pesquisa um dia com reservas das disputas.
- Em regime: P95 7 ms, P99 9 ms, máximo 32 ms; erro inesperado 0.0% (meta: P95 <= 2000 ms).

## Comprovação: uma única reserva aceita por disputa

- Disputas: 150 (MATERIAL: 50, PROFESSOR: 50, SALA: 50).
- Tentativas: 1500; aceitas (201): 150; recusadas (409): 1350; outros: 0.
- PostgreSQL: 150 reservas persistidas para 150 disputas; em cada uma, a única linha persistida é a mesma atividade que recebeu 201 no JTL.
- Espalhamento entre o primeiro e o último envio de cada disputa: mediana 1 ms, máximo 13 ms (Synchronizing Timer libera todas as threads juntas).
- Disputas violando a regra: **0**. Detalhe por disputa em `disputas.csv`.

## Consistência do PE-02 no banco

- Criadas (201 no JTL): 6252; persistidas: 6252.
- Canceladas (JTL): 6222; canceladas no banco: 6222; não canceladas: 30 (iterações interrompidas pelo fim do tempo).
- Canceladas sem histórico completo (criação, alteração, cancelamento): 0.
