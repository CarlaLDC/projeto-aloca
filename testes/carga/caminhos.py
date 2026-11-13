"""Limita os arquivos da campanha a testes/carga/resultados/<id>, sem links simbólicos."""
from pathlib import Path
import re

RESULTADOS = Path(__file__).resolve().parent / 'resultados'
IDENTIFICADOR = re.compile(r'[A-Za-z0-9][A-Za-z0-9_-]{0,63}')


def pasta_execucao(caminho, subpasta=None):
    """Aceita apenas a pasta de uma execução (ou sua massa), dentro da raiz fixa.

    Valida antes de criar arquivos ou acessar a API. Caminhos absolutos gerados pelo
    shell continuam aceitos; travessia, caminhos externos e symlinks são recusados.
    """
    informado = Path(caminho)
    if '..' in informado.parts:
        raise ValueError('Travessia de diretórios não é permitida.')
    absoluto = informado.absolute()
    raiz = RESULTADOS.absolute()
    try:
        partes = absoluto.relative_to(raiz).parts
    except ValueError as erro:
        raise ValueError('Use testes/carga/resultados/<id> para a campanha.') from erro
    esperado = 2 if subpasta else 1
    if len(partes) != esperado or not IDENTIFICADOR.fullmatch(partes[0]):
        raise ValueError('Identificador de campanha inválido.')
    if subpasta and partes[1] != subpasta:
        raise ValueError('Subpasta de campanha inválida.')
    # Reconstrução a partir da raiz fixa e identificador validado; não usar o caminho CLI no I/O.
    destino = raiz / partes[0]
    if subpasta:
        destino = destino / subpasta
    if raiz.is_symlink() or destino.resolve() != destino or not destino.resolve().is_relative_to(raiz):
        raise ValueError('Links simbólicos não são permitidos na campanha.')
    return destino


def arquivo_execucao(pasta, nome):
    """Somente nomes fixos utilizados pelo plano, nunca nomes fornecidos pelo usuário."""
    permitidos = {'resultados.jtl', 'manifesto.json', 'pe02.csv', 'pe03_usuarios.csv',
                  'pe03_disputas.csv', 'resumo.json', 'disputas.csv', 'RESULTADO.md'}
    if nome not in permitidos:
        raise ValueError('Arquivo de campanha inválido.')
    pasta = pasta_execucao(pasta, 'massa' if Path(pasta).name == 'massa' else None)
    arquivo = pasta / nome
    if arquivo.is_symlink() or arquivo.resolve() != arquivo:
        raise ValueError('Link simbólico de arquivo não é permitido.')
    return arquivo


def entrada_jtl(caminho):
    informado = Path(caminho)
    if informado.name != 'resultados.jtl':
        raise ValueError('O JTL deve se chamar resultados.jtl.')
    return arquivo_execucao(pasta_execucao(informado.parent), 'resultados.jtl')
