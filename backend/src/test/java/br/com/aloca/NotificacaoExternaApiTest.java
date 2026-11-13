package br.com.aloca;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.http.Fault;
import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integração crítica de ponta a ponta: a reserva é criada pela API (PostgreSQL em container)
 * e a notificação chega ao serviço de e-mail externo, simulado pelo WireMock.
 * Casos EX-01 a EX-04 do plano de testes; o tempo limite do cliente aqui é de 1 s.
 */
class NotificacaoExternaApiTest extends ApiTesteBase {

    @RegisterExtension
    static WireMockExtension servicoEmail = WireMockExtension.newInstance().options(wireMockConfig().dynamicPort()).build();

    @DynamicPropertySource
    static void apontarParaOWireMock(DynamicPropertyRegistry propriedades) {
        propriedades.add("aloca.notificacao.url", () -> servicoEmail.baseUrl() + "/api/notificacoes");
        propriedades.add("aloca.notificacao.token", () -> "token-da-instituicao");
        propriedades.add("aloca.notificacao.timeout", () -> "PT1S");
    }

    @Test
    @DisplayName("EX-01: reserva criada notifica o solicitante pelo serviço externo")
    void reservaCriadaNotificaOSolicitantePeloServicoExterno() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(202)));

        String codigo = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", SALA_204, null));

        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes"))
            .withHeader("Authorization", equalTo("Bearer token-da-instituicao"))
            .withRequestBody(matchingJsonPath("$.para", equalTo("ana.martins@aloca.edu.br")))
            .withRequestBody(matchingJsonPath("$.codigoReserva", equalTo(codigo)))
            .withRequestBody(matchingJsonPath("$.mensagem", containing("confirmada"))));
    }

    @Test
    @DisplayName("EX-01: solicitação restrita avisa o responsável pelo serviço externo")
    void solicitacaoRestritaAvisaOResponsavel() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(202)));

        String codigo = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", LAB_02, null));

        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes"))
            .withRequestBody(matchingJsonPath("$.para", equalTo("roberto.almeida@aloca.edu.br")))
            .withRequestBody(matchingJsonPath("$.mensagem", containing(codigo + " aguarda sua aprovação"))));
    }

    @Test
    @DisplayName("EX-03: serviço externo com 503 não impede a reserva")
    void servicoExternoForaDoArNaoImpedeAReserva() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(503)));
        String ana = token(ANA);

        String codigo = codigoCriado(ana, reserva(diaLivre(), "11:00", "12:00", SALA_204, null));

        get(ana, "/api/reservas/" + codigo).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.itens[*].codigoReserva", hasItem(codigo)));
        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes")));
    }

    @Test
    @DisplayName("EX-02: serviço lento estoura o tempo limite e a reserva continua gravada e notificada no sistema")
    void servicoLentoNaoDesfazAReserva() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(202).withFixedDelay(3000)));
        String ana = token(ANA);
        long inicio = System.nanoTime();

        String codigo = codigoCriado(ana, reserva(diaLivre(), "13:00", "14:00", SALA_204, null));

        // Uma notificação (confirmação ao solicitante): espera no máximo o tempo limite de 1 s, não os 3 s do serviço.
        org.assertj.core.api.Assertions.assertThat(Duration.ofNanos(System.nanoTime() - inicio))
            .isLessThan(Duration.ofMillis(2800));
        get(ana, "/api/reservas/" + codigo).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.itens[*].codigoReserva", hasItem(codigo)));
    }

    @Test
    @DisplayName("EX-04: resposta quebrada do serviço externo não impede a reserva")
    void respostaQuebradaNaoImpedeAReserva() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withFault(Fault.RANDOM_DATA_THEN_CLOSE)));
        String ana = token(ANA);

        String codigo = codigoCriado(ana, reserva(diaLivre(), "15:00", "16:00", SALA_204, null));

        get(ana, "/api/reservas/" + codigo).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes")));
    }

    @Test
    @DisplayName("Conflito (409) não envia notificação externa")
    void reservaRecusadaPorConflitoNaoEnviaNotificacao() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(202)));
        String ana = token(ANA);
        var dia = diaLivre();
        codigoCriado(ana, reserva(dia, "14:00", "16:00", SALA_108, null));
        servicoEmail.resetRequests();

        criar(ana, reserva(dia, "15:00", "17:00", SALA_108, null)).andExpect(status().isConflict());

        servicoEmail.verify(0, postRequestedFor(anyUrl()));
    }
}
