package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;

/** Telas 02 · Visão geral e 03 · Disponibilidade (RF-03, RF-14). */
class DisponibilidadeApiTest extends ApiTesteBase {

    @Test
    void disponibilidadeFiltraPorTipoBuscaECapacidade() throws Exception {
        String ana = token(ANA);
        get(ana, "/api/disponibilidade?data=" + diaLivre() + "&tipo=LABORATORIO")
            .andExpect(jsonPath("$.recursos[*].tipo", everyItem(is("LABORATORIO"))))
            .andExpect(jsonPath("$.resumo.total").value(greaterThanOrEqualTo(13)));
        get(ana, "/api/disponibilidade?busca=bloco b")
            .andExpect(jsonPath("$.recursos[*].nome", hasItems("Sala 108", "Sala de reuniões")));
        get(ana, "/api/disponibilidade?capacidadeMinima=50")
            .andExpect(jsonPath("$.recursos[*].nome", contains("Auditório")));
    }

    @Test
    void disponibilidadeMostraOcupacaoEJanelaLivre() throws Exception {
        String ana = token(ANA);
        var dia = diaLivre();
        codigoCriado(ana, reserva(dia, "07:00", "12:00", LAB_03, null));
        get(ana, "/api/disponibilidade?data=" + dia + "&busca=informática 03")
            .andExpect(jsonPath("$.recursos[0].status").value("Disponível"))
            .andExpect(jsonPath("$.recursos[0].livreDe").value("12:00:00"))
            .andExpect(jsonPath("$.recursos[0].ocupacoes", hasSize(1)));
    }

    @Test
    void recursoEmManutencaoODiaInteiroApareceComoIndisponivel() throws Exception {
        String patricia = token(PATRICIA);
        var dia = diaLivre();
        postJson(patricia, "/api/admin/bloqueios", """
            {"recursoId":%d,"inicio":"%sT00:00:00","fim":"%sT00:00:00","motivo":"Reforma"}""".formatted(SALA_204, dia, dia.plusDays(1)))
            .andExpect(status().isCreated());
        get(token(ANA), "/api/disponibilidade?data=" + dia + "&busca=Sala 204")
            .andExpect(jsonPath("$.recursos[0].status").value("Em manutenção"))
            .andExpect(jsonPath("$.recursos[0].livreDe").doesNotExist());
    }

    @Test
    void painelMostraContadoresEAgenda() throws Exception {
        get(token(ANA), "/api/painel")
            .andExpect(jsonPath("$.perfil").value("Solicitante"))
            .andExpect(jsonPath("$.reservasHoje").isNumber())
            .andExpect(jsonPath("$.recursosDisponiveis").isNumber())
            .andExpect(jsonPath("$.agenda").isArray());
        get(token(ANDRE), "/api/painel").andExpect(jsonPath("$.perfil").value("Professor"));
    }
}
