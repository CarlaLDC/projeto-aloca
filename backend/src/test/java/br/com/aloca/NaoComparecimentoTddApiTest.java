package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.aloca.servico.ReservaService;
import java.time.LocalDate;
import java.time.LocalTime;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;

/**
 * Funcionalidade desenvolvida com TDD: registrar que o solicitante NÃO COMPARECEU.
 * Estes testes foram escritos antes da implementação (veja src/docs/tdd-nao-compareceu.md).
 *
 * Regra: o Responsável registra o não comparecimento de uma reserva Aprovada ou Em uso,
 * entre o início e o fim do horário, quando nenhum material foi retirado. A reserva vai
 * para "Não compareceu", com auditoria, e o horário restante fica livre.
 */
@Import(RelogioDeTeste.Config.class)
@DisplayName("Não compareceu (TDD)")
class NaoComparecimentoTddApiTest extends ApiTesteBase {

    private static final String NAO_COMPARECEU = "/api/aprovacoes/%s/nao-compareceu";

    @Autowired RelogioDeTeste relogio;
    @Autowired ReservaService reservas;

    @AfterEach
    void voltarAoHorarioReal() {
        relogio.reiniciar();
    }

    private String marcar(String token, String codigo) throws Exception {
        return postJson(token, NAO_COMPARECEU.formatted(codigo), "{\"observacao\":\"Turma não apareceu\"}")
            .andReturn().getResponse().getContentAsString();
    }

    @ParameterizedTest(name = "reserva {0} depois do início vira \"Não compareceu\"")
    @ValueSource(strings = {"APROVADA", "EM_USO"})
    @DisplayName("Dado uma reserva iniciada sem uso, quando o responsável registra, então ela vira Não compareceu com auditoria")
    void responsavelRegistraNaoComparecimento(String estadoInicial) throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        relogio.ajustar(dia.atTime(9, 20));
        if (estadoInicial.equals("EM_USO")) {
            reservas.atualizarCiclo();
            get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("EM_USO"));
        }

        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), "{\"observacao\":\"Turma não apareceu\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NAO_COMPARECEU"))
            .andExpect(jsonPath("$.statusRotulo").value("Não compareceu"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].ator", contains("Roberto Almeida")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].estadoAnterior",
                contains(estadoInicial.equals("EM_USO") ? "Em uso" : "Aprovada")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].descricao", contains("Turma não apareceu")));
    }

    @Test
    @DisplayName("Dado o registro, então o horário restante fica livre para outra reserva")
    void horarioRestanteFicaLivre() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_108, null));
        relogio.ajustar(dia.atTime(9, 15));
        criar(token(MARIANA), reserva(dia, "09:30", "11:00", SALA_108, null)).andExpect(status().isConflict());

        marcar(token(ROBERTO), codigo);

        criar(token(MARIANA), reserva(dia, "09:30", "11:00", SALA_108, null)).andExpect(status().isCreated());
    }

    @Test
    @DisplayName("Dado o registro, então o solicitante é notificado")
    void solicitanteEhNotificado() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "14:00", "15:00", SALA_204, null));
        relogio.ajustar(dia.atTime(14, 20));

        marcar(token(ROBERTO), codigo);

        get(token(ANA), "/api/notificacoes")
            .andExpect(jsonPath("$.itens[*].mensagem", hasItem(containsString(codigo + " foi registrada como não comparecimento"))));
    }

    @Test
    @DisplayName("Quando ainda não começou, então é recusado")
    void antesDoInicioEhRecusado() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        relogio.ajustar(dia.atTime(8, 59));

        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("15 minutos após o início")));
        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    @DisplayName("No fim do horário ou depois (já Concluída), o registro é recusado")
    void noFimOuDepoisEhRecusado() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        relogio.ajustar(dia.atTime(11, 0));
        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("até o fim da reserva")));

        relogio.ajustar(dia.atTime(15, 0));
        reservas.atualizarCiclo();
        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null)
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("aprovadas ou em uso")));
        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("CONCLUIDA"));
    }

    @Test
    @DisplayName("Quando a reserva não está aprovada nem em uso, então é recusado")
    void statusQueNaoPermitemSaoRecusados() throws Exception {
        LocalDate dia = diaLivre();
        String pendente = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", LAB_02, null));
        String cancelada = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        postJson(token(ANA), "/api/reservas/" + cancelada + "/cancelamento", "{\"motivo\":\"Remarcada\"}");
        String jaMarcada = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_108, null));
        relogio.ajustar(dia.atTime(9, 30));
        marcar(token(ROBERTO), jaMarcada);

        for (String codigo : new String[] {pendente, cancelada, jaMarcada}) {
            postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), null)
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.mensagem", containsString("aprovadas ou em uso")));
        }
    }

    @Test
    @DisplayName("Somente o responsável pode registrar")
    void somenteOResponsavel() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, PROF_ANDRE));
        relogio.ajustar(dia.atTime(9, 30));

        for (String email : new String[] {ANA, PATRICIA, ANDRE}) {
            postJson(token(email), NAO_COMPARECEU.formatted(codigo), null).andExpect(status().isForbidden());
        }
        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("APROVADA"));
    }

    @Test
    @DisplayName("Depois do fim, o ciclo automático não muda uma reserva marcada como Não compareceu")
    void cicloAutomaticoNaoSobrescreve() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        relogio.ajustar(dia.atTime(9, 30));
        marcar(token(ROBERTO), codigo);

        relogio.ajustar(dia.atTime(LocalTime.of(12, 0)));
        reservas.atualizarCiclo();

        get(token(ANA), "/api/reservas/" + codigo).andExpect(jsonPath("$.status").value("NAO_COMPARECEU"));
    }

    @Test
    @DisplayName("Reserva inexistente devolve 404 e observação muito longa devolve 400")
    void validacoes() throws Exception {
        postJson(token(ROBERTO), NAO_COMPARECEU.formatted("RES-9999"), null).andExpect(status().isNotFound());

        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, null));
        relogio.ajustar(dia.atTime(9, 30));
        postJson(token(ROBERTO), NAO_COMPARECEU.formatted(codigo), "{\"observacao\":\"" + "x".repeat(501) + "\"}")
            .andExpect(status().isBadRequest());
    }
}
