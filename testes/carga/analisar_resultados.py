#!/usr/bin/env python3
"""Analisa o JTL de uma execução de carga (PE-01, PE-02 e PE-03) e confirma, no banco, uma única
reserva por disputa.

Produz ``resumo.json`` e ``RESULTADO.md`` no diretório da execução com:
  * p50/p90/p95/p99, média e máximo por operação (total e após o aquecimento; a fase PE-01 tem o
    próprio aquecimento, contado a partir da primeira pesquisa);
  * requisições por segundo (por operação e global);
  * taxa de erro inesperado (409 das disputas é resposta esperada e é contado à parte);
  * distribuição de códigos HTTP;
  * por disputa: tentativas, 201, 409, outros, vencedora pelo JTL e linha persistida no PostgreSQL;
  * consistência do PE-02: criadas x canceladas x histórico de auditoria no banco.

Percentis usam o método nearest-rank; o dashboard do JMeter usa outro estimador e pode
diferir em poucos milissegundos.

Saída 1 se houver disputa com zero ou mais de uma reserva aceita/persistida.
"""
import argparse
import csv
import json
import math
import os
import re
import subprocess
import sys
from collections import Counter, defaultdict

ATIVOS = {"SOLICITADA", "APROVADA", "EM_USO"}
P95_LIMITE_MS = 3000          # RNF-01: demais operações comuns em até 3 s (P95)
P95_PESQUISA_MS = 2000        # RNF-01: pesquisa de disponibilidade em até 2 s (P95)
ERRO_LIMITE_PCT = 1.0         # erros inesperados tolerados no total
OPERACOES_LOGIN = ("PE-01 Login", "PE-02 Login", "PE-03 Login")
PREFIXO_PESQUISA = "PE-01 Pesquisa"


def limite_p95(rotulo):
    return P95_PESQUISA_MS if rotulo.startswith(PREFIXO_PESQUISA) else P95_LIMITE_MS


def percentil(valores, p):
    if not valores:
        return None
    ordenados = sorted(valores)
    k = max(1, math.ceil(p / 100 * len(ordenados)))
    return ordenados[k - 1]


def estatisticas(amostras):
    if not amostras:
        return None
    tempos = [a["elapsed"] for a in amostras]
    inicio = min(a["ts"] for a in amostras)
    fim = max(a["ts"] + a["elapsed"] for a in amostras)
    janela = max((fim - inicio) / 1000, 0.001)
    erros = sum(1 for a in amostras if not a["ok"])
    return {
        "amostras": len(amostras),
        "erros_inesperados": erros,
        "taxa_erro_pct": round(100 * erros / len(amostras), 3),
        "media_ms": round(sum(tempos) / len(tempos), 1),
        "p50_ms": percentil(tempos, 50),
        "p90_ms": percentil(tempos, 90),
        "p95_ms": percentil(tempos, 95),
        "p99_ms": percentil(tempos, 99),
        "max_ms": max(tempos),
        "janela_s": round(janela, 1),
        "req_por_s": round(len(amostras) / janela, 2),
        "codigos": dict(sorted(Counter(a["code"] for a in amostras).items())),
    }


def ler_jtl(caminho):
    amostras = []
    with open(caminho, newline="", encoding="utf-8") as f:
        for linha in csv.DictReader(f):
            rotulo = linha["label"]
            if not rotulo.startswith("PE-"):
                continue
            amostras.append({
                "ts": int(linha["timeStamp"]),
                "elapsed": int(linha["elapsed"]),
                "label": rotulo,
                "code": linha["responseCode"],
                "ok": linha["success"].lower() == "true",
                "falha": linha.get("failureMessage", ""),
                "thread": linha["threadName"],
                "disputa": linha.get("disputa", ""),
                "tipo": linha.get("tipoDisputa", ""),
                "atividade": linha.get("atividade", ""),
            })
    return amostras


NOME_CONTAINER = re.compile(r"[A-Za-z0-9][A-Za-z0-9_.-]{0,127}")
PREFIXO_MASSA = re.compile(r"CARGA-[A-Za-z0-9_.-]{1,64}")


