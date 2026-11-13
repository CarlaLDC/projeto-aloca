#!/usr/bin/env bash
# Executa PE-02 + PE-03 contra API + MySQL descartável e preserva as evidências em
# testes/carga/resultados/<RUN_ID>/ (JTL, dashboard HTML, logs, massa, análise).
#
# Pré-requisitos: Java 21, Maven, Docker, Python 3 e JMeter 5.6.3 (JMETER_HOME ou jmeter no PATH).
# Variáveis opcionais: RUN_ID, DURACAO (s, padrão 660 = 60 aquecimento + 600 medição), RAMPUP (60),
#   PE02_THREADS (40), PE03_THREADS (10), PE03_DISPUTAS (150), PORTA_API (8080), PORTA_MYSQL (3307).
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
PORTA_MYSQL="${PORTA_MYSQL:-3307}"
MYSQL_CONTAINER="aloca-carga-mysql-$RUN_ID"
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
  docker rm -f "$MYSQL_CONTAINER" >/dev/null 2>&1 || true
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
  echo "mysql_imagem=mysql:8.4 (tmpfs, descartável)"
  echo "api=backend/target/aloca-backend-0.1.0.jar, perfil mysql, Hikari padrão (10 conexões)"
  echo "observacao=JMeter, API e MySQL na mesma máquina"
  echo "parametros=duracao=${DURACAO}s rampup=${RAMPUP}s pe02.threads=$PE02_THREADS pe03.threads=$PE03_THREADS pe03.disputas=$PE03_DISPUTAS"
} | tee "$OUT/ambiente.txt"

echo "== MySQL descartável"
docker run -d --name "$MYSQL_CONTAINER" -p "$PORTA_MYSQL:3306" --tmpfs /var/lib/mysql \
  -e MYSQL_DATABASE=aloca -e MYSQL_USER=aloca -e MYSQL_PASSWORD=aloca -e MYSQL_ROOT_PASSWORD=aloca-root \
  mysql:8.4 --default-time-zone=-03:00 --character-set-server=utf8mb4 --collation-server=utf8mb4_0900_ai_ci >/dev/null
for _ in $(seq 1 90); do
  docker exec "$MYSQL_CONTAINER" mysql -ualoca -paloca -e "SELECT 1" aloca >/dev/null 2>&1 && break
  sleep 2
done
echo "mysql_versao=$(docker exec "$MYSQL_CONTAINER" mysql -V)" >> "$OUT/ambiente.txt"

echo "== API"
JAR="$RAIZ/backend/target/aloca-backend-0.1.0.jar"
[ -f "$JAR" ] || mvn -q -f "$RAIZ/backend/pom.xml" -DskipTests package
ALOCA_DB_URL="jdbc:mysql://localhost:$PORTA_MYSQL/aloca" ALOCA_JWT_SEGREDO="carga-$RUN_ID-segredo-local-com-32-caracteres" \
  java -Xms512m -Xmx1g -jar "$JAR" --spring.profiles.active=mysql --server.port="$PORTA_API" \
  > "$OUT/api.log" 2>&1 &
API_PID=$!
for _ in $(seq 1 90); do
  curl -s -o /dev/null -w '%{http_code}' -X POST -H 'Content-Type: application/json' -d '{}' \
    "http://localhost:$PORTA_API/api/auth/login" | grep -q 400 && break
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
  --aquecimento "$RAMPUP" --mysql-container "$MYSQL_CONTAINER" | tee "$OUT/analise-console.txt"
STATUS=${PIPESTATUS[0]}
set -e
# Nenhum segredo deve sobrar nos artefatos preservados.
if grep -rl "$CARGA_SENHA" "$OUT" >/dev/null 2>&1; then echo "Senha encontrada nos artefatos!" >&2; exit 3; fi
echo "Evidências em $OUT"
exit "$STATUS"
