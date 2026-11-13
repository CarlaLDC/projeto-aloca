package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.aloca.servico.ReservaService;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Funcionalidade nova desenvolvida com TDD + BDD: bloqueio de manutenção sobre reservas já feitas.
 * Testes escritos ANTES da implementação (veja src/docs/tdd-bdd-bloqueio.md).
 *
 * Regra: ao bloquear um recurso, as reservas Aprovadas que usam esse recurso (sala ou material) no
 * período são canceladas, com auditoria e aviso ao solicitante. Solicitações pendentes ficam como estão
 * (o Responsável não consegue aprová-las enquanto houver manutenção) e são listadas na resposta.
 * Se houver reserva Em uso no período, o bloqueio é recusado.
 */
@Import(RelogioDeTeste.Config.class)
@DisplayName("Bloqueio de manutenção sobre reservas já feitas (TDD)")
class BloqueioAfetaReservasApiTest extends ApiTesteBase {

    @Autowired RelogioDeTeste relogio;
    @Autowired ReservaService reservas;

    @AfterEach
    void voltarAoHorarioReal() { relogio.reiniciar(); }

    private static String bloqueio(long recurso, LocalDate dia, String inicio, String fim, String motivo) {
        return "{\"recursoId\":%d,\"inicio\":\"%sT%s:00\",\"fim\":\"%sT%s:00\",\"motivo\":\"%s\"}"
            .formatted(recurso, dia, inicio, dia, fim, motivo);
    }

    @Test
    @DisplayName("Reserva aprovada no período é cancelada, com auditoria do administrador e aviso ao solicitante")
    void aprovadaNoPeriodoEhCancelada() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_108, null));

        postJson(token(PATRICIA), "/api/admin/bloqueios", bloqueio(SALA_108, dia, "08:00", "12:00", "Troca do piso"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservasCanceladas", contains(codigo)));

        get(token(ANA), "/api/reservas/" + codigo)
            .andExpect(jsonPath("$.status").value("CANCELADA"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Cancelada')].ator", contains("Patrícia Souza")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Cancelada')].descricao", contains("Manutenção: Troca do piso")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Cancelada')].estadoAnterior", contains("Aprovada")));
        get(token(ANA), "/api/notificacoes")
            .andExpect(jsonPath("$.itens[*].mensagem", hasItem(containsString(codigo + " foi cancelada por manutenção"))));
    }

    @Test
    @DisplayName("Solicitação pendente no período continua Solicitada, é listada e não pode ser aprovada")
    void pendenteNoPeriodoContinuaEBloqueiaAprovacao() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "14:00", "16:00", AUDITORIO, null));

        postJson(token(PATRICIA), "/api/admin/bloqueios", bloqueio(AUDITORIO, dia, "13:00", "18:00", "Revisão do som"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservasCanceladas", hasSize(0)))
            .andExpect(jsonPath("$.reservasPendentes", contains(codigo)));

        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("SOLICITADA"));
        postJson(token(ROBERTO), "/api/aprovacoes/" + codigo + "/aprovar", null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsStringIgnoringCase("manutenção")));
    }

    @Test
    @DisplayName("Material bloqueado também cancela a reserva que o usa")
    void materialBloqueadoCancelaReserva() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "10:00", SALA_204, null, PROJETOR_04));

        postJson(token(PATRICIA), "/api/admin/bloqueios", bloqueio(PROJETOR_04, dia, "09:30", "12:00", "Lâmpada queimada"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservasCanceladas", contains(codigo)));
    }

    @Test
    @DisplayName("Reservas fora do período, encostadas no bloqueio, de outro recurso ou já canceladas não são afetadas")
    void naoAfetados() throws Exception {
        LocalDate dia = diaLivre();
        String ana = token(ANA);
        String depois = codigoCriado(ana, reserva(dia, "12:00", "13:00", SALA_108, null));
        String antes = codigoCriado(ana, reserva(dia, "07:00", "08:00", SALA_108, null));
        String outraSala = codigoCriado(ana, reserva(dia, "09:00", "10:00", SALA_204, null));
        String cancelada = codigoCriado(ana, reserva(dia, "09:00", "10:00", SALA_108, null));
        postJson(ana, "/api/reservas/" + cancelada + "/cancelamento", "{\"motivo\":\"Remarcada\"}");

        postJson(token(PATRICIA), "/api/admin/bloqueios", bloqueio(SALA_108, dia, "08:00", "12:00", "Pintura"))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.reservasCanceladas", hasSize(0)));

        for (String codigo : new String[] {depois, antes, outraSala}) {
            get(ana, "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("APROVADA"));
        }
        get(ana, "/api/reservas/" + cancelada).andExpect(jsonPath("$.historico[?(@.acao == 'Cancelada')]", hasSize(1)));
    }

    @Test
    @DisplayName("Com reserva em uso no período, o bloqueio é recusado e nada muda")
    void emUsoImpedeOBloqueio() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_108, null));
        relogio.ajustar(dia.atTime(9, 30));
        reservas.atualizarCiclo();

        postJson(token(PATRICIA), "/api/admin/bloqueios", bloqueio(SALA_108, dia, "10:00", "12:00", "Vazamento"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", allOf(containsString(codigo), containsString("em uso"))));

        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("EM_USO"));
        get(token(PATRICIA), "/api/admin/bloqueios").andExpect(jsonPath("$[*].motivo", not(hasItem("Vazamento"))));
    }

    @Test
    @DisplayName("A listagem de bloqueios continua igual (sem reservas canceladas)")
    void listagemNaoMuda() throws Exception {
        get(token(PATRICIA), "/api/admin/bloqueios").andExpect(status().isOk())
            .andExpect(jsonPath("$[0].recurso").exists());
    }
}
