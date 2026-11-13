# Carga de pesquisa, criação e concorrência de reservas (JMeter) — PE-01, PE-02 e PE-03

Roteiro executável dos casos PE-01, PE-02 e PE-03 do [plano de testes](../../src/docs/qualidade/PLANO-TESTES.md#carga-jmeter--pe-01-a-pe-03) (RF-04, RF-05, RF-06, RF-10 e RNF-01). Fecha o impedimento IMP-003 do [registro de defeitos](../../src/docs/defeitos.md): agora há plano, execução, JTL, dashboard, análise e decisão versionados em `develop`.

| Arquivo | Papel |
|---|---|
| `aloca-reservas-carga.jmx` | Plano JMeter 5.6.3: setUp (carrega a massa), PE-02 e PE-03 e, em seguida, PE-01 |
| `carga.properties` | Formato do JTL (CSV, sem corpo/cabeçalhos), variáveis de correlação e percentis 90/95/99 do dashboard |
| `preparar_massa.py` | Cria pela API a massa sintética **isolada** da execução |
| `analisar_resultados.py` | P50/P90/P95/P99, req/s, taxa de erro, códigos HTTP, prova por disputa (JTL + PostgreSQL) e decisão |
| `test_analisar_resultados.py` | Autoteste do analisador (aprova 1 aceita; reprova 0 ou 2 aceitas, 5xx e pesquisa acima de 2 s) |
| `executar-carga.sh` | Orquestra PostgreSQL descartável, API, massa, JMeter, dashboard e análise |
| `resultados/<RUN_ID>/` | Evidências preservadas de cada execução |

## Perfil de carga

| Grupo | Threads | O que cada thread faz | Expectativa |
|---|---:|---|---|
| **PE-02 Ciclo de reserva** | 40 | Login uma vez; por iteração: criar → consultar detalhe → alterar horário → cancelar. Pensa 0,5–1,5 s antes de cada requisição | 201, 200, 200 (novo horário), 200 (`CANCELADA`) |
| **PE-01 Pesquisa de disponibilidade** | 50 | Fase própria, depois de PE-02/PE-03. Login uma vez; por iteração, pesquisa um dia que tem reservas das disputas: recursos do dia, salas filtradas (tipo, capacidade e texto) e professores por competência. Pensa 0,5–1,5 s antes de cada pesquisa | 200 com a lista (`recursos` / `professores`) |
| **PE-03 Disputa** | 10 | Login uma vez; a cada 2 s entra na próxima disputa. O *Synchronizing Timer* só libera quando as 10 threads chegaram, então as 10 tentativas saem juntas | Exatamente **1×201** (`APROVADA`) e **9×409** com `conflitos` |

- **Total simultâneo:** 50 usuários (RNF-01). Ramp-up de 60 s; PE-03 começa depois do ramp-up, quando o PE-02 já está em regime, então as disputas acontecem **sob volume**.
- **Duração:** 660 s = 60 s de aquecimento + 600 s de medição (proposta do plano) para PE-02/PE-03 e, depois, mais 660 s para PE-01 (cerca de 23 min no total). O analisador reporta a execução completa e o recorte em regime, descartando os primeiros 60 s **de cada fase**.
- **PE-01 em fase separada:** assim os 50 usuários do RNF-01 ficam só pesquisando, sem somar aos 50 de PE-02/PE-03, e a pesquisa encontra a base já carregada pelas reservas da campanha (as vencedoras das disputas continuam `APROVADA`).
- **Disputas:** 150, alternando o alvo comum — **sala** (mesmo espaço), **professor** (salas próprias diferentes, mesmo professor) e **material** (salas próprias diferentes, mesmo material). As 10 tentativas não são idênticas: começam às 14:00, 14:10 ou 14:20 com 60 min de duração, cobrindo sobreposição parcial.

## Massa isolada

`preparar_massa.py` usa o administrador de demonstração somente para cadastrar, pela API, dados com o prefixo `CARGA-<RUN_ID>`:

- PE-02: por thread, 1 Solicitante, 1 sala, 1 professor e 1 material **exclusivos**; cada iteração usa uma data diferente. Assim nenhum 409 é esperado no PE-02 — qualquer conflito ali seria defeito.
- PE-03: por thread, 1 Solicitante e 1 sala própria; por disputa, **um alvo exclusivo** (sala, professor ou material) e uma data própria. Disputas não interferem entre si, e cada uma é correlacionada pela atividade `CARGA-<RUN_ID> PE03 D<n> T<thread>`.
- PE-01 não cria dados: reaproveita os 50 Solicitantes de PE-02 e PE-03 e só lê.
- Nenhuma reserva de demonstração é alvo. O banco é um contêiner `postgres:17-alpine` em `tmpfs`, criado e destruído pela execução.
- A senha dos usuários sintéticos é aleatória por execução, passada ao JMeter por arquivo temporário e nunca gravada nos artefatos (o script falha se encontrá-la). O JTL não grava corpo de requisição, cabeçalhos nem tokens.

## Como executar

Pré-requisitos: Java 21, Maven, Docker, Python 3 e [JMeter 5.6.3](https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz).

```bash
mvn -f backend/pom.xml -DskipTests package
```

```bash
JMETER_HOME=/caminho/apache-jmeter-5.6.3 testes/carga/executar-carga.sh
```

Variáveis: `RUN_ID`, `DURACAO` (660), `RAMPUP` (60), `PE02_THREADS` (40), `PE03_THREADS` (10), `PE03_DISPUTAS` (150), `PE01_THREADS` (50; `0` desliga a fase), `PE01_DURACAO` (igual a `DURACAO`), `PORTA_API` (8080), `PORTA_BANCO` (5434). A saída do script é **1** se alguma disputa tiver zero ou mais de uma reserva aceita/persistida.

Autoteste do analisador:

```bash
python3 -m unittest discover -s testes/carga -p 'test_*.py' -v
```

No GitHub Actions, `.github/workflows/carga-jmeter.yml` roda um perfil reduzido em PRs que tocam `backend/` ou `testes/carga/` (150 s, 20 + 10 threads, 30 disputas e depois 150 s de PE-01 com 20 threads) e a campanha completa por *workflow_dispatch*; JTL, dashboard e análise ficam como artefato por 90 dias.

## Evidências preservadas por execução

| Arquivo em `resultados/<RUN_ID>/` | Conteúdo |
|---|---|
| `resultados.jtl` | Todas as amostras (CSV), com `disputa`, `tipoDisputa`, `alvoId` e `atividade` |
| `dashboard/index.html` | Dashboard HTML do JMeter (percentis 90/95/99, throughput, códigos, tempo de resposta ao longo do tempo) |
| `RESULTADO.md` / `resumo.json` | P50/P90/P95/P99, req/s, taxa de erro por operação, critérios e decisão |
| `disputas.csv` | Por disputa: tentativas, 201, 409, outros, espalhamento do envio, linhas persistidas/ativas no PostgreSQL, vencedora no JTL e no banco |
| `ambiente.txt` | Commit, máquina, Java, JMeter, PostgreSQL e parâmetros (inclusive os de PE-01) |
| `jmeter.log`, `jmeter-console.txt`, `api.log`, `analise-console.txt` | Logs da execução |
| `massa/` | Manifesto e IDs da massa isolada (sem senha) |

## Critérios de aceite aplicados pelo analisador

1. Cada disputa: 1×201, (K−1)×409, nenhum outro código; no PostgreSQL, **uma** linha persistida e ativa, e é a mesma atividade que recebeu 201 no JTL.
2. Nenhuma dupla reserva; todas as disputas planejadas executadas com K tentativas.
3. Erro inesperado total < 1% e nenhum 5xx/falha de conexão. O 409 das perdedoras é resposta correta e não conta como erro (uma asserção exige o corpo com `conflitos`).
4. P95 ≤ **2000 ms** em cada pesquisa de PE-01 e ≤ 3000 ms nas demais operações de negócio, em regime (RNF-01). Login fica fora do critério, pois roda só uma vez por thread. Se PE-01 foi planejado (`PE01_THREADS` > 0) e não gerou amostras, a execução é reprovada.
5. PE-02: reservas criadas no JTL = persistidas no PostgreSQL; canceladas com histórico de auditoria (criação, alteração e cancelamento).

## Limites

JMeter, API e PostgreSQL compartilham a mesma máquina; os tempos refletem esse ambiente e não um servidor de produção. A pesquisa de professores (`/api/disponibilidade/professores`) entra com `feat/back-disponibilidade`; numa API sem ela, o script detecta e PE-01 pesquisa só recursos (registrado em `ambiente.txt`). O teste mede um único nó da API; implantação distribuída não é avaliada.
