package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

/** Tela 08 · Aprovações (RF-07). */
class AprovacaoApiTest extends ApiTesteBase {

    @Test
    void somenteOResponsavelAprovaRecursoRestrito() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "11:00", LAB_02, null));

        // Solicitante e administrador não aprovam (RF-07 e persona do Administrador).
        postJson(ana, "/api/aprovacoes/" + codigo + "/aprovar", null).andExpect(status().isForbidden());
        postJson(token(PATRICIA), "/api/aprovacoes/" + codigo + "/aprovar", null).andExpect(status().isForbidden());

        get(roberto, "/api/aprovacoes")
            .andExpect(jsonPath("$.solicitacoes[?(@.codigo == '%s')].disponibilidadeValidada".formatted(codigo), contains(true)));
        postJson(roberto, "/api/aprovacoes/" + codigo + "/aprovar", null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.historico[*].ator", hasItem("Roberto Almeida")));

        postJson(roberto, "/api/aprovacoes/" + codigo + "/aprovar", null).andExpect(status().isConflict());
        get(roberto, "/api/aprovacoes").andExpect(jsonPath("$.aprovadasHoje").value(greaterThanOrEqualTo(1)));
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.itens[*].mensagem", hasItem(containsString(codigo + " foi aprovada"))));
    }

    @Test
    void rejeicaoExigeJustificativaEGuardaOMotivo() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "13:00", "15:00", AUDITORIO, null));
        postJson(roberto, "/api/aprovacoes/" + codigo + "/rejeitar", "{\"justificativa\":\"  \"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("Informe a justificativa."));
        postJson(roberto, "/api/aprovacoes/" + codigo + "/rejeitar", "{\"justificativa\":\"Evento externo no auditório\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("REJEITADA"))
            .andExpect(jsonPath("$.motivoRejeicao").value("Evento externo no auditório"));
        // Rejeitada não ocupa mais a agenda.
        criar(ana, reserva(java.time.LocalDate.parse(
            com.jayway.jsonpath.JsonPath.read(get(ana, "/api/reservas/" + codigo).andReturn().getResponse().getContentAsString(), "$.data")),
            "13:00", "15:00", AUDITORIO, null)).andExpect(status().isCreated());
    }
}
