package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/** Tela 09 · Retiradas e devoluções (RF-09). */
class MaterialApiTest extends ApiTesteBase {

    @Test
    void retiradaEDevolucaoDeMaterialGeramProtocoloEHistorico() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "11:00", SALA_204, null, NOTEBOOK_08));

        String lista = get(roberto, "/api/movimentacoes?busca=" + codigo).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String mov = JsonPath.read(lista, "$.movimentacoes[0].codigo");

        postJson(roberto, "/api/movimentacoes/" + mov + "/devolucao", null).andExpect(status().isConflict());
        postJson(roberto, "/api/movimentacoes/" + mov + "/retirada", null)
            .andExpect(status().isOk()).andExpect(jsonPath("$.status").value("EM_USO"));
        postJson(roberto, "/api/movimentacoes/" + mov + "/devolucao", null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("DEVOLVIDO"))
            .andExpect(jsonPath("$.protocolo", startsWith("DEV-")));

        get(ana, "/api/reservas/" + codigo).andExpect(jsonPath("$.historico[*].acao", hasItems("Retirada", "Devolução")));
    }

    @Test
    void reservaComMaterialRetiradoNaoPodeSerCancelada() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "11:00", SALA_108, null, PROJETOR_04));
        String mov = JsonPath.read(get(roberto, "/api/movimentacoes?busca=" + codigo).andReturn().getResponse()
            .getContentAsString(), "$.movimentacoes[0].codigo");
        postJson(roberto, "/api/movimentacoes/" + mov + "/retirada", null).andExpect(status().isOk());
        postJson(ana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"x\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("em uso")));
    }

    @Test
    void cancelarReservaDescartaMovimentacaoNaoRetirada() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "11:00", SALA_204, null, NOTEBOOK_08));
        postJson(ana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"Remarcada\"}").andExpect(status().isOk());
        get(roberto, "/api/movimentacoes?busca=" + codigo).andExpect(jsonPath("$.movimentacoes", hasSize(0)));
    }

    @Test
    void materialDeReservaPendenteSoApareceDepoisDaAprovacao() throws Exception {
        String ana = token(ANA), roberto = token(ROBERTO);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "11:00", LAB_02, null, KIT_ROBOTICA));
        get(roberto, "/api/movimentacoes?busca=" + codigo).andExpect(jsonPath("$.movimentacoes", hasSize(0)));
        postJson(roberto, "/api/aprovacoes/" + codigo + "/aprovar", null).andExpect(status().isOk());
        get(roberto, "/api/movimentacoes?busca=" + codigo).andExpect(jsonPath("$.movimentacoes", hasSize(1)));
    }

    @Test
    void somenteOResponsavelRegistraMovimentacoes() throws Exception {
        get(token(ANA), "/api/movimentacoes").andExpect(status().isForbidden());
        get(token(PATRICIA), "/api/movimentacoes").andExpect(status().isForbidden());
    }
}
