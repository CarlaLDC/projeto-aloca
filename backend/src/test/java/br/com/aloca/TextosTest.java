package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.aloca.servico.Textos;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/** Busca sem diferenciar maiúsculas, acentos e espaços (usada na pesquisa por competência). */
class TextosTest {

    @ParameterizedTest(name = "[{index}] \"{0}\" → \"{1}\"")
    @CsvSource(delimiter = '|', textBlock = """
        Matemática                | matematica
        ROBÓTICA EDUCACIONAL      | robotica educacional
        '  Química   Orgânica  '  | quimica organica
        Sala de reuniões          | sala de reunioes
        Ação · Integração         | acao · integracao
        """)
    void normalizaParaComparar(String texto, String esperado) {
        assertThat(Textos.normalizar(texto)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "[{index}] \"{1}\" encontra \"{0}\"? {2}")
    @CsvSource(delimiter = '|', textBlock = """
        Matemática                  | matematica   | true
        Matemática                  | MATEMÁTICA   | true
        Matemática                  | temat        | true
        Desenvolvimento de sistemas | sistemas     | true
        Desenvolvimento de sistemas | de  sist     | true
        Robótica educacional        | robotica edu | true
        Química                     | fisica       | false
        Matemática                  | matematicas  | false
        """)
    void comparaSemAcentoNemMaiuscula(String texto, String termo, boolean esperado) {
        assertThat(Textos.contem(texto, termo)).isEqualTo(esperado);
    }

    @ParameterizedTest(name = "[{index}] termo vazio \"{0}\" sempre combina")
    @NullAndEmptySource
    @ValueSource(strings = {"   "})
    void termoVazioNaoFiltra(String termo) {
        assertThat(Textos.contem("Matemática", termo)).isTrue();
    }

    @ParameterizedTest(name = "[{index}] texto nulo só combina com termo vazio")
    @CsvSource(value = {"'', true", "mat, false"}, nullValues = "NULO")
    void textoNulo(String termo, boolean esperado) {
        assertThat(Textos.contem(null, termo)).isEqualTo(esperado);
    }
}
