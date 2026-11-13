#!/usr/bin/env python3
"""Consulta na base OSV.dev as dependências Maven resolvidas do back-end (RNF-07).

Entrada: a saída de `mvn dependency:list -DoutputFile=...` (todas as dependências, inclusive transitivas, já com a
versão que o Maven escolheu). Saída: relatório em markdown e JSON com cada dependência vulnerável, a severidade e o
escopo (compile/runtime vão para produção; test não).

Uso: python3 scripts/osv-maven.py --lista deps.txt --markdown saida.md --json saida.json
Variável OSV_API (opcional) troca o endereço da API, para testes.
"""
import argparse
import json
import os
import re
import sys
import urllib.request

API = os.environ.get("OSV_API", "https://api.osv.dev")
LINHA = re.compile(r"^\s*([\w.\-]+):([\w.\-]+):[\w\-]+(?::[\w.\-]+)?:([\w.\-+]+):(compile|runtime|test|provided|system)\b")


def ler_dependencias(caminho):
    deps = {}
    for linha in open(caminho, encoding="utf-8"):
        m = LINHA.match(linha)
        if m:
            grupo, artefato, versao, escopo = m.groups()
            deps[(f"{grupo}:{artefato}", versao)] = escopo
    return deps


def chamar(caminho, corpo=None):
    dados = json.dumps(corpo).encode() if corpo is not None else None
    req = urllib.request.Request(API + caminho, data=dados, method="POST" if dados else "GET",
                                 headers={"Content-Type": "application/json"})
    with urllib.request.urlopen(req, timeout=60) as resp:
        return json.load(resp)


def severidade(vuln):
    rotulo = (vuln.get("database_specific") or {}).get("severity")
    if rotulo:
        return str(rotulo).upper()
    return "SEM CLASSIFICAÇÃO"


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--lista", required=True)
    p.add_argument("--markdown", required=True)
    p.add_argument("--json", required=True)
    a = p.parse_args()

    deps = ler_dependencias(a.lista)
    if not deps:
        sys.exit("Nenhuma dependência encontrada na lista do Maven.")
    chaves = sorted(deps)
    achados = []
    for i in range(0, len(chaves), 500):
        lote = chaves[i:i + 500]
        resposta = chamar("/v1/querybatch", {"queries": [
            {"package": {"ecosystem": "Maven", "name": nome}, "version": versao} for nome, versao in lote]})
        for (nome, versao), resultado in zip(lote, resposta.get("results", [])):
            for v in resultado.get("vulns", []):
                detalhe = chamar("/v1/vulns/" + v["id"])
                achados.append({"pacote": nome, "versao": versao, "escopo": deps[(nome, versao)], "id": v["id"],
                                "aliases": detalhe.get("aliases", []), "severidade": severidade(detalhe),
                                "resumo": detalhe.get("summary", "")})

    producao = [x for x in achados if x["escopo"] in ("compile", "runtime")]
    json.dump({"dependencias": len(deps), "vulnerabilidades": achados}, open(a.json, "w", encoding="utf-8"),
              indent=2, ensure_ascii=False)
    with open(a.markdown, "w", encoding="utf-8") as f:
        f.write(f"### Back-end (Maven): {len(deps)} dependências resolvidas\n\n")
        if not achados:
            f.write("Nenhuma vulnerabilidade conhecida.\n")
        else:
            f.write(f"{len(achados)} aviso(s), {len(producao)} em dependências que vão para produção "
                    "(escopo compile/runtime).\n\n")
            f.write("| Pacote | Versão | Escopo | Aviso | Severidade | Resumo |\n|---|---|---|---|---|---|\n")
            for x in sorted(achados, key=lambda x: (x["escopo"] == "test", x["pacote"])):
                ids = ", ".join([x["id"]] + x["aliases"][:2])
                f.write(f"| {x['pacote']} | {x['versao']} | {x['escopo']} | {ids} | {x['severidade']} | "
                        f"{x['resumo'][:120]} |\n")
    print(f"{len(deps)} dependências; {len(achados)} aviso(s); {len(producao)} em produção")


if __name__ == "__main__":
    main()
