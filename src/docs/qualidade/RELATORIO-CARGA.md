# Relatório de carga — pesquisa, criação e concorrência de reservas (PE-01 / PE-02 / PE-03)

| Item | Valor |
|---|---|
| Execução principal | `20261113-campanha-03`, 2026-11-13 04:13–04:36 UTC |
| Código da API | develop + as 9 branches de back-end (PostgreSQL), com a correção do DEF-PERF-001 (`feat/back-reservas` `ed41485`). Detalhe em `ambiente.txt` |
| Plano e scripts | [testes/carga](../../../testes/carga/README.md) — `aloca-reservas-carga.jmx`, JMeter 5.6.3 |
| Ambiente | 4 vCPU, 15 GiB, Linux 6.18; OpenJDK 21.0.12; PostgreSQL 17.11 em contêiner `tmpfs` descartável; API (jar, Hikari padrão) e JMeter na mesma máquina |
| Perfil | Fase 1: 50 usuários simultâneos, 40 no ciclo PE-02 + 10 por disputa no PE-03, 150 disputas. Fase 2: 50 usuários só pesquisando (PE-01). Cada fase: ramp-up de 60 s e 660 s (60 s de aquecimento + 600 s de medição) |
| Massa | Isolada, prefixo `CARGA-20261113-campanha-03`: 50 Solicitantes, 190 recursos e 90 professores criados pela API só para esta execução; PE-01 reaproveita os mesmos 50 usuários |
| Evidências | [resultados/20261113-campanha-03](../../../testes/carga/resultados/20261113-campanha-03/) — `resultados.jtl`, `dashboard/index.html`, `RESULTADO.md`, `resumo.json`, `disputas.csv`, `ambiente.txt`, logs da API e do JMeter |

## Resultado

**Decisão: aprovado para RNF-01 (pesquisa ≤ 2 s e demais operações ≤ 3 s, P95, 50 usuários) e para RF-06/RNF-02 sob carga, neste ambiente.** O DEF-PERF-001 foi corrigido e validado nesta campanha.

| Critério | Meta | Obtido (regime) |
|---|---|---|
| PE-01 Pesquisa recursos do dia | P95 ≤ 2.000 ms | **8 ms** |
| PE-01 Pesquisa salas filtradas | P95 ≤ 2.000 ms | **8 ms** |
| PE-01 Pesquisa professores | P95 ≤ 2.000 ms | **6 ms** |
| PE-02 Criar / Consultar / Alterar / Cancelar | P95 ≤ 3.000 ms | 12 / 9 / 19 / 16 ms |
| PE-03 Disputa SALA / PROFESSOR / MATERIAL | P95 ≤ 3.000 ms | 117 / 87 / 90 ms |
| Uma única reserva aceita por disputa (JTL + PostgreSQL) | 150/150 | **150/150**, 0 dupla reserva |
| Erro inesperado / 5xx | < 1% / 0 | **0,0% / 0** |
| PE-02 criadas = persistidas; canceladas com auditoria | iguais; 0 sem histórico | 6.252 = 6.252; 0 |

### Métricas (execução completa)

| Operação | Amostras | Req/s | Erro inesperado | P50 ms | P90 | P95 | P99 | Máx |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **Total** | 57.914 | **43,38** | **0,0%** | 6 | 14 | **17** | **61** | 291 |
| PE-01 Pesquisa recursos do dia | 10.473 | 15,89 | 0,0% | 5 | 7 | 8 | 10 | 32 |
| PE-01 Pesquisa salas filtradas | 10.451 | 15,89 | 0,0% | 5 | 7 | 8 | 10 | 26 |
| PE-01 Pesquisa professores | 10.437 | 15,90 | 0,0% | 4 | 6 | 6 | 8 | 27 |
| PE-02 Criar reserva | 6.252 | 9,50 | 0,0% | 8 | 12 | 14 | 20 | 70 |
| PE-02 Consultar detalhe | 6.246 | 9,51 | 0,0% | 6 | 9 | 10 | 13 | 107 |
| PE-02 Alterar reserva | 6.233 | 9,51 | 0,0% | 14 | 18 | 20 | 26 | 58 |
| PE-02 Cancelar reserva | 6.222 | 9,51 | 0,0% | 11 | 15 | 16 | 20 | 35 |
| PE-03 Disputa SALA | 500 | 1,63 | 0,0% | 54 | 99 | 117 | 175 | 291 |
| PE-03 Disputa PROFESSOR | 500 | 1,63 | 0,0% | 42 | 76 | 87 | 137 | 174 |
| PE-03 Disputa MATERIAL | 500 | 1,63 | 0,0% | 45 | 79 | 90 | 109 | 118 |

