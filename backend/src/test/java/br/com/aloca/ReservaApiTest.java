package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Telas 04 a 07: criar, conflitos, alterar, cancelar e histórico (RF-04, RF-05, RF-08, RF-10, RF-11). */
class ReservaApiTest extends ApiTesteBase {

    @Test
    void opcoesListamSalasProfessoresEMateriais() throws Exception {
        get(token(ANA), "/api/reservas/opcoes")
            .andExpect(jsonPath("$.espacos[*].tipo", everyItem(anyOf(is("SALA"), is("LABORATORIO")))))
            .andExpect(jsonPath("$.materiais[*].tipo", everyItem(is("EQUIPAMENTO"))))
            .andExpect(jsonPath("$.professores", hasSize(greaterThanOrEqualTo(4))));
    }

    @Test
    void reservaDeRecursoSemRestricaoEhAprovadaAutomaticamente() throws Exception {
        criar(token(ANA), reserva(diaLivre(), "09:00", "11:00", SALA_204, PROF_LUCAS, NOTEBOOK_08))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.codigo", startsWith("RES-")))
            .andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.solicitante").value("Ana Martins"))
            .andExpect(jsonPath("$.historico[0].acao").value("Solicitada"))
            .andExpect(jsonPath("$.historico[1].estadoAnterior").value("Solicitada"))
            .andExpect(jsonPath("$.historico[1].estadoNovo").value("Aprovada"));
    }

    @Test
    void recursoRestritoFicaAguardandoAprovacao() throws Exception {
        criar(token(ANA), reserva(diaLivre(), "09:00", "11:00", LAB_02, null))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.status").value("SOLICITADA"))
            .andExpect(jsonPath("$.exigeAprovacao").value(true));
    }