def consultar_banco(container, prefixo):
    # Nome do contêiner e prefixo da massa só com caracteres seguros: nada de opções ("-...") nem SQL
    # vindo da linha de comando ou do manifesto.
    if not NOME_CONTAINER.fullmatch(container):
        raise SystemExit(f"Nome de contêiner inválido: {container!r}")
    if not PREFIXO_MASSA.fullmatch(prefixo):
        raise SystemExit(f"Prefixo de massa inválido: {prefixo!r}")
    sql = (
        "SELECT r.atividade, r.status, r.codigo, "
        "(SELECT COUNT(*) FROM historico_reserva h WHERE h.reserva_id = r.id) "
        f"FROM reserva r WHERE r.atividade LIKE '{prefixo} %';\n"
    )
    # psql sem cabeçalho (-A -t) e com TAB entre as colunas; o SQL vai pela entrada padrão (-i), não como argumento.
    cmd = ["docker", "exec", "-i", "--", container, "psql", "-h", "127.0.0.1", "-U", "aloca", "-d", "aloca",
           "-A", "-t", "-F", "\t", "-q"]
    saida = subprocess.run(cmd, input=sql, capture_output=True, text=True, check=True).stdout
    linhas = []
    for l in saida.splitlines():
        partes = l.split("\t")
        if len(partes) == 4:
            linhas.append({"atividade": partes[0], "status": partes[1], "codigo": partes[2],
                           "historico": int(partes[3])})
    return linhas


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--jtl", required=True)
    p.add_argument("--massa", required=True)
    p.add_argument("--saida", required=True)
    p.add_argument("--aquecimento", type=int, default=60, help="segundos descartados no recorte em regime")
    p.add_argument("--banco-container", default="", help="container PostgreSQL da execução (conferência no banco)")
    a = p.parse_args()

    manifesto = json.load(open(os.path.join(a.massa, "manifesto.json"), encoding="utf-8"))
    k_esperado = manifesto["pe03Threads"]
    amostras = ler_jtl(a.jtl)
    if not amostras:
        sys.exit("JTL sem amostras PE-*")

    # PE-01 é uma fase própria (começa depois de PE-02/PE-03): o aquecimento conta do início dela.
    def inicio_fase(prefixo):
        ts = [x["ts"] for x in amostras if x["label"].startswith(prefixo)]
        return min(ts) if ts else None

    outras = [x["ts"] for x in amostras if not x["label"].startswith("PE-01")]
    t0 = min(outras) if outras else inicio_fase("PE-01")
    t0_pe01 = inicio_fase("PE-01")
    regime = [x for x in amostras
              if x["ts"] >= (t0_pe01 if x["label"].startswith("PE-01") else t0) + a.aquecimento * 1000]
    pesquisas = [x for x in amostras if x["label"].startswith(PREFIXO_PESQUISA)]

    rotulos = sorted({x["label"] for x in amostras})
    por_operacao = {r: estatisticas([x for x in amostras if x["label"] == r]) for r in rotulos}
    por_operacao_regime = {r: estatisticas([x for x in regime if x["label"] == r]) for r in rotulos}
    grupos = {
        "TOTAL": estatisticas(amostras),
        "PE-01 (pesquisa)": estatisticas(pesquisas),
        "PE-02 (ciclo)": estatisticas([x for x in amostras if x["label"].startswith("PE-02")]),
        "PE-03 (disputas)": estatisticas([x for x in amostras if x["label"].startswith("PE-03 Disputa")]),
    }
    grupos_regime = {"TOTAL": estatisticas(regime),
                     "PE-01 (pesquisa)": estatisticas([x for x in regime if x["label"].startswith(PREFIXO_PESQUISA)])}

    # ---------------------------------------------------------------- disputas pelo JTL
    disputas = defaultdict(list)
    for x in amostras:
        if x["label"].startswith("PE-03 Disputa"):
            disputas[int(x["disputa"])].append(x)
    tabela = {}
    for d, tentativas in sorted(disputas.items()):
        c = Counter(t["code"] for t in tentativas)
        vencedoras = [t["atividade"] for t in tentativas if t["code"] == "201"]
        tabela[d] = {
            "tipo": tentativas[0]["tipo"], "tentativas": len(tentativas),
            "aceitas_201": c.get("201", 0), "recusadas_409": c.get("409", 0),
            "outros": len(tentativas) - c.get("201", 0) - c.get("409", 0),
            "vencedora_jtl": vencedoras[0] if len(vencedoras) == 1 else vencedoras,
            # disparo: diferença entre o primeiro e o último envio da disputa
            "espalhamento_envio_ms": max(t["ts"] for t in tentativas) - min(t["ts"] for t in tentativas),
        }

    # ---------------------------------------------------------------- banco
    banco = None
    if a.banco_container:
        linhas = consultar_banco(a.banco_container, manifesto["prefixo"])
        pe03 = defaultdict(list)
        pe02 = []
        for l in linhas:
            partes = l["atividade"].split()
            if "PE03" in partes:
                pe03[int(partes[partes.index("PE03") + 1][1:])].append(l)
            elif "PE02" in partes:
                pe02.append(l)
        for d, info in tabela.items():
            persistidas = pe03.get(d, [])
            info["persistidas_bd"] = len(persistidas)
            info["ativas_bd"] = sum(1 for l in persistidas if l["status"] in ATIVOS)
            info["vencedora_bd"] = persistidas[0]["atividade"] if len(persistidas) == 1 else [l["atividade"] for l in persistidas]
            info["historico_vencedora"] = persistidas[0]["historico"] if len(persistidas) == 1 else None
        criadas_jtl = sum(1 for x in amostras if x["label"] == "PE-02 Criar reserva" and x["code"] == "201")
        canceladas_jtl = sum(1 for x in amostras if x["label"] == "PE-02 Cancelar reserva" and x["ok"])
        banco = {
            "pe02_reservas_bd": len(pe02),
            "pe02_criadas_jtl": criadas_jtl,
            "pe02_canceladas_bd": sum(1 for l in pe02 if l["status"] == "CANCELADA"),
            "pe02_canceladas_jtl": canceladas_jtl,
            "pe02_nao_canceladas_bd": sum(1 for l in pe02 if l["status"] != "CANCELADA"),
            "pe02_canceladas_com_menos_de_3_eventos": sum(
                1 for l in pe02 if l["status"] == "CANCELADA" and l["historico"] < 3),
            "pe03_reservas_bd": sum(len(v) for v in pe03.values()),
        }

    def disputa_valida(info):
        ok_jtl = (info["aceitas_201"] == 1 and info["outros"] == 0
                  and info["recusadas_409"] == info["tentativas"] - 1)
        if banco is None:
            return ok_jtl
        return (ok_jtl and info["persistidas_bd"] == 1 and info["ativas_bd"] == 1
                and info["vencedora_bd"] == info["vencedora_jtl"])

    completas = [d for d, i in tabela.items() if i["tentativas"] == k_esperado]
    violacoes = [d for d, i in tabela.items() if not disputa_valida(i)]
    duplas = [d for d, i in tabela.items() if i["aceitas_201"] > 1 or (banco and i["ativas_bd"] > 1)]

    # ---------------------------------------------------------------- critérios
    criterios = []

    def criterio(nome, ok, obtido):
        criterios.append({"criterio": nome, "aprovado": bool(ok), "obtido": obtido})

    pe01_planejado = manifesto.get("pe01Threads", 0)
    if pe01_planejado:
        criterio(f"PE-01 executado com {pe01_planejado} usuários pesquisando", bool(pesquisas),
                 f"{len(pesquisas)} pesquisas")
    criterio("Uma única reserva aceita por disputa (JTL + PostgreSQL)", not violacoes and tabela,
             f"{len(tabela) - len(violacoes)}/{len(tabela)} disputas válidas; violações: {violacoes[:10]}")
    criterio("Nenhuma dupla reserva", not duplas, f"{len(duplas)} disputas com mais de uma aceita")
    criterio(f"Todas as disputas com {k_esperado} tentativas simultâneas", len(completas) == len(tabela),
             f"{len(completas)}/{len(tabela)}")
    criterio("Disputas executadas = disputas planejadas", len(tabela) == manifesto["pe03Disputas"],
             f"{len(tabela)}/{manifesto['pe03Disputas']}")
    criterio(f"Taxa de erro inesperado total < {ERRO_LIMITE_PCT}%",
             grupos["TOTAL"]["taxa_erro_pct"] < ERRO_LIMITE_PCT, f"{grupos['TOTAL']['taxa_erro_pct']}%")
    cinco = sum(1 for x in amostras if x["code"].startswith("5") or not x["code"].isdigit())
    criterio("Nenhuma resposta 5xx ou falha de conexão", cinco == 0, f"{cinco}")
    for r in rotulos:
        if r in OPERACOES_LOGIN:
            continue
        e = por_operacao_regime[r] or por_operacao[r]
        limite = limite_p95(r)
        criterio(f"P95 '{r}' <= {limite} ms (regime)", e["p95_ms"] <= limite, f"{e['p95_ms']} ms")
    if banco:
        criterio("PE-02: criadas no JTL = persistidas no PostgreSQL",
                 banco["pe02_criadas_jtl"] == banco["pe02_reservas_bd"],
                 f"{banco['pe02_criadas_jtl']} x {banco['pe02_reservas_bd']}")
        criterio("PE-02: canceladas com auditoria (>=3 eventos)",
                 banco["pe02_canceladas_com_menos_de_3_eventos"] == 0,
                 f"{banco['pe02_canceladas_com_menos_de_3_eventos']} sem histórico completo")
    decisao = "APROVADO" if all(c["aprovado"] for c in criterios) else "REPROVADO"

    resumo = {
        "runId": manifesto["runId"], "decisao": decisao, "criterios": criterios,
        "aquecimento_s": a.aquecimento, "grupos": grupos, "grupos_regime": grupos_regime,
        "por_operacao": por_operacao, "por_operacao_regime": por_operacao_regime,
        "banco": banco, "disputas": tabela,
    }
    os.makedirs(a.saida, exist_ok=True)
    with open(os.path.join(a.saida, "resumo.json"), "w", encoding="utf-8") as f:
        json.dump(resumo, f, indent=2, ensure_ascii=False)
    with open(os.path.join(a.saida, "disputas.csv"), "w", newline="", encoding="utf-8") as f:
        campos = ["disputa", "tipo", "tentativas", "aceitas_201", "recusadas_409", "outros",
                  "espalhamento_envio_ms", "persistidas_bd", "ativas_bd", "vencedora_jtl", "vencedora_bd",
                  "historico_vencedora"]
        w = csv.DictWriter(f, fieldnames=campos, extrasaction="ignore")
        w.writeheader()
        for d, info in tabela.items():
            w.writerow({"disputa": d, **info})
    with open(os.path.join(a.saida, "RESULTADO.md"), "w", encoding="utf-8") as f:
        f.write(markdown(resumo, manifesto, len(violacoes)))
    print(f"Decisão: {decisao}")
    for c in criterios:
        print(("OK   " if c["aprovado"] else "FALHA") + f" {c['criterio']}: {c['obtido']}")
    return 1 if violacoes or duplas else 0


