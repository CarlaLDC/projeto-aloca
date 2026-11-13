package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.aloca.servico.ReservaService;
import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;

/**
 * Ciclo de vida completo da reserva pela API, com PostgreSQL real, e a prova de que
 * TODA transição de estado gera um registro de auditoria (RF-10).
 *
 * A matriz de transições permitidas está em CicloDeVidaReserva e é testada isoladamente em
 * CicloDeVidaReservaTest. Aqui cada uma das 12 transições da matriz, mais a criação e a
 * aprovação automática, é provocada pelo caminho real (API ou ciclo automático).
 */
@Import(RelogioDeTeste.Config.class)
@DisplayName("Ciclo de vida da reserva e auditoria de todas as transições (API)")
class CicloDeVidaApiTest extends ApiTesteBase {

    @Autowired RelogioDeTeste relogio;
    @Autowired ReservaService reservas;

    @AfterEach
    void voltarAoHorarioReal() { relogio.reiniciar(); }

    /** Eventos de mudança de estado (as notificações também ficam no histórico, mas não mudam o estado). */
    private List<Map<String, Object>> transicoes(String codigo) throws Exception {
        String json = get(token(ANA), "/api/reservas/" + codigo).andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(json, "$.historico[?(@.acao != 'Notificação')]");
    }

    private String estado(String codigo) throws Exception {
        return JsonPath.read(get(token(ANA), "/api/reservas/" + codigo).andReturn().getResponse().getContentAsString(), "$.status");
    }

    private static String resumo(Map<String, Object> e) {
        return e.get("acao") + ": " + e.get("estadoAnterior") + " → " + e.get("estadoNovo") + " (" + e.get("ator") + ")";
    }

    @Test
    @DisplayName("SOLICITADA → APROVADA → EM_USO → CONCLUÍDA, com um registro de auditoria por passo")
    void cicloCompleto() throws Exception {
        LocalDate dia = diaLivre();
        String codigo = codigoCriado(token(ANA), reserva(dia, "09:00", "11:00", AUDITORIO, null));
        assertThat(estado(codigo)).isEqualTo("SOLICITADA");

        postJson(token(ROBERTO), "/api/aprovacoes/" + codigo + "/aprovar", null).andExpect(status().isOk());
        assertThat(estado(codigo)).isEqualTo("APROVADA");

        relogio.ajustar(dia.atTime(9, 0));
        reservas.atualizarCiclo();
        assertThat(estado(codigo)).isEqualTo("EM_USO");

        relogio.ajustar(dia.atTime(11, 0));
        reservas.atualizarCiclo();
        assertThat(estado(codigo)).isEqualTo("CONCLUIDA");

        List<Map<String, Object>> eventos = transicoes(codigo);
        assertThat(eventos).extracting(CicloDeVidaApiTest::resumo).containsExactly(
            "Solicitada: null → Solicitada (Ana Martins)",
            "Aprovada: Solicitada → Aprovada (Roberto Almeida)",
            "Em uso: Aprovada → Em uso (Sistema)",
            "Concluída: Em uso → Concluída (Sistema)");
        assertThat(eventos).allSatisfy(e -> assertThat(e.get("dataHora")).isNotNull());
        assertThat(eventos.subList(2, 4)).extracting(e -> LocalDateTime.parse((String) e.get("dataHora")))
            .containsExactly(dia.atTime(9, 0), dia.atTime(11, 0));

        // Concluída não volta: o ciclo automático rodando de novo não cria eventos.
        reservas.atualizarCiclo();
        assertThat(transicoes(codigo)).hasSize(4);
    }

