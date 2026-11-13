# Relatório de carga — criação e concorrência de reservas (PE-02 / PE-03)

| Item | Valor |
|---|---|
| Execução | `20261113-campanha-01`, 2026-11-13 01:54–02:05 UTC |
| Código da API | `ca5919c` (develop). Os commits seguintes só adicionam `testes/carga/`, sem alterar `backend/` |
| Plano e scripts | [testes/carga](../../../testes/carga/README.md) — `aloca-reservas-carga.jmx`, JMeter 5.6.3 |
| Ambiente | 4 vCPU, 15 GiB, Linux 6.18; OpenJDK 21.0.12; MySQL 8.4.13 em contêiner `tmpfs` descartável; API (jar, perfil `mysql`, Hikari padrão) e JMeter na mesma máquina |
| Perfil | 50 usuários simultâneos: 40 no ciclo PE-02 + 10 por disputa no PE-03; ramp-up 60 s; 660 s no total (60 s de aquecimento + 600 s de medição); 150 disputas |
| Massa | Isolada, prefixo `CARGA-20261113-campanha-01`: 50 Solicitantes, 190 recursos e 90 professores criados pela API só para esta execução |
| Evidências | [resultados/20261113-campanha-01](../../../testes/carga/resultados/20261113-campanha-01/) — `resultados.jtl`, `dashboard/index.html`, `RESULTADO.md`, `resumo.json`, `disputas.csv`, `ambiente.txt`, logs da API e do JMeter |

## Resultado

**Decisão: aprovado para RF-06/RNF-02 sob carga e para a meta de 3 s do RNF-01 nas operações de reserva, neste ambiente.** Fica registrada uma observação de desempenho (DEF-PERF-001).

### Uma única reserva aceita por disputa

| Medida | Valor |
|---|---|
| Disputas executadas / planejadas | 150 / 150 (50 por sala, 50 por professor, 50 por material) |
| Tentativas | 1.500 (10 por disputa, todas enviadas juntas: espalhamento mediano de 1 ms e máximo de 6 ms) |
| Aceitas (201, `APROVADA`) | **150, exatamente 1 por disputa** |
| Recusadas (409 com `conflitos`) | 1.350 = 9 por disputa |
| Outros códigos | 0 |
| Linhas no MySQL para as disputas | 150; em todas as disputas, a única linha persistida e ativa é a mesma atividade que recebeu 201 no JTL, com histórico de auditoria |
| Disputas com dupla reserva | **0** |

### Métricas (execução completa)

| Operação | Amostras | Req/s | Erro inesperado | P50 ms | P90 | P95 | P99 | Máx |
|---|---:|---:|---:|---:|---:|---:|---:|---:|
| **Total** | 26.481 | **40,13** | **0,0%** | 10 | 17 | **43** | **1.247** | 2.172 |
| PE-02 Criar reserva | 6.245 | 9,49 | 0,0% | 10 | 14 | 17 | 25 | 77 |
| PE-02 Consultar detalhe | 6.238 | 9,50 | 0,0% | 6 | 9 | 10 | 16 | 35 |
| PE-02 Alterar reserva | 6.231 | 9,50 | 0,0% | 13 | 18 | 20 | 30 | 57 |
| PE-02 Cancelar reserva | 6.217 | 9,49 | 0,0% | 10 | 13 | 15 | 22 | 50 |
| PE-03 Disputa SALA | 500 | 1,08 | 0,0% | 77 | 134 | 145 | 198 | 315 |
| PE-03 Disputa PROFESSOR | 500 | 1,08 | 0,0% | 835 | 1.545 | 1.606 | 1.756 | 2.172 |
| PE-03 Disputa MATERIAL | 500 | 1,08 | 0,0% | 839 | 1.547 | 1.624 | 1.705 | 1.916 |

Códigos HTTP: 200 = 18.736, 201 = 6.395, 409 = 1.350 (todos em disputas), 5xx = 0. A taxa de erro inesperado exclui os 409 das perdedoras, que são a resposta correta. Qualquer 5xx, timeout, 409 fora de disputa ou asserção violada contaria como erro. O recorte em regime (sem os primeiros 60 s) e os percentis do dashboard estão no `RESULTADO.md` e em `dashboard/index.html`. O dashboard usa outro estimador de percentil, por isso pode diferir em poucos milissegundos.

### Consistência do PE-02

- 6.245 reservas criadas (201) e 6.245 persistidas; nenhum 409 com massa exclusiva, ou seja, nenhum falso conflito.
- 6.217 canceladas no JTL e 6.217 canceladas no banco, todas com histórico de criação, alteração e cancelamento. As 28 restantes são iterações interrompidas pelo fim do tempo, entre a criação e o cancelamento, e permanecem `APROVADA` no banco descartável.

## Análise

1. **RF-06 / RNF-02 sob volume:** com 40 usuários mutando reservas em paralelo, as 10 tentativas simultâneas de cada disputa produziram exatamente uma aceitação, nos três tipos de alvo, inclusive com horários parcialmente sobrepostos (14:00, 14:10 e 14:20, de 60 min). Não houve deadlock nem 5xx no `api.log`. O travamento pessimista em ordem de id (`RecursoRepositorio.travar`) e depois do professor se mostrou suficiente nesse cenário.
2. **RNF-01 (3 s):** o P95 de todas as operações de reserva ficou abaixo de 3 s. O ciclo PE-02 ficou abaixo de 20 ms, e o pior P95 foi o da disputa por material, com 1,62 s.
3. **DEF-PERF-001 — 409 lento quando o conflito é de professor ou material:** nas disputas por professor e por material, o 201 leva cerca de 15 ms, mas os 409 têm mediana de cerca de 0,9 s e P95 de 1,6 s. Nas disputas por sala, o 409 leva 83 ms.
   - **Causa provável:** `ReservaService.alternativas` busca “outro local, mesmo horário” varrendo todos os espaços ativos e consultando conflitos de cada um, ainda dentro da transação que mantém as travas. Quando o choque é do professor ou do material, nenhum outro local resolve, e a varredura percorre os cerca de 190 espaços da massa. As 9 perdedoras são serializadas pela trava e somam esses tempos.
   - **Risco:** o tempo cresce com o número de salas e com a fila da disputa. Ainda cumpre o RNF-01 aqui, mas pode estourar com mais salas ou disputas maiores.
   - **Recomendação:** pular o passo 3 quando o conflito não for do espaço, ou calcular as alternativas fora da seção travada. Depois, repetir esta campanha como regressão.

## Limites

- Uma única máquina, com JMeter, API e MySQL competindo por 4 vCPU; os números não representam um servidor de produção.
- PE-01 (pesquisa de disponibilidade, meta de 2 s) não foi executado nesta campanha.
- Um único nó da API; a concorrência entre várias instâncias não foi avaliada.
- A massa sintética é isolada; o comportamento com o volume de dados de produção não foi medido.

## Reprodução

```bash
JMETER_HOME=/caminho/apache-jmeter-5.6.3 RUN_ID=<novo-id> testes/carga/executar-carga.sh
```

O workflow `.github/workflows/carga-jmeter.yml` repete um perfil reduzido em PRs e a campanha completa sob demanda, publicando o mesmo pacote de evidências como artefato.
