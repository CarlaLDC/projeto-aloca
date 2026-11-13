#!/usr/bin/env python3
"""Cria, pela API, a massa sintética e isolada de uma execução de carga (PE-01/PE-02/PE-03).

Tudo o que é criado leva o prefixo ``CARGA-<run_id>``: usuários Solicitantes, salas,
professores e materiais exclusivos desta execução. Nenhum dado de demonstração é usado
como alvo de reserva; o administrador de demonstração só é usado para cadastrar a massa.

Arquivos gerados em ``<saida>`` (sem senha nem token):
  pe02.csv            um usuário + sala + professor + material exclusivos por thread
  pe03_usuarios.csv   um usuário + sala própria por thread da disputa
  pe03_disputas.csv   alvo exclusivo de cada disputa (sala, professor ou material)
  manifesto.json      parâmetros, datas-base e contagens

PE-01 (pesquisa) não cria dados: reaproveita os Solicitantes de PE-02 e PE-03.

Uso: CARGA_SENHA=... python3 preparar_massa.py --run-id X --saida DIR [--base-url URL]
"""
import argparse
import csv
import datetime as dt
import json
import os
import sys
import urllib.error
import urllib.request

TIPOS_DISPUTA = ("SALA", "PROFESSOR", "MATERIAL")


class Api:
    def __init__(self, base_url):
        self.base_url = base_url.rstrip("/")
        self.token = None

    def chamar(self, metodo, caminho, corpo=None, esperado=(200, 201)):
        dados = json.dumps(corpo).encode() if corpo is not None else None
        req = urllib.request.Request(self.base_url + caminho, data=dados, method=metodo)
        req.add_header("Content-Type", "application/json")
        if self.token:
            req.add_header("Authorization", "Bearer " + self.token)
        try:
            with urllib.request.urlopen(req, timeout=30) as resp:
                status, texto = resp.status, resp.read().decode()
        except urllib.error.HTTPError as erro:
            status, texto = erro.code, erro.read().decode()
        if status not in esperado:
            raise SystemExit(f"{metodo} {caminho} devolveu {status}: {texto[:300]}")
        return json.loads(texto) if texto else None


def main():
    p = argparse.ArgumentParser()
    p.add_argument("--run-id", required=True)
    p.add_argument("--saida", required=True)
    p.add_argument("--base-url", default="http://localhost:8080")
    p.add_argument("--pe02-threads", type=int, default=40)
    p.add_argument("--pe03-threads", type=int, default=10)
    p.add_argument("--pe03-disputas", type=int, default=150)
    p.add_argument("--pe01-threads", type=int, default=0)
    p.add_argument("--admin-email", default=os.environ.get("ADMIN_EMAIL", "patricia.souza@aloca.edu.br"))
    p.add_argument("--admin-senha", default=os.environ.get("ADMIN_SENHA", "aloca123"))
    a = p.parse_args()

    senha = os.environ.get("CARGA_SENHA")
    if not senha or len(senha) < 8:
        sys.exit("Defina CARGA_SENHA (mínimo 8 caracteres); ela não é gravada nos arquivos de massa.")

    prefixo = f"CARGA-{a.run_id}"
    api = Api(a.base_url)
    api.token = api.chamar("POST", "/api/auth/login", {"email": a.admin_email, "senha": a.admin_senha})["token"]

    def usuario(rotulo):
        email = f"{prefixo.lower()}.{rotulo}@carga.aloca.test"
        api.chamar("POST", "/api/admin/usuarios", {
            "nome": f"{prefixo} {rotulo}", "email": email, "unidade": "Carga",
            "perfis": ["SOLICITANTE"], "senha": senha}, esperado=(201,))
        return email

    def recurso(rotulo, tipo):
        return api.chamar("POST", "/api/admin/recursos", {
            "nome": f"{prefixo} {rotulo}", "tipo": tipo, "categoria": "Carga",
            "localizacao": f"Bloco carga {a.run_id}", "capacidade": 30 if tipo != "EQUIPAMENTO" else 1,
            "restrito": False, "abertura": "00:00", "fechamento": "23:59"}, esperado=(201,))["id"]

    def professor(rotulo):
        return api.chamar("POST", "/api/admin/professores", {
            "nome": f"{prefixo} {rotulo}", "competencia": "Carga"}, esperado=(201,))["id"]

    os.makedirs(a.saida, exist_ok=True)

    with open(os.path.join(a.saida, "pe02.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["idx", "email", "salaId", "professorId", "materialId"])
        for i in range(a.pe02_threads):
            w.writerow([i, usuario(f"pe02-u{i}"), recurso(f"PE02 sala {i}", "SALA"),
                        professor(f"PE02 prof {i}"), recurso(f"PE02 material {i}", "EQUIPAMENTO")])

    with open(os.path.join(a.saida, "pe03_usuarios.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["idx", "email", "salaPropriaId"])
        for i in range(a.pe03_threads):
            w.writerow([i, usuario(f"pe03-u{i}"), recurso(f"PE03 sala propria {i}", "SALA")])

    with open(os.path.join(a.saida, "pe03_disputas.csv"), "w", newline="") as f:
        w = csv.writer(f)
        w.writerow(["disputa", "tipo", "alvoId"])
        for d in range(a.pe03_disputas):
            tipo = TIPOS_DISPUTA[d % len(TIPOS_DISPUTA)]
            if tipo == "SALA":
                alvo = recurso(f"PE03 D{d} sala disputada", "SALA")
            elif tipo == "PROFESSOR":
                alvo = professor(f"PE03 D{d} professor disputado")
            else:
                alvo = recurso(f"PE03 D{d} material disputado", "EQUIPAMENTO")
            w.writerow([d, tipo, alvo])

    hoje = dt.date.today()
    manifesto = {
        "runId": a.run_id,
        "prefixo": prefixo,
        "criadoEm": dt.datetime.now(dt.timezone.utc).isoformat(timespec="seconds"),
        "pe02Threads": a.pe02_threads,
        "pe03Threads": a.pe03_threads,
        "pe03Disputas": a.pe03_disputas,
        "pe01Threads": a.pe01_threads,
        # Datas distintas por iteração/disputa evitam conflito acidental entre iterações.
        "pe02DataBase": (hoje + dt.timedelta(days=30)).isoformat(),
        "pe03DataBase": (hoje + dt.timedelta(days=30)).isoformat(),
        "usuariosCriados": a.pe02_threads + a.pe03_threads,
        "recursosCriados": 2 * a.pe02_threads + a.pe03_threads
                           + sum(1 for d in range(a.pe03_disputas) if TIPOS_DISPUTA[d % 3] != "PROFESSOR"),
        "professoresCriados": a.pe02_threads
                              + sum(1 for d in range(a.pe03_disputas) if TIPOS_DISPUTA[d % 3] == "PROFESSOR"),
    }
    with open(os.path.join(a.saida, "manifesto.json"), "w") as f:
        json.dump(manifesto, f, indent=2, ensure_ascii=False)
    print(json.dumps(manifesto, ensure_ascii=False))


if __name__ == "__main__":
    main()
