package br.com.aloca;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.assertj.core.api.Assertions.assertThat;

import br.com.aloca.integracao.NotificacaoCriada;
import br.com.aloca.integracao.NotificacaoExternaClient;
import br.com.aloca.integracao.NotificacaoExternaClient.Envio;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Teste do cliente da integração externa, sem Spring. O serviço de e-mail da instituição
 * é substituído pelo WireMock, que responde como o serviço real (sucesso, erro ou demora).
 */
class NotificacaoExternaClientTest {

    @RegisterExtension
    static WireMockExtension servico = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    private static final Envio ENVIO = new Envio("ana.martins@aloca.edu.br", "Ana Martins",
        "Aloca · reserva RES-0001", "Reserva RES-0001 confirmada.", "RES-0001");

    private NotificacaoExternaClient cliente(Duration timeout) {
        return new NotificacaoExternaClient(servico.baseUrl() + "/notificacoes", "token-teste", timeout);
    }

    @Test
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

    @ParameterizedTest(name = "resposta {0} não derruba o sistema")
    @ValueSource(ints = {400, 401, 404, 500, 503})
    void erroDoServicoExternoEhTratadoSemExcecao(int status) {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(status)));
        assertThat(cliente(Duration.ofSeconds(2)).enviar(ENVIO)).isFalse();
    }

    @Test
    void servicoLentoEstouraOTempoLimiteSemTravar() {
        servico.stubFor(post("/notificacoes").willReturn(aResponse().withStatus(200).withFixedDelay(1500)));
        long inicio = System.nanoTime();

        assertThat(cliente(Duration.ofMillis(300)).enviar(ENVIO)).isFalse();

        assertThat(Duration.ofNanos(System.nanoTime() - inicio)).isLessThan(Duration.ofMillis(1400));
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