    @Test
    void horarioSobrepostoNaMesmaSalaEhRecusadoComAlternativas() throws Exception {
        String ana = token(ANA), mariana = token(MARIANA);
        LocalDate dia = diaLivre();
        codigoCriado(ana, reserva(dia, "09:00", "11:00", SALA_108, null));

        criar(mariana, reserva(dia, "10:00", "12:00", SALA_108, null))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("Sala 108")))
            .andExpect(jsonPath("$.conflitos[0].tipo").value("Sala"))
            .andExpect(jsonPath("$.conflitos[0].sobreposicaoMinutos").value(60))
            .andExpect(jsonPath("$.alternativas", hasSize(greaterThan(0))))
            .andExpect(jsonPath("$.alternativas", hasSize(lessThanOrEqualTo(3))));
    }

    @Test
    void reservasConsecutivasSaoPermitidas() throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        codigoCriado(ana, reserva(dia, "09:00", "11:00", SALA_204, null));
        criar(ana, reserva(dia, "11:00", "13:00", SALA_204, null)).andExpect(status().isCreated());
        criar(ana, reserva(dia, "07:00", "09:00", SALA_204, null)).andExpect(status().isCreated());
    }

    @Test
    void materialEProfessorTambemNaoPodemTerSobreposicao() throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        codigoCriado(ana, reserva(dia, "14:00", "16:00", SALA_204, PROF_ANDRE, PROJETOR_04));

        criar(ana, reserva(dia, "15:00", "17:00", SALA_108, null, PROJETOR_04))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.conflitos[0].tipo").value("Material"));
        criar(ana, reserva(dia, "15:00", "17:00", LAB_03, PROF_ANDRE))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.conflitos[0].tipo").value("Professor"));
    }

    @Test
    void horarioInvalidoEDadosObrigatoriosSaoRecusados() throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        criar(ana, reserva(dia, "11:00", "09:00", SALA_204, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("O término deve ser posterior ao início."));
        criar(ana, reserva(dia, "10:00", "10:00", SALA_204, null)).andExpect(status().isBadRequest());
        criar(ana, reserva(LocalDate.now().minusDays(1), "10:00", "11:00", SALA_204, null))
            .andExpect(status().isBadRequest());
        criar(ana, reserva(dia, "05:00", "06:00", SALA_204, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem", containsString("funciona das")));
        criar(ana, "{\"atividade\":\"\"}")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.campos.atividade").exists())
            .andExpect(jsonPath("$.campos.espacoId").exists());
        criar(ana, reserva(dia, "10:00", "11:00", PROJETOR_04, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem", containsString("sala ou laboratório")));
    }

    @Test
    void recursoBloqueadoNaoPodeSerReservadoNoPeriodo() throws Exception {
        String ana = token(ANA), patricia = token(PATRICIA);
        LocalDate dia = diaLivre();
        postJson(patricia, "/api/admin/bloqueios", """
            {"recursoId":%d,"inicio":"%sT08:00:00","fim":"%sT12:00:00","motivo":"Troca do ar-condicionado"}
            """.formatted(LAB_03, dia, dia)).andExpect(status().isCreated());

        criar(ana, reserva(dia, "10:00", "11:00", LAB_03, null))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.conflitos[0].tipo").value("Manutenção"));
        criar(ana, reserva(dia, "12:00", "13:00", LAB_03, null)).andExpect(status().isCreated());
    }

    @Test
    void recursoDesativadoNaoPodeSerReservado() throws Exception {
        String patricia = token(PATRICIA);
        String criado = postJson(patricia, "/api/admin/recursos", """
            {"nome":"Sala 302","tipo":"SALA","localizacao":"Bloco A","capacidade":20,"abertura":"07:00","fechamento":"22:00"}""")
            .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");
        postJson(patricia, "/api/admin/recursos/" + id + "/desativar", null);
        criar(token(ANA), reserva(diaLivre(), "09:00", "10:00", id, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem", containsString("desativado")));
    }

    @Test
    void cancelamentoExigeMotivoEliberaOHorario() throws Exception {
        String ana = token(ANA), mariana = token(MARIANA);
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(ana, reserva(dia, "09:00", "10:00", SALA_204, null));

        postJson(mariana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"teste\"}")
            .andExpect(status().isForbidden());
        postJson(ana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"\"}")
            .andExpect(status().isBadRequest());
        postJson(ana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"Atividade remarcada\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELADA"))
            .andExpect(jsonPath("$.podeGerenciar").value(false))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Cancelada')].descricao", hasItem("Atividade remarcada")));
        postJson(ana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"de novo\"}")
            .andExpect(status().isConflict());

        criar(mariana, reserva(dia, "09:00", "10:00", SALA_204, null)).andExpect(status().isCreated());
    }

    @Test
    void alteracaoRevalidaConflitosERegistraHistorico() throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        codigoCriado(ana, reserva(dia, "14:00", "16:00", SALA_108, null));
        String codigo = codigoCriado(ana, reserva(dia, "09:00", "10:00", SALA_108, null));

        mvc.perform(put("/api/reservas/" + codigo).header("Authorization", "Bearer " + ana)
                .contentType(MediaType.APPLICATION_JSON).content(reserva(dia, "15:00", "17:00", SALA_108, null)))
            .andExpect(status().isConflict());
        mvc.perform(put("/api/reservas/" + codigo).header("Authorization", "Bearer " + ana)
                .contentType(MediaType.APPLICATION_JSON).content(reserva(dia, "10:00", "12:00", SALA_108, null)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.inicio").value("10:00:00"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Alterada')].descricao",
                hasItem(containsString("Horário 09:00–10:00 → 10:00–12:00"))));

        mvc.perform(put("/api/reservas/" + codigo).header("Authorization", "Bearer " + token(MARIANA))
                .contentType(MediaType.APPLICATION_JSON).content(reserva(dia, "18:00", "19:00", SALA_108, null)))
            .andExpect(status().isForbidden());
    }

    @Test
    void professorConsultaAgendaMasNaoCriaReservas() throws Exception {
        String ana = token(ANA), andre = token(ANDRE);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "19:00", "21:00", SALA_204, PROF_ANDRE));

        get(andre, "/api/reservas/minhas").andExpect(jsonPath("$[*].codigo", hasItem(codigo)));
        get(andre, "/api/reservas/" + codigo).andExpect(status().isOk()).andExpect(jsonPath("$.podeGerenciar").value(false));
        criar(andre, reserva(diaLivre(), "09:00", "10:00", SALA_204, null)).andExpect(status().isForbidden());
        postJson(andre, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"x\"}").andExpect(status().isForbidden());
    }

    @Test
    void solicitanteNaoVeReservaDeOutraPessoa() throws Exception {
        String codigo = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", SALA_204, null));
        get(token(MARIANA), "/api/reservas/" + codigo).andExpect(status().isForbidden());
        get(token(ROBERTO), "/api/reservas/" + codigo).andExpect(status().isOk());
        get(token(ANA), "/api/reservas/RES-9999").andExpect(status().isNotFound());
    }

    @Test
    void notificacoesPodemSerMarcadasComoLidas() throws Exception {
        String ana = token(ANA);
        codigoCriado(ana, reserva(diaLivre(), "09:00", "10:00", SALA_204, null));
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.naoLidas").value(greaterThan(0)));
        postJson(ana, "/api/notificacoes/lidas", null).andExpect(status().isNoContent());
        get(ana, "/api/notificacoes").andExpect(jsonPath("$.naoLidas").value(0));
    }
}
