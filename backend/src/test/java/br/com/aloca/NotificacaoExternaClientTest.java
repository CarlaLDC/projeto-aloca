package br.com.aloca;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.aloca.integracao.NotificacaoCriada;
import br.com.aloca.integracao.NotificacaoExternaClient;
import br.com.aloca.integracao.NotificacaoExternaClient.Envio;
import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;

/**
 * Teste do cliente da integração externa, sem Spring. O serviço de e-mail da instituição
 * é substituído pelo WireMock, que responde como o serviço real (sucesso, erro, demora ou resposta quebrada).
 *
 * <p>Casos do plano de testes: EX-01 (resposta válida), EX-02 (tempo limite), EX-03 (4xx/5xx sem expor
 * payload nem credencial) e EX-04 (resposta inválida e campos ausentes).
 */
@ExtendWith(OutputCaptureExtension.class)
class NotificacaoExternaClientTest {

    @RegisterExtension
    static WireMockExtension servico = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Envio ENVIO = new Envio("ana.martins@aloca.edu.br", "Ana Martins",
        "Aloca · reserva RES-0001", "Reserva RES-0001 confirmada.", "RES-0001");

    private NotificacaoExternaClient cliente(Duration timeout) {
        return new NotificacaoExternaClient(servico.baseUrl() + "/notificacoes", "token-teste", timeout);
    }

    @Test
    @DisplayName("EX-01: resposta válida - envia JSON com o token e considera aceita")
    void enviaANotificacaoComoJsonComOToken() {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(202)));

        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isTrue();

        servico.verify(1, postRequestedFor(urlEqualTo("/notificacoes"))
            .withHeader("Authorization", equalTo("Bearer token-teste"))
            .withHeader("Content-Type", containing("application/json"))
            .withRequestBody(equalToJson("""
                {"para":"ana.martins@aloca.edu.br","nome":"Ana Martins","assunto":"Aloca · reserva RES-0001",
                 "mensagem":"Reserva RES-0001 confirmada.","codigoReserva":"RES-0001"}""")));
    }

    @ParameterizedTest(name = "EX-03: resposta {0} não derruba o sistema")
    @ValueSource(ints = {400, 401, 404, 500, 503})
    void erroDoServicoExternoEhTratadoSemExcecao(int status) {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(status)));
        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isFalse();
    }

    @Test
    @DisplayName("EX-03: erro externo não grava no log o corpo da resposta, o token nem o e-mail")
    void erroExternoNaoExpoePayloadNemCredencialNoLog(CapturedOutput saida) {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(500)
            .withBody("{\"erro\":\"falha interna\",\"detalhe\":\"segredo-do-servico-externo\"}")));

        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isFalse();

        assertThat(saida.getAll())
            .contains("HTTP 500", "RES-0001")
            .doesNotContain("segredo-do-servico-externo", "token-teste", "ana.martins@aloca.edu.br");
    }

    @Test
    @DisplayName("EX-02: serviço lento estoura o tempo limite sem travar")
    void servicoLentoEstouraOTempoLimiteSemTravar() {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200).withFixedDelay(1500)));
        long inicio = System.nanoTime();

        assertThat(cliente(Duration.ofMillis(300)).enviar(ENVIO)).isFalse();

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1400));
    }

    @Test
    @DisplayName("EX-04: 2xx com corpo que não é JSON conta como aceito (o corpo não faz parte do contrato)")
    void respostaDeSucessoComCorpoInvalidoEhAceita() {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200)
            .withHeader("Content-Type", "application/json").withBody("{isto não é json")));

        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isTrue();
    }

    @ParameterizedTest(name = "EX-04: resposta quebrada ({0}) vira falha tratada")
    @EnumSource(value = Fault.class, names = {"EMPTY_RESPONSE", "CONNECTION_RESET_BY_PEER", "RANDOM_DATA_THEN_CLOSE"})
    void respostaQuebradaEhTratadaSemExcecao(Fault falha, CapturedOutput saida) {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withFault(falha)));

        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isFalse();
        assertThat(saida.getAll()).doesNotContain("token-teste", "ana.martins@aloca.edu.br");
    }

    @ParameterizedTest(name = "EX-04: destinatário ausente [{0}] não chama o serviço")
    @ValueSource(strings = {"", "   "})
    void notificacaoSemDestinatarioNaoEhEnviada(String para) {
        Envio semDestinatario = new Envio(para, "Ana Martins", "Aloca", "Mensagem", "RES-0001");

        assertThat(cliente(Duration.ofSeconds(2)).enviar(semDestinatario)).isFalse();
        assertThat(cliente(Duration.ofSeconds(2)).enviar(new Envio(null, null, "Aloca", "Mensagem", null))).isFalse();
        servico.verify(0, postRequestedFor(anyUrl()));
    }

    @Test
    void semUrlConfiguradaNaoChamaOServico() {
        NotificacaoExternaClient desligado = new NotificacaoExternaClient("", "", Duration.ofSeconds(1));

        assertThat(desligado.habilitado()).isFalse();
        assertThat(desligado.enviar(ENVIO)).isFalse();
        desligado.aoCriarNotificacao(new NotificacaoCriada("x@y.z", "X", "msg", null));
        servico.verify(0, postRequestedFor(anyUrl()));
    }
}
