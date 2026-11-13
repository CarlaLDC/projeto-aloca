package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/** "Não compareceu" (TDD) junto com o controle de materiais (RF-09). */
@Import(RelogioDeTeste.Config.class)
@DisplayName("Não compareceu x materiais (TDD)")
class NaoComparecimentoMateriaisTest extends ApiTesteBase {

    private static final String NAO_COMPARECEU = "/api/aprovacoes/%s/nao-compareceu";

    @Autowired RelogioDeTeste relogio;

    @AfterEach
    void voltarAoHorarioReal() {
        relogio.reiniciar();
    }

    private String movimentacaoDa(String codigo) throws Exception {
        String lista = get(token(ROBERTO), "/api/movimentacoes?busca=" + codigo).andReturn().getResponse().getContentAsString();
        return JsonPath.read(lista, "$.movimentacoes[0].codigo");
    }

    @Test
    @DisplayName("Quando um material já foi retirado, houve uso: o registro é recusado")
    void materialRetiradoImpedeORegistro() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null, NOTEBOOK_08));
        postJson(token(ROBERTO), "/api/movimentacoes/" + movimentacaoDa(codigo) + "/retirada", null).andExpect(status().isOk());
        relogio.ajustar(dia.atTime(9, 30));

        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("em uso")));
    }

    @Test
    @DisplayName("Dado o registro, a retirada pendente do material é cancelada")
    void retiradaPendenteEhCancelada() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_108, null, PROJETOR_04));
        get(token(ROBERTO), "/api/movimentacoes?busca=" + codigo).andExpect(jsonPath("$.movimentacoes", hasSize(1)));
        relogio.ajustar(dia.atTime(9, 30));

        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null).andExpect(status().isOk());

        get(token(ROBERTO), "/api/movimentacoes?busca=" + codigo).andExpect(jsonPath("$.movimentacoes", hasSize(0)));
    }
}
