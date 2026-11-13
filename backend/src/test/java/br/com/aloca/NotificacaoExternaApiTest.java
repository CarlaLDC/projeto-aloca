package br.com.aloca;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.github.tomakehurst.wiremock.junit5.WireMockExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Integração crítica de ponta a ponta: a reserva é criada pela API (PostgreSQL em container)
 * e a notificação chega ao serviço de e-mail externo, simulado pelo WireMock.
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
    void solicitacaoRestritaAvisaOResponsavel() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(202)));

        String codigo = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", LAB_02, null));

        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes"))
            .withRequestBody(matchingJsonPath("$.para", equalTo("roberto.almeida@aloca.edu.br")))
            .withRequestBody(matchingJsonPath("$.mensagem", containing(codigo + " aguarda sua aprovação"))));
    }

    @Test
    void servicoExternoForaDoArNaoImpedeAReserva() throws Exception {
        servicoEmail.stubFor(post("/api/notificacoes").willReturn(aResponse().withStatus(503)));
        String ana = token(ANA);

        String codigo = codigoCriado(ana, reserva(diaLivre(), "11:00", "12:00", SALA_204, null));

        get(ana, "/api/reservas/" + codigo).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("APROVADA"));
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.itens[*].codigoReserva", hasItem(codigo)));
        servicoEmail.verify(postRequestedFor(urlEqualTo("/api/notificacoes")));
    }

    @Test
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
