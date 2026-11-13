#!/usr/bin/env bash
# Executa PE-02 + PE-03 contra API + PostgreSQL descartável e preserva as evidências em
# testes/carga/resultados/<RUN_ID>/ (JTL, dashboard HTML, logs, massa, análise).
#
# Pré-requisitos: Java 21, Maven, Docker, Python 3 e JMeter 5.6.3 (JMETER_HOME ou jmeter no PATH).
# Variáveis opcionais: RUN_ID, DURACAO (s, padrão 660 = 60 aquecimento + 600 medição), RAMPUP (60),
#   PE02_THREADS (40), PE03_THREADS (10), PE03_DISPUTAS (150), PORTA_API (8080), PORTA_BANCO (5434).
set -euo pipefail

RAIZ="$(cd "$(dirname "$0")/../.." && pwd)"
CARGA="$RAIZ/testes/carga"
RUN_ID="${RUN_ID:-$(date -u +%Y%m%dT%H%M%SZ)}"
OUT="$CARGA/resultados/$RUN_ID"
DURACAO="${DURACAO:-660}"
RAMPUP="${RAMPUP:-60}"
PE02_THREADS="${PE02_THREADS:-40}"
PE03_THREADS="${PE03_THREADS:-10}"
PE03_DISPUTAS="${PE03_DISPUTAS:-150}"
PORTA_API="${PORTA_API:-8080}"
PORTA_BANCO="${PORTA_BANCO:-5434}"
BANCO_CONTAINER="aloca-carga-postgres-$RUN_ID"
JMETER="${JMETER_HOME:+$JMETER_HOME/bin/}jmeter"

[ -e "$OUT" ] && { echo "Diretório $OUT já existe; use outro RUN_ID." >&2; exit 2; }
mkdir -p "$OUT/massa"
# Senha sintética da massa: só em memória (variável), nunca gravada nos artefatos.
export CARGA_SENHA="carga-$(head -c 12 /dev/urandom | od -An -tx1 | tr -d ' \n')"

SENHA_PROPS="$(mktemp)"
printf 'senha=%s\n' "$CARGA_SENHA" > "$SENHA_PROPS"   # via -q: o jmeter.log não registra o valor

API_PID=""
limpar() {
  rm -f "$SENHA_PROPS"
  [ -n "$API_PID" ] && kill "$API_PID" 2>/dev/null && wait "$API_PID" 2>/dev/null || true
  docker rm -f "$BANCO_CONTAINER" >/dev/null 2>&1 || true
}
trap limpar EXIT

echo "== Ambiente"
{
  echo "run_id=$RUN_ID"
  echo "commit=$(git -C "$RAIZ" rev-parse HEAD)"
  echo "branch=$(git -C "$RAIZ" rev-parse --abbrev-ref HEAD)"
  echo "data_utc=$(date -u +%FT%TZ)"
  echo "so=$(uname -srm)"
  echo "cpus=$(nproc)"
  echo "memoria=$(free -h | awk '/Mem:/{print $2}')"
  echo "java=$(java -version 2>&1 | grep -m1 version)"
  echo "jmeter=$("$JMETER" -j /dev/null --version 2>/dev/null | grep -o '5\.[0-9.]*' | head -1)"
  echo "banco_imagem=postgres:17-alpine (tmpfs, descartável)"
  echo "api=backend/target/aloca-backend-*.jar, PostgreSQL, Hikari padrão (10 conexões)"
  echo "observacao=JMeter, API e PostgreSQL na mesma máquina"
  echo "parametros=duracao=${DURACAO}s rampup=${RAMPUP}s pe02.threads=$PE02_THREADS pe03.threads=$PE03_THREADS pe03.disputas=$PE03_DISPUTAS"
} | tee "$OUT/ambiente.txt"