def linha_stats(nome, e):
    return (f"| {nome} | {e['amostras']} | {e['req_por_s']} | {e['taxa_erro_pct']}% | {e['media_ms']} | "
            f"{e['p50_ms']} | {e['p90_ms']} | {e['p95_ms']} | {e['p99_ms']} | {e['max_ms']} | "
            + ", ".join(f"{k}: {v}" for k, v in e["codigos"].items()) + " |\n")


def markdown(r, manifesto, n_violacoes):
    cab = ("| Operação | Amostras | Req/s | Erro inesperado | Média ms | P50 | P90 | P95 | P99 | Máx | Códigos |\n"
           "|---|---:|---:|---:|---:|---:|---:|---:|---:|---:|---|\n")
    out = [f"# Resultado da carga {r['runId']}\n\n",
           f"**Decisão automática: {r['decisao']}** (critérios abaixo; gerado por `analisar_resultados.py`).\n\n",
           f"Massa isolada `{manifesto['prefixo']}`: {manifesto['usuariosCriados']} usuários, "
           f"{manifesto['recursosCriados']} recursos e {manifesto['professoresCriados']} professores criados só para esta execução.\n\n",
           f"Metas (RNF-01): P95 da pesquisa de disponibilidade (PE-01) <= {P95_PESQUISA_MS} ms; "
           f"demais operações (PE-02, PE-03) <= {P95_LIMITE_MS} ms; logins fora do critério.\n\n",
           "## Critérios\n\n| Critério | Resultado | Obtido |\n|---|---|---|\n"]
    for c in r["criterios"]:
        out.append(f"| {c['criterio']} | {'✅' if c['aprovado'] else '❌'} | {c['obtido']} |\n")
    out.append("\n## Totais da execução\n\n" + cab)
    for nome, e in r["grupos"].items():
        if e:
            out.append(linha_stats(nome, e))
    out.append(f"\n## Por operação — execução completa\n\n" + cab)
    for nome, e in r["por_operacao"].items():
        out.append(linha_stats(nome, e))
    out.append(f"\n## Por operação — regime (descartados os primeiros {r['aquecimento_s']} s de cada fase)\n\n" + cab)
    for nome, e in r["por_operacao_regime"].items():
        if e:
            out.append(linha_stats(nome, e))
    out.append("\n“Erro inesperado” exclui o 409 das perdedoras de cada disputa, que é a resposta correta; "
               "qualquer 5xx, timeout, 409 fora de disputa ou asserção violada conta como erro.\n")
    p1 = r["grupos"].get("PE-01 (pesquisa)")
    if p1:
        p1r = r["grupos_regime"].get("PE-01 (pesquisa)") or p1
        out.append("\n## PE-01: pesquisa de disponibilidade\n\n")
        out.append(f"- {p1['amostras']} pesquisas ({p1['req_por_s']} req/s) depois de PE-02/PE-03, com a base já carregada "
                   "pelas reservas da campanha; cada usuário pesquisa um dia com reservas das disputas.\n")
        out.append(f"- Em regime: P95 {p1r['p95_ms']} ms, P99 {p1r['p99_ms']} ms, máximo {p1r['max_ms']} ms; "
                   f"erro inesperado {p1['taxa_erro_pct']}% (meta: P95 <= {P95_PESQUISA_MS} ms).\n")
    d = r["disputas"]
    tipos = Counter(i["tipo"] for i in d.values())
    out.append("\n## Comprovação: uma única reserva aceita por disputa\n\n")
    out.append(f"- Disputas: {len(d)} ({', '.join(f'{t}: {n}' for t, n in sorted(tipos.items()))}).\n")
    out.append(f"- Tentativas: {sum(i['tentativas'] for i in d.values())}; aceitas (201): "
               f"{sum(i['aceitas_201'] for i in d.values())}; recusadas (409): "
               f"{sum(i['recusadas_409'] for i in d.values())}; outros: {sum(i['outros'] for i in d.values())}.\n")
    if r["banco"]:
        out.append(f"- PostgreSQL: {r['banco']['pe03_reservas_bd']} reservas persistidas para {len(d)} disputas; "
                   f"em cada uma, a única linha persistida é a mesma atividade que recebeu 201 no JTL.\n")
    esp = sorted(i["espalhamento_envio_ms"] for i in d.values())
    if esp:
        out.append(f"- Espalhamento entre o primeiro e o último envio de cada disputa: mediana {esp[len(esp)//2]} ms, máximo {esp[-1]} ms "
                   "(Synchronizing Timer libera todas as threads juntas).\n")
    out.append(f"- Disputas violando a regra: **{n_violacoes}**. Detalhe por disputa em `disputas.csv`.\n")
    if r["banco"]:
        b = r["banco"]
        out.append("\n## Consistência do PE-02 no banco\n\n")
        out.append(f"- Criadas (201 no JTL): {b['pe02_criadas_jtl']}; persistidas: {b['pe02_reservas_bd']}.\n")
        out.append(f"- Canceladas (JTL): {b['pe02_canceladas_jtl']}; canceladas no banco: {b['pe02_canceladas_bd']}; "
                   f"não canceladas: {b['pe02_nao_canceladas_bd']} (iterações interrompidas pelo fim do tempo).\n")
        out.append(f"- Canceladas sem histórico completo (criação, alteração, cancelamento): "
                   f"{b['pe02_canceladas_com_menos_de_3_eventos']}.\n")
    return "".join(out)


if __name__ == "__main__":
    sys.exit(main())
