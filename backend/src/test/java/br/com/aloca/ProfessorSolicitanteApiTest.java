package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * Professor ou coordenador como solicitante (o enunciado permite). O perfil Professor já pode reservar
 * (regra da develop, coberta em UsuariosApiTest); aqui, a conta ligada ao cadastro de professor reserva
 * para a própria turma, a tela sugere o próprio professor e a agenda dele bloqueia conflitos.
 *
 * Usuária de demonstração: Juliana Lima (Professor + Solicitante), ligada ao cadastro de professora de Química.
 */
@DisplayName("Professor como solicitante (perfis Professor + Solicitante)")
class ProfessorSolicitanteApiTest extends ApiTesteBase {

    private static final String JULIANA = "juliana.lima@aloca.edu.br";
    private static final long PROF_JULIANA = 4;

    @Test
    @DisplayName("Professora-solicitante reserva para a própria turma, vê na agenda, altera e cancela")
    void professoraReservaParaAPropriaTurma() throws Exception {
        String juliana = token(JULIANA);
        get(juliana, "/api/auth/me")
            .andExpect(jsonPath("$.perfis", containsInAnyOrder("PROFESSOR", "SOLICITANTE")))
            .andExpect(jsonPath("$.perfilRotulo").value("Solicitante / Professor"));
        // A tela de nova reserva já sugere a própria professora.
        get(juliana, "/api/reservas/opcoes").andExpect(jsonPath("$.professorDoUsuario").value((int) PROF_JULIANA));

        LocalDate dia = diaLivre();
        String codigo = codigoCriado(juliana, reserva(dia, "14:00", "16:00", LAB_03, PROF_JULIANA));
        get(juliana, "/api/reservas/" + codigo)
            .andExpect(jsonPath("$.solicitante").value("Juliana Lima"))
            .andExpect(jsonPath("$.professor.nome").value("Juliana Lima"))
            .andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.podeGerenciar").value(true));
        // Aparece uma vez só em "Minhas reservas", embora ela seja solicitante e professora.
        get(juliana, "/api/reservas/minhas").andExpect(jsonPath("$[?(@.codigo == '%s')]".formatted(codigo), hasSize(1)));
        // A agenda da professora passa a ter o horário ocupado.
        get(token(ANA), "/api/disponibilidade/professores?data=" + dia + "&busca=juliana")
            .andExpect(jsonPath("$.professores[0].ocupacoes", hasSize(1)));

        mvc.perform(put("/api/reservas/" + codigo).header("Authorization", "Bearer " + juliana)
                .contentType(MediaType.APPLICATION_JSON).content(reserva(dia, "14:00", "17:00", LAB_03, PROF_JULIANA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.historico[?(@.acao == 'Alterada')].ator", contains("Juliana Lima")));
        postJson(juliana, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"Turma em prova\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELADA"));
    }

    @Test
    @DisplayName("A própria agenda vale como conflito: a professora não se aloca em dois lugares ao mesmo tempo")
    void agendaDaProfessoraBloqueiaConflito() throws Exception {
        LocalDate dia = diaLivre();
        codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", SALA_204, PROF_JULIANA));

        criar(token(JULIANA), reserva(dia, "10:00", "12:00", LAB_03, PROF_JULIANA))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.conflitos[*].tipo", hasItem("Professor")));
    }

    @Test
    @DisplayName("Continua sem permissões de Responsável ou Administrador e não mexe em reserva de outra pessoa")
    void semPermissoesExtras() throws Exception {
        String juliana = token(JULIANA);
        String daAna = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", SALA_108, null));

        get(juliana, "/api/aprovacoes").andExpect(status().isForbidden());
        get(juliana, "/api/admin/usuarios").andExpect(status().isForbidden());
        get(juliana, "/api/relatorios").andExpect(status().isForbidden());
        postJson(juliana, "/api/reservas/" + daAna + "/cancelamento", "{\"motivo\":\"x\"}").andExpect(status().isForbidden());
    }
}
