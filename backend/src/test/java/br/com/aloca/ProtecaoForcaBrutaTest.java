package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aloca.seguranca.ProtecaoForcaBruta;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/** Regras do bloqueio de login por excesso de senhas erradas, sem Spring e com relógio controlado. */
class ProtecaoForcaBrutaTest {

    /** Relógio que o teste adianta à vontade. */
    static class Relogio extends Clock {
        Instant agora = Instant.parse("2030-03-12T12:00:00Z");
        void avancar(Duration d) { agora = agora.plus(d); }
        @Override public ZoneId getZone() { return ZoneOffset.UTC; }
        @Override public Clock withZone(ZoneId zona) { return this; }
        @Override public Instant instant() { return agora; }
    }

    private static final String EMAIL = "ana.martins@aloca.edu.br";
    private Relogio relogio;
    private ProtecaoForcaBruta protecao;

    @BeforeEach
    void criar() {
        relogio = new Relogio();
        protecao = new ProtecaoForcaBruta(5, Duration.ofMinutes(15), Duration.ofMinutes(15), relogio);
    }

    private void errar(int vezes) {
        for (int i = 0; i < vezes; i++) protecao.registrarFalha(EMAIL);
    }

    @ParameterizedTest(name = "[{index}] {0} senha(s) errada(s) → bloqueado? {1}")
    @CsvSource({"1, false", "3, false", "4, false", "5, true", "6, true", "10, true"})
    void bloqueiaSomenteNaQuintaFalha(int falhas, boolean bloqueado) {
        errar(falhas);
        assertThat(protecao.tempoRestante(EMAIL).isPresent()).isEqualTo(bloqueado);
    }

    @Test
    void informaQueAQuintaFalhaBloqueou() {
        for (int i = 1; i <= 4; i++) assertThat(protecao.registrarFalha(EMAIL)).as("falha %d", i).isFalse();
        assertThat(protecao.registrarFalha(EMAIL)).isTrue();
        assertThat(protecao.tempoRestante(EMAIL)).contains(Duration.ofMinutes(15));
    }

    @ParameterizedTest(name = "[{index}] {0} min depois do bloqueio → ainda bloqueado? {1}")
    @CsvSource({"0, true", "1, true", "14, true", "15, false", "60, false"})
    void bloqueioDuraQuinzeMinutos(long minutos, boolean bloqueado) {
        errar(5);
        relogio.avancar(Duration.ofMinutes(minutos));
        assertThat(protecao.tempoRestante(EMAIL).isPresent()).isEqualTo(bloqueado);
    }

    @Test
    void depoisDoBloqueioAContagemRecomeca() {
        errar(5);
        relogio.avancar(Duration.ofMinutes(15));
        errar(4);
        assertThat(protecao.tempoRestante(EMAIL)).isEmpty();
        errar(1);
        assertThat(protecao.tempoRestante(EMAIL)).isPresent();
    }

    @Test
    void falhasForaDaJanelaDeQuinzeMinutosNaoSeSomam() {
        errar(4);
        relogio.avancar(Duration.ofMinutes(16));
        errar(4);
        assertThat(protecao.tempoRestante(EMAIL)).isEmpty();
    }

    @Test
    void loginCertoZeraAContagem() {
        errar(4);
        protecao.registrarSucesso(EMAIL);
        errar(4);
        assertThat(protecao.tempoRestante(EMAIL)).isEmpty();
    }

    @ParameterizedTest(name = "[{index}] \"{0}\" é o mesmo e-mail")
    @ValueSource(strings = {"ANA.MARTINS@ALOCA.EDU.BR", "  ana.martins@aloca.edu.br  ", "Ana.Martins@Aloca.edu.br"})
    void emailNaoDiferenciaMaiusculasNemEspacos(String variante) {
        errar(4);
        protecao.registrarFalha(variante);
        assertThat(protecao.tempoRestante(EMAIL)).isPresent();
    }

    @Test
    void bloqueioDeUmEmailNaoAfetaOutros() {
        errar(5);
        assertThat(protecao.tempoRestante("roberto.almeida@aloca.edu.br")).isEmpty();
    }

    @Test
    void limparLiberaTodos() {
        errar(5);
        protecao.limpar();
        assertThat(protecao.tempoRestante(EMAIL)).isEmpty();
    }

    @Test
    void configuracaoInvalidaEhRecusada() {
        assertThatThrownBy(() -> new ProtecaoForcaBruta(0, Duration.ofMinutes(15), Duration.ofMinutes(15), relogio))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
