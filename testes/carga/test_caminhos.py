"""Regressão dos caminhos CLI e symlinks apontados pelo Sonar no PR #62."""
from pathlib import Path
import os
import subprocess
import sys
import tempfile
import unittest
from caminhos import RESULTADOS, arquivo_execucao, entrada_jtl, pasta_execucao


class CaminhosTest(unittest.TestCase):
    def setUp(self):
        RESULTADOS.mkdir(exist_ok=True)
        self.temporario = tempfile.TemporaryDirectory(prefix='seguranca-', dir=RESULTADOS)
        self.addCleanup(self.temporario.cleanup)
        self.pasta = Path(self.temporario.name)

    def test_pasta_de_campanha_e_massa_validas(self):
        self.assertEqual(pasta_execucao(self.pasta), self.pasta)
        self.assertEqual(pasta_execucao(self.pasta / 'massa', 'massa'), self.pasta / 'massa')
        self.assertEqual(entrada_jtl(self.pasta / 'resultados.jtl'), self.pasta / 'resultados.jtl')

    def test_rejeita_pasta_externa_travessia_e_niveis_extras(self):
        for caminho in ['/tmp/fora', RESULTADOS / '..' / 'fora', RESULTADOS / '.',
                        RESULTADOS / 'campanha' / 'extra', RESULTADOS / '.oculta']:
            with self.subTest(caminho=caminho), self.assertRaises(ValueError):
                pasta_execucao(caminho)

    def test_rejeita_subpasta_e_nome_de_arquivo_arbitrarios(self):
        with self.assertRaises(ValueError):
            pasta_execucao(self.pasta / 'outra', 'massa')
        with self.assertRaises(ValueError):
            entrada_jtl(self.pasta / 'outro.jtl')
        with self.assertRaises(ValueError):
            arquivo_execucao(self.pasta, '../fora')

    def test_rejeita_diretorio_simbolico(self):
        with tempfile.TemporaryDirectory() as fora:
            link = self.pasta / 'massa'
            link.symlink_to(fora, target_is_directory=True)
            with self.assertRaises(ValueError):
                pasta_execucao(link, 'massa')

    def test_rejeita_links_nos_arquivos_de_entrada_e_saida(self):
        with tempfile.TemporaryDirectory() as fora:
            alvo = Path(fora) / 'segredo'
            alvo.write_text('intacto')
            for nome in ['resultados.jtl', 'resumo.json', 'disputas.csv', 'RESULTADO.md']:
                link = self.pasta / nome
                link.symlink_to(alvo)
                with self.subTest(nome=nome), self.assertRaises(ValueError):
                    arquivo_execucao(self.pasta, nome)
                self.assertEqual(alvo.read_text(), 'intacto')
                link.unlink()

    def executar(self, script, argumentos):
        env = {**os.environ, 'CARGA_SENHA': 'senha-teste-local'}
        return subprocess.run([sys.executable, str(RESULTADOS.parent / script), *argumentos],
                              env=env, capture_output=True, text=True, timeout=10)

    def test_preparacao_recusa_saida_externa_antes_de_acessar_api(self):
        proc = self.executar('preparar_massa.py', ['--run-id', 'teste', '--saida', '/tmp/fora',
                                                  '--base-url', 'http://127.0.0.1:1'])
        self.assertEqual(proc.returncode, 2)
        self.assertIn('resultados/<id>', proc.stderr)
        self.assertNotIn('Connection refused', proc.stderr)

    def test_preparacao_recusa_identificador_incompativel(self):
        proc = self.executar('preparar_massa.py', ['--run-id', '../fora', '--saida', str(self.pasta / 'massa')])
        self.assertEqual(proc.returncode, 2)
        self.assertIn('run-id', proc.stderr)

    def test_analisador_recusa_entrada_externa_antes_de_ler(self):
        proc = self.executar('analisar_resultados.py', ['--saida', str(self.pasta), '--massa',
                         str(self.pasta / 'massa'), '--jtl', '/tmp/resultados.jtl'])
        self.assertEqual(proc.returncode, 2)
        self.assertIn('resultados/<id>', proc.stderr)

    def test_analisador_nao_mistura_campanhas(self):
        proc = self.executar('analisar_resultados.py', ['--saida', str(self.pasta), '--massa',
                         str(RESULTADOS / 'outra' / 'massa'), '--jtl', str(self.pasta / 'resultados.jtl')])
        self.assertEqual(proc.returncode, 2)
        self.assertIn('mesma campanha', proc.stderr)


if __name__ == '__main__':
    unittest.main()