    @ParameterizedTest(name = "{0}: {1} → {2} por {3}")
    @CsvSource(delimiter = '|', value = {
        "criação                               |              | Solicitada     | Ana Martins     | Solicitada",
        "aprovação automática (sem restrição)  | Solicitada   | Aprovada       | Sistema         | Aprovada",
        "aprovação pelo responsável            | Solicitada   | Aprovada       | Roberto Almeida | Aprovada",
        "rejeição                              | Solicitada   | Rejeitada      | Roberto Almeida | Rejeitada",
        "cancelamento de solicitação           | Solicitada   | Cancelada      | Ana Martins     | Cancelada",
        "alteração de solicitação              | Solicitada   | Solicitada     | Ana Martins     | Alterada",
        "alteração de reserva aprovada         | Aprovada     | Aprovada       | Ana Martins     | Alterada",
        "alteração que exige nova aprovação    | Aprovada     | Solicitada     | Ana Martins     | Alterada",
        "cancelamento de reserva aprovada      | Aprovada     | Cancelada      | Ana Martins     | Cancelada",
        "início do horário                     | Aprovada     | Em uso         | Sistema         | Em uso",
        "fim do horário                        | Em uso       | Concluída      | Sistema         | Concluída",
        "fim sem passar por Em uso             | Aprovada     | Concluída      | Sistema         | Concluída",
        "não compareceu (aprovada)             | Aprovada     | Não compareceu | Roberto Almeida | Não compareceu",
        "não compareceu (em uso)               | Em uso       | Não compareceu | Roberto Almeida | Não compareceu"
    })
    @DisplayName("Cada transição da matriz gera auditoria com estado anterior, estado novo, ator e data")
    void cadaTransicaoEhAuditada(String caso, String anterior, String novo, String ator, String acao) throws Exception {
        String codigo = provocar(caso.trim());

        List<Map<String, Object>> eventos = transicoes(codigo);
        Map<String, Object> ultimo = eventos.getLast();
        assertThat(ultimo.get("estadoAnterior")).isEqualTo(anterior);
        assertThat(ultimo.get("estadoNovo")).isEqualTo(novo);
        assertThat(ultimo.get("ator")).isEqualTo(ator);
        assertThat(ultimo.get("acao")).isEqualTo(acao);
        assertThat(ultimo.get("dataHora")).isNotNull();
        // O estado atual é o estado novo do último evento: nenhuma mudança fica sem registro.
        Map<String, String> rotulo = Map.of("SOLICITADA", "Solicitada", "APROVADA", "Aprovada", "REJEITADA", "Rejeitada",
            "CANCELADA", "Cancelada", "EM_USO", "Em uso", "CONCLUIDA", "Concluída", "NAO_COMPARECEU", "Não compareceu");
        assertThat(rotulo.get(estado(codigo))).isEqualTo(novo);
        // E a cadeia é contínua: cada evento parte do estado em que o anterior terminou.
        for (int i = 1; i < eventos.size(); i++) {
            assertThat(eventos.get(i).get("estadoAnterior")).as("evento %d de %s", i, codigo)
                .isEqualTo(eventos.get(i - 1).get("estadoNovo"));
        }
    }

    /** Leva uma reserva nova até a transição pedida, pelo caminho real. Devolve o código. */
    private String provocar(String caso) throws Exception {
        LocalDate dia = diaLivre();
        String ana = token(ANA), roberto = token(ROBERTO);
        String livre = reserva(dia, "09:00", "11:00", SALA_108, null);
        String restrita = reserva(dia, "09:00", "11:00", AUDITORIO, null);
        return switch (caso) {
            case "criação" -> codigoCriado(ana, restrita);
            case "aprovação automática (sem restrição)" -> codigoCriado(ana, livre);
            case "aprovação pelo responsável" -> {
                String c = codigoCriado(ana, restrita);
                postJson(roberto, "/api/aprovacoes/" + c + "/aprovar", null).andExpect(status().isOk());
                yield c;
            }
            case "rejeição" -> {
                String c = codigoCriado(ana, restrita);
                postJson(roberto, "/api/aprovacoes/" + c + "/rejeitar", "{\"justificativa\":\"Evento institucional\"}").andExpect(status().isOk());
                yield c;
            }
            case "cancelamento de solicitação" -> cancelar(ana, codigoCriado(ana, restrita));
            case "alteração de solicitação" -> alterar(ana, codigoCriado(ana, restrita), reserva(dia, "09:00", "12:00", AUDITORIO, null));
            case "alteração de reserva aprovada" -> alterar(ana, codigoCriado(ana, livre), reserva(dia, "09:00", "12:00", SALA_108, null));
            case "alteração que exige nova aprovação" ->
                alterar(ana, codigoCriado(ana, livre), reserva(dia, "09:00", "11:00", SALA_108, null, KIT_ROBOTICA));
            case "cancelamento de reserva aprovada" -> cancelar(ana, codigoCriado(ana, livre));
            case "início do horário" -> noRelogio(codigoCriado(ana, livre), dia.atTime(9, 30));
            case "fim do horário" -> {
                String c = noRelogio(codigoCriado(ana, livre), dia.atTime(9, 30));
                yield noRelogio(c, dia.atTime(11, 0));
            }
            case "fim sem passar por Em uso" -> noRelogio(codigoCriado(ana, livre), dia.atTime(12, 0));
            case "não compareceu (aprovada)" -> {
                String c = codigoCriado(ana, livre);
                relogio.ajustar(dia.atTime(9, 20));
                yield naoCompareceu(roberto, c);
            }
            case "não compareceu (em uso)" -> naoCompareceu(roberto, noRelogio(codigoCriado(ana, livre), dia.atTime(9, 20)));
            default -> throw new IllegalArgumentException(caso);
        };
    }

    private String noRelogio(String codigo, LocalDateTime quando) {
        relogio.ajustar(quando);
        reservas.atualizarCiclo();
        return codigo;
    }

    private String cancelar(String token, String codigo) throws Exception {
        postJson(token, "/api/reservas/" + codigo + "/cancelamento", "{\"motivo\":\"Remarcada\"}").andExpect(status().isOk());
        return codigo;
    }

    private String alterar(String token, String codigo, String corpo) throws Exception {
        mvc.perform(put("/api/reservas/" + codigo).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(corpo)).andExpect(status().isOk());
        return codigo;
    }

    private String naoCompareceu(String token, String codigo) throws Exception {
        postJson(token, "/api/aprovacoes/" + codigo + "/nao-compareceu", null).andExpect(status().isOk());
        return codigo;
    }
}
