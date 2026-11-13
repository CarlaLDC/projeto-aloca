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
    void buscaIgnoraAcentosEMaiusculas() throws Exception {
        get(token(ANA), "/api/disponibilidade?busca=SALA DE REUNIOES")
            .andExpect(jsonPath("$.recursos[*].nome", contains("Sala de reuniões")));
    }

    // ------------------------------------------------------------------ professores por competência (RF-03)

    @Test
    void pesquisaProfessoresPorCompetenciaSemAcento() throws Exception {
        String ana = token(ANA);
        get(ana, "/api/disponibilidade/professores?competencia=matematica")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.professores[*].nome", contains("Lucas Oliveira")))
            .andExpect(jsonPath("$.professores[0].competencia").value("Matemática"));
        get(ana, "/api/disponibilidade/professores?competencia=QUÍMICA")
            .andExpect(jsonPath("$.professores[*].nome", contains("Juliana Lima")));
        get(ana, "/api/disponibilidade/professores?competencia=astronomia")
            .andExpect(jsonPath("$.professores", hasSize(0)))
            .andExpect(jsonPath("$.resumo.total").value(greaterThanOrEqualTo(4)));
    }

    @Test
    void buscaDeProfessorPorNomeOuCompetencia() throws Exception {
        String ana = token(ANA);
        get(ana, "/api/disponibilidade/professores?busca=andre")
            .andExpect(jsonPath("$.professores[*].nome", contains("André Lima")));
        get(ana, "/api/disponibilidade/professores?busca=lima")
            .andExpect(jsonPath("$.professores[*].nome", containsInAnyOrder("André Lima", "Juliana Lima")));
        get(ana, "/api/disponibilidade/professores?busca=robotica")
            .andExpect(jsonPath("$.professores[*].nome", contains("Ricardo Alves")));
        // Os dois filtros juntos: "lima" no nome e competência em química.
        get(ana, "/api/disponibilidade/professores?busca=lima&competencia=quimica")
            .andExpect(jsonPath("$.professores[*].nome", contains("Juliana Lima")));
    }

    @Test
    void agendaDoProfessorMostraOcupacaoEHorarioLivre() throws Exception {
        String ana = token(ANA);
        var dia = diaLivre();
        codigoCriado(ana, reserva(dia, "07:00", "10:00", SALA_204, PROF_LUCAS));

        get(ana, "/api/disponibilidade/professores?data=" + dia + "&competencia=matematica")
            .andExpect(jsonPath("$.data").value(dia.toString()))
            .andExpect(jsonPath("$.professores[0].status").value("Disponível"))
            .andExpect(jsonPath("$.professores[0].ocupacoes", hasSize(1)))
            .andExpect(jsonPath("$.professores[0].ocupacoes[0].motivo", containsString("Sala 204")))
            .andExpect(jsonPath("$.professores[0].livreDe").value("10:00:00"))
            .andExpect(jsonPath("$.professores[0].livreAte").value("22:00:00"));
    }

    @Test
    void professorOcupadoODiaInteiroApareceComoOcupado() throws Exception {
        String ana = token(ANA);
        var dia = diaLivre();
        codigoCriado(ana, reserva(dia, "07:00", "22:00", SALA_108, PROF_ANDRE));

        get(ana, "/api/disponibilidade/professores?data=" + dia + "&busca=andre")
            .andExpect(jsonPath("$.professores[0].status").value("Ocupado"))
            .andExpect(jsonPath("$.professores[0].livreDe").doesNotExist())
            .andExpect(jsonPath("$.resumo.ocupados").value(greaterThanOrEqualTo(1)));
    }

    @Test
    void pesquisaDeProfessoresExigeLogin() throws Exception {
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/api/disponibilidade/professores"))
            .andExpect(status().isUnauthorized());
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
