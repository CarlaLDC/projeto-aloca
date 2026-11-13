"""Autoteste do analisador: garante que ele reprova dupla reserva e erros inesperados.

Executar: python3 -m unittest discover -s testes/carga -p 'test_*.py'
"""
import csv
import json
import os
import subprocess
import sys
import tempfile
import unittest

AQUI = os.path.dirname(os.path.abspath(__file__))
CAMPOS = ["timeStamp", "elapsed", "label", "responseCode", "responseMessage", "threadName", "dataType",
          "success", "failureMessage", "bytes", "sentBytes", "grpThreads", "allThreads", "URL", "Latency",
          "IdleTime", "Connect", "disputa", "tipoDisputa", "alvoId", "atividade"]


def amostra(ts, label, code, ok=True, disputa="null", tipo="null", atividade="x", elapsed=50):
    return {"timeStamp": ts, "elapsed": elapsed, "label": label, "responseCode": code, "responseMessage": "",
            "threadName": "t", "dataType": "text", "success": str(ok).lower(), "failureMessage": "",
            "bytes": 1, "sentBytes": 1, "grpThreads": 1, "allThreads": 1, "URL": "u", "Latency": 1,
            "IdleTime": 0, "Connect": 0, "disputa": disputa, "tipoDisputa": tipo, "alvoId": "1",
            "atividade": atividade}


class AnalisadorTest(unittest.TestCase):
    def executar(self, amostras, disputas=2, k=3, pe01=0):
        d = tempfile.mkdtemp()
        os.makedirs(os.path.join(d, "massa"))
        with open(os.path.join(d, "massa", "manifesto.json"), "w") as f:
            json.dump({"runId": "t", "prefixo": "CARGA-t", "pe03Threads": k, "pe03Disputas": disputas, "pe01Threads": pe01,
                       "usuariosCriados": 0, "recursosCriados": 0, "professoresCriados": 0}, f)
        jtl = os.path.join(d, "r.jtl")
        with open(jtl, "w", newline="") as f:
            w = csv.DictWriter(f, fieldnames=CAMPOS)
            w.writeheader()
            w.writerows(amostras)
        proc = subprocess.run([sys.executable, os.path.join(AQUI, "analisar_resultados.py"), "--jtl", jtl,
                               "--massa", os.path.join(d, "massa"), "--saida", d, "--aquecimento", "0"],
                              capture_output=True, text=True)
        return proc.returncode, json.load(open(os.path.join(d, "resumo.json")))

    def disputa(self, n, codigos, ts=1000):
        return [amostra(ts + i, "PE-03 Disputa SALA", c, ok=c in ("201", "409"), disputa=str(n), tipo="SALA",
                        atividade=f"CARGA-t PE03 D{n} T{i}") for i, c in enumerate(codigos)]

    def test_uma_aceita_por_disputa_aprova(self):
        rc, r = self.executar(self.disputa(0, ["201", "409", "409"]) + self.disputa(1, ["409", "201", "409"]))
        self.assertEqual(rc, 0)
        self.assertEqual(r["decisao"], "APROVADO")

    def test_duas_aceitas_reprova_com_saida_1(self):
        rc, r = self.executar(self.disputa(0, ["201", "201", "409"]) + self.disputa(1, ["409", "201", "409"]))
        self.assertEqual(rc, 1)
        self.assertEqual(r["decisao"], "REPROVADO")

    def test_nenhuma_aceita_reprova(self):
        rc, _ = self.executar(self.disputa(0, ["409", "409", "409"]) + self.disputa(1, ["409", "201", "409"]))
        self.assertEqual(rc, 1)

    def test_5xx_conta_como_erro_inesperado(self):
        amostras = self.disputa(0, ["201", "409", "409"]) + self.disputa(1, ["409", "201", "409"])
        amostras.append(amostra(2000, "PE-02 Criar reserva", "500", ok=False))
        rc, r = self.executar(amostras)
        self.assertEqual(rc, 0)  # sem violação de disputa, mas critério de erro reprova
        self.assertEqual(r["decisao"], "REPROVADO")
        self.assertEqual(r["por_operacao"]["PE-02 Criar reserva"]["erros_inesperados"], 1)

    def test_percentis_nearest_rank(self):
        amostras = self.disputa(0, ["201", "409", "409"]) + self.disputa(1, ["409", "201", "409"])
        amostras += [amostra(3000 + i, "PE-02 Criar reserva", "201", elapsed=i) for i in range(1, 101)]
        _, r = self.executar(amostras)
        op = r["por_operacao"]["PE-02 Criar reserva"]
        self.assertEqual((op["p50_ms"], op["p95_ms"], op["p99_ms"], op["max_ms"]), (50, 95, 99, 100))


    def base(self):
        return self.disputa(0, ["201", "409", "409"]) + self.disputa(1, ["409", "201", "409"])

    def criterio(self, r, trecho):
        return next(c for c in r["criterios"] if trecho in c["criterio"])

    def test_pesquisa_pe01_usa_meta_de_2s(self):
        lentas = [amostra(5000 + i, "PE-01 Pesquisa recursos do dia", "200", elapsed=2500) for i in range(20)]
        _, r = self.executar(self.base() + lentas, pe01=5)
        self.assertEqual(r["decisao"], "REPROVADO")
        self.assertFalse(self.criterio(r, "P95 'PE-01 Pesquisa recursos do dia' <= 2000 ms")["aprovado"])
        # 2,5 s seria aceito nas demais operações (meta de 3 s)
        outras = [amostra(5000 + i, "PE-02 Criar reserva", "201", elapsed=2500) for i in range(20)]
        _, r2 = self.executar(self.base() + outras)
        self.assertEqual(r2["decisao"], "APROVADO")

    def test_pesquisa_pe01_rapida_aprova_e_tem_regime_proprio(self):
        # Aquecimento lento no início da fase PE-01 fica fora do regime (contado do início da fase).
        aquecendo = [amostra(100_000 + i, "PE-01 Pesquisa professores", "200", elapsed=2900) for i in range(5)]
        regime = [amostra(200_000 + i, "PE-01 Pesquisa professores", "200", elapsed=40) for i in range(50)]
        rc, r = self.executar(self.base() + aquecendo + regime, pe01=5)
        self.assertEqual(rc, 0)
        self.assertTrue(self.criterio(r, "PE-01 executado")["aprovado"])
        self.assertEqual(r["grupos"]["PE-01 (pesquisa)"]["amostras"], 55)

    def test_pe01_planejado_e_ausente_reprova(self):
        _, r = self.executar(self.base(), pe01=5)
        self.assertEqual(r["decisao"], "REPROVADO")
        self.assertFalse(self.criterio(r, "PE-01 executado")["aprovado"])


if __name__ == "__main__":
    unittest.main()