Códigos HTTP: 200 = 50.162, 201 = 6.402, 409 = 1.350 (todos em disputas), 5xx = 0. A taxa de erro inesperado exclui os 409 das perdedoras, que são a resposta correta. O recorte em regime descarta os primeiros 60 s de cada fase. O dashboard usa outro estimador de percentil e pode diferir em poucos milissegundos.

### PE-01 — pesquisa de disponibilidade

- 31.361 pesquisas (47,6 req/s) com 50 usuários, depois de PE-02/PE-03, com a base já carregada pelas reservas da campanha (mais de 6.000 reservas e as 150 vencedoras das disputas ainda `APROVADA`). Cada iteração pesquisa um dia que tem reservas das disputas.
- Todas as respostas 200 com a lista no corpo (cerca de 14 KB para os recursos do dia). P95 de 6 a 8 ms, máximo de 32 ms: muito abaixo da meta de 2 s.

### Uma única reserva aceita por disputa

- 150 disputas (50 por sala, 50 por professor, 50 por material), 1.500 tentativas, 10 por disputa enviadas juntas (espalhamento mediano de 1 ms, máximo de 13 ms).
- 150 aceitas (201, `APROVADA`), exatamente 1 por disputa; 1.350 recusadas (409 com `conflitos`); 0 outros códigos.
- No PostgreSQL, 150 linhas para as disputas; em todas, a única linha persistida e ativa é a mesma atividade que recebeu 201 no JTL.

### DEF-PERF-001 — antes e depois da correção, no PostgreSQL

A campanha `20261113-campanha-02` rodou o mesmo perfil no mesmo ambiente, com o mesmo código, mas **sem** a correção
(linha de base; [resumo](../../../testes/carga/resultados/20261113-campanha-02/RESULTADO.md)). Tempo dos 409 por tipo de disputa:

| Disputa | 409 antes (campanha-02): mediana / P95 | 409 depois (campanha-03): mediana / P95 |
|---|---|---|
| SALA | 61 / 112 ms | 59 / 118 ms |
| PROFESSOR | 620 / **1.167 ms** | 47 / **90 ms** |
| MATERIAL | 686 / **1.256 ms** | 49 / **90 ms** |

Com a correção, o 409 por professor ou material ficou tão rápido quanto o da disputa por sala, e o P99 total caiu de
490 ms para 61 ms. A regra continua igual: 150/150 disputas com uma única aceita nas duas campanhas.

## Análise

1. **RNF-01:** a pesquisa de disponibilidade (PE-01) e todas as operações de reserva ficaram muito abaixo das metas com 50 usuários. O pior P95 de negócio foi o da disputa por sala, com 117 ms.
2. **RF-06 / RNF-02 sob volume:** com 40 usuários mutando reservas em paralelo, as 10 tentativas simultâneas de cada disputa produziram exatamente uma aceitação, nos três tipos de alvo e com horários parcialmente sobrepostos. Não houve deadlock nem 5xx no `api.log`.
3. **DEF-PERF-001:** a causa apontada na campanha 01 se confirmou. A busca por "outro local, mesmo horário" varria todos os espaços sob a trava, mesmo quando o conflito era do professor ou do material. Agora esse passo só roda quando o conflito é do próprio local, sem mudar o resultado da API. Testes unitários em `AlternativasConflitoUnitTest` falham no código anterior e passam no novo.

## Histórico

| Execução | Banco | Código | Resultado |
|---|---|---|---|
| `20261113-campanha-01` | MySQL 8.4 | develop `ca5919c` | Aprovada (PE-02/PE-03); DEF-PERF-001 encontrado (P95 do 409 de 1,6 s); PE-01 não executado |
| `20261113-campanha-02` | PostgreSQL 17 | develop + 9 branches, sem a correção | Aprovada (PE-01/02/03); DEF-PERF-001 reproduzido (P95 de 1,17–1,26 s). Pacote reduzido (sem JTL e dashboard) |
| `20261113-campanha-03` | PostgreSQL 17 | develop + 9 branches, com a correção | **Aprovada** (PE-01/02/03); DEF-PERF-001 validado |

## Limites

- Uma única máquina, com JMeter, API e PostgreSQL competindo por 4 vCPU; os números não representam um servidor de produção.
- Um único nó da API; a concorrência entre várias instâncias não foi avaliada.
- A massa sintética é isolada; o comportamento com o volume de dados de produção não foi medido.
- A execução foi feita sobre a árvore equivalente ao develop depois do merge das 9 branches de back-end. Depois do merge, vale repetir pelo workflow `carga-jmeter.yml` (campanha completa sob demanda) para vincular a evidência ao SHA do develop.

## Reprodução

```bash
JMETER_HOME=/caminho/apache-jmeter-5.6.3 RUN_ID=<novo-id> testes/carga/executar-carga.sh
```

O workflow `.github/workflows/carga-jmeter.yml` repete um perfil reduzido em PRs e a campanha completa sob demanda, publicando o mesmo pacote de evidências como artefato.