echo "== PostgreSQL descartável"
docker run -d --name "$BANCO_CONTAINER" -p "$PORTA_BANCO:5432" --tmpfs /var/lib/postgresql/data \
  -e POSTGRES_DB=aloca -e POSTGRES_USER=aloca -e POSTGRES_PASSWORD=aloca -e TZ=America/Sao_Paulo \
  postgres:17-alpine >/dev/null
# Espera aceitar conexão TCP (o pg_isready pelo socket responde antes do reinício final da inicialização).
for _ in $(seq 1 90); do
  docker exec "$BANCO_CONTAINER" psql -h 127.0.0.1 -U aloca -d aloca -Atc "SELECT 1" >/dev/null 2>&1 && break
  sleep 2
done
echo "banco_versao=$(docker exec "$BANCO_CONTAINER" postgres --version)" >> "$OUT/ambiente.txt"

echo "== API"
JAR="$(ls "$RAIZ"/backend/target/aloca-backend-*.jar 2>/dev/null | head -1)"
if [ -z "$JAR" ]; then
  mvn -q -f "$RAIZ/backend/pom.xml" -DskipTests package
  JAR="$(ls "$RAIZ"/backend/target/aloca-backend-*.jar | head -1)"
fi
ALOCA_DB_URL="jdbc:postgresql://localhost:$PORTA_BANCO/aloca" ALOCA_JWT_SEGREDO="carga-$RUN_ID-segredo-local-com-32-caracteres" \
  java -Xms512m -Xmx1g -jar "$JAR" --server.port="$PORTA_API" \
  > "$OUT/api.log" 2>&1 &
API_PID=$!
# Pronta = o login da administradora de demonstração funciona. O servidor começa a responder antes
# de os dados de demonstração serem gravados; esperar só pela porta deixava a massa falhar com 401.
for _ in $(seq 1 90); do
  curl -s -X POST -H 'Content-Type: application/json' \
    -d "{\"email\":\"${ADMIN_EMAIL:-patricia.souza@aloca.edu.br}\",\"senha\":\"${ADMIN_SENHA:-aloca123}\"}" \
    "http://localhost:$PORTA_API/api/auth/login" | grep -q '"token"' && break
  sleep 2
done

echo "== Massa isolada"
python3 "$CARGA/preparar_massa.py" --run-id "$RUN_ID" --saida "$OUT/massa" \
  --base-url "http://localhost:$PORTA_API" --pe02-threads "$PE02_THREADS" \
  --pe03-threads "$PE03_THREADS" --pe03-disputas "$PE03_DISPUTAS"

echo "== JMeter"
"$JMETER" -n -t "$CARGA/aloca-reservas-carga.jmx" -q "$CARGA/carga.properties" -q "$SENHA_PROPS" \
  -Jhost=localhost -Jport="$PORTA_API" -Jmassa="$OUT/massa" \
  -Jduracao="$DURACAO" -Jrampup="$RAMPUP" -Jpe02.threads="$PE02_THREADS" \
  -Jpe03.threads="$PE03_THREADS" -Jpe03.disputas="$PE03_DISPUTAS" -Jpe03.atraso="$RAMPUP" \
  -l "$OUT/resultados.jtl" -j "$OUT/jmeter.log" -e -o "$OUT/dashboard" | tee "$OUT/jmeter-console.txt"

echo "== Análise"
set +e
python3 "$CARGA/analisar_resultados.py" --jtl "$OUT/resultados.jtl" --massa "$OUT/massa" --saida "$OUT" \
  --aquecimento "$RAMPUP" --banco-container "$BANCO_CONTAINER" | tee "$OUT/analise-console.txt"
STATUS=${PIPESTATUS[0]}
set -e
# Nenhum segredo deve sobrar nos artefatos preservados.
if grep -rl "$CARGA_SENHA" "$OUT" >/dev/null 2>&1; then echo "Senha encontrada nos artefatos!" >&2; exit 3; fi
echo "Evidências em $OUT"
exit "$STATUS"
