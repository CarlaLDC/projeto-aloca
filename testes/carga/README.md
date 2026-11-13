# Carga de criação e concorrência de reservas (JMeter) — PE-02 e PE-03

Roteiro executável dos casos PE-02 e PE-03 do [plano de testes](../../src/docs/qualidade/PLANO-TESTES.md#carga-jmeter--pe-01-a-pe-03) (RF-04, RF-05, RF-06, RF-10 e RNF-01). Fecha o impedimento IMP-003 do [registro de defeitos](../../src/docs/defeitos.md): agora há plano, execução, JTL, dashboard, análise e decisão versionados em `develop`.

| Arquivo | Papel |
|---|---|
| `aloca-reservas-carga.jmx` | Plano JMeter 5.6.3: setUp (carrega a massa), PE-02 e PE-03 |
| `carga.properties` | Formato do JTL (CSV, sem corpo/cabeçalhos), variáveis de correlação e percentis 90/95/99 do dashboard |
| `preparar_massa.py` | Cria pela API a massa sintética **isolada** da execução |
| `analisar_resultados.py` | P50/P90/P95/P99, req/s, taxa de erro, códigos HTTP, prova por disputa (JTL + MySQL) e decisão |
| `test_analisar_resultados.py` | Autoteste do analisador (aprova 1 aceita; reprova 0 ou 2 aceitas e 5xx) |
| `executar-carga.sh` | Orquestra MySQL descartável, API, massa, JMeter, dashboard e análise |
| `resultados/<RUN_ID>/` | Evidências preservadas de cada execução |

## Perfil de carga

| Grupo | Threads | O que cada thread faz | Expectativa |
|---|---:|---|---|
| **PE-02 Ciclo de reserva** | 40 | Login uma vez; por iteração: criar → consultar detalhe → alterar horário → cancelar. Pensa 0,5–1,5 s antes de cada requisição | 201, 200, 200 (novo horário), 200 (`CANCELADA`) |
| **PE-03 Disputa** | 10 | Login uma vez; a cada 2 s entra na próxima disputa. O *Synchronizing Timer* só libera quando as 10 threads chegaram, então as 10 tentativas saem juntas | Exatamente **1×201** (`APROVADA`) e **9×409** com `conflitos` |

- **Total simultâneo:** 50 usuários (RNF-01). Ramp-up de 60 s; PE-03 começa depois do ramp-up, quando o PE-02 já está em regime, então as disputas acontecem **sob volume**.
- **Duração:** 660 s = 60 s de aquecimento + 600 s de medição (proposta do plano). O analisador reporta a execução completa e o recorte em regime (sem os primeiros 60 s).
- **Disputas:** 150, alternando o alvo comum — **sala** (mesmo espaço), **professor** (salas próprias diferentes, mesmo professor) e **material** (salas próprias diferentes, mesmo material). As 10 tentativas não são idênticas: começam às 14:00, 14:10 ou 14:20 com 60 min de duração, cobrindo sobreposição parcial.

## Massa isolada

`preparar_massa.py` usa o administrador de demonstração somente para cadastrar, pela API, dados com o prefixo `CARGA-<RUN_ID>`:

- PE-02: por thread, 1 Solicitante, 1 sala, 1 professor e 1 material **exclusivos**; cada iteração usa uma data diferente. Assim nenhum 409 é esperado no PE-02 — qualquer conflito ali seria defeito.
- PE-03: por thread, 1 Solicitante e 1 sala própria; por disputa, **um alvo exclusivo** (sala, professor ou material) e uma data própria. Disputas não interferem entre si, e cada uma é correlacionada pela atividade `CARGA-<RUN_ID> PE03 D<n> T<thread>`.
- Nenhuma reserva de demonstração é alvo. O MySQL é um contêiner `mysql:8.4` em `tmpfs`, criado e destruído pela execução.
- A senha dos usuários sintéticos é aleatória por execução, passada ao JMeter por arquivo temporário e nunca gravada nos artefatos (o script falha se encontrá-la). O JTL não grava corpo de requisição, cabeçalhos nem tokens.

## Como executar

Pré-requisitos: Java 21, Maven, Docker, Python 3 e [JMeter 5.6.3](https://archive.apache.org/dist/jmeter/binaries/apache-jmeter-5.6.3.tgz).

```bash
mvn -f backend/pom.xml -DskipTests package
```

```bash
JMETER_HOME=/caminho/apache-jmeter-5.6.3 testes/carga/executar-carga.sh
```

Variáveis: `RUN_ID`, `DURACAO` (660), `RAMPUP` (60), `PE02_THREADS` (40), `PE03_THREADS` (10), `PE03_DISPUTAS` (150), `PORTA_API` (8080), `PORTA_MYSQL` (3307). A saída do script é **1** se alguma disputa tiver zero ou mais de uma reserva aceita/persistida.

Autoteste do analisador:

```bash
python3 -m unittest discover -s testes/carga -p 'test_*.py' -v
```

No GitHub Actions, `.github/workflows/carga-jmeter.yml` roda um perfil reduzido em PRs que tocam `backend/` ou `testes/carga/` (150 s, 20 + 10 threads, 30 disputas) e a campanha completa por *workflow_dispatch*; JTL, dashboard e análise ficam como artefato por 90 dias.

## Evidências preservadas por execução

| Arquivo em `resultados/<RUN_ID>/` | Conteúdo |
|---|---|
| `resultados.jtl` | Todas as amostras (CSV), com `disputa`, `tipoDisputa`, `alvoId` e `atividade` |
| `dashboard/index.html` | Dashboard HTML do JMeter (percentis 90/95/99, throughput, códigos, tempo de resposta ao longo do tempo) |
| `RESULTADO.md` / `resumo.json` | P50/P90/P95/P99, req/s, taxa de erro por operação, critérios e decisão |
| `disputas.csv` | Por disputa: tentativas, 201, 409, outros, espalhamento do envio, linhas persistidas/ativas no MySQL, vencedora no JTL e no banco |
| `ambiente.txt` | Commit, máquina, Java, JMeter, MySQL e parâmetros |
| `jmeter.log`, `jmeter-console.txt`, `api.log`, `analise-console.txt` | Logs da execução |
| `massa/` | Manifesto e IDs da massa isolada (sem senha) |

## Critérios de aceite aplicados pelo analisador

1. Cada disputa: 1×201, (K−1)×409, nenhum outro código; no MySQL, **uma** linha persistida e ativa, e é a mesma atividade que recebeu 201 no JTL.
2. Nenhuma dupla reserva; todas as disputas planejadas executadas com K tentativas.
3. Erro inesperado total < 1% e nenhum 5xx/falha de conexão. O 409 das perdedoras é resposta correta e não conta como erro (uma asserção exige o corpo com `conflitos`).
4. P95 ≤ 3000 ms em cada operação de negócio em regime (RNF-01, “demais operações comuns”). Login fica fora do critério, pois roda só uma vez por thread.
5. PE-02: reservas criadas no JTL = persistidas no MySQL; canceladas com histórico de auditoria (criação, alteração e cancelamento).

## Limites

JMeter, API e MySQL compartilham a mesma máquina; os tempos refletem esse ambiente e não um servidor de produção. PE-01 (pesquisa de disponibilidade) e a meta de 2 s para pesquisa não são cobertos aqui. O teste mede um único nó da API; implantação distribuída não é avaliada.
