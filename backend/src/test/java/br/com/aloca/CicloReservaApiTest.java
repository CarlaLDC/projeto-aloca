package br.com.aloca;

import static org.hamcrest.Matchers.hasItem;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.aloca.servico.ReservaService;
import com.jayway.jsonpath.JsonPath;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/** Comprova o ciclo completo de uma reserva, inclusive as transições automáticas e sua auditoria. */
class CicloReservaApiTest extends ApiTesteBase {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

    @TestConfiguration
    static class ConfiguracaoRelogio {
        @Bean
        @Primary
        RelogioCicloTeste relogioCicloTeste() {
            return new RelogioCicloTeste(Clock.system(FUSO));
        }
    }

    static class RelogioCicloTeste extends Clock {
        private volatile Instant instante;
        private final ZoneId zona;

        RelogioCicloTeste(Clock inicial) {
            this.instante = inicial.instant();
            this.zona = inicial.getZone();
        }

        void fixar(String instante) {
            this.instante = Instant.parse(instante);
        }

        @Override
        public Instant instant() {
            return instante;
        }

        @Override
        public ZoneId getZone() {
            return zona;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return new RelogioCicloTeste(Clock.fixed(instante, zone));
        }
    }

    @Autowired
    RelogioCicloTeste relogio;

    @Autowired
    ReservaService reservas;

    @BeforeEach
    void iniciarAntesDaReserva() {
        relogio.fixar("2030-01-15T12:00:00Z"); // 09h em São Paulo
    }

    @Test
    void percorreSolicitadaAprovadaEmUsoEConcluidaComAuditoria() throws Exception {
        String solicitante = token(ANA);
        String responsavel = token(ROBERTO);

        String codigo = codigoCriado(solicitante, reserva(DIA, "10:00", "11:00", LAB_02, null));

        // O laboratório é restrito: a criação inicia em SOLICITADA.
        get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("SOLICITADA"))
            .andExpect(jsonPath("$.historico[*].estadoNovo", hasItem("Solicitada")));

        // Decisão humana do responsável: SOLICITADA -> APROVADA.
        postJson(responsavel, "/api/aprovacoes/" + codigo + "/aprovar", null)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("APROVADA"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Aprovada')].ator", hasItem("Roberto Almeida")));

        // O agendador executa a transição para EM_USO quando chega o início.
        // A rotina é global e pode avançar outras reservas de demonstração no mesmo instante;
        // por isso, a asserção é feita sobre a reserva deste cenário, não sobre a contagem global.
        relogio.fixar("2030-01-15T13:00:00Z"); // 10h em São Paulo
        reservas.atualizarCiclo();
        get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("EM_USO"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Em uso')].ator", hasItem("Sistema")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Em uso')].estadoAnterior", hasItem("Aprovada")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Em uso')].estadoNovo", hasItem("Em uso")));

        // Ao atingir o fim, a mesma rotina conclui a reserva e registra a segunda transição automática.
        relogio.fixar("2030-01-15T14:00:00Z"); // 11h em São Paulo
        reservas.atualizarCiclo();
        get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CONCLUIDA"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Concluída')].ator", hasItem("Sistema")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Concluída')].estadoAnterior", hasItem("Em uso")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Concluída')].estadoNovo", hasItem("Concluída")));

        // A rotina é idempotente para esta reserva: não cria nova entrada de auditoria terminal.
        String antes = get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        Object historicoAntes = JsonPath.read(antes, "$.historico");

        relogio.fixar("2030-01-15T14:30:00Z");
        reservas.atualizarCiclo();

        String depois = get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        assertEquals("CONCLUIDA", JsonPath.read(depois, "$.status"));
        assertEquals(historicoAntes, JsonPath.read(depois, "$.historico"));
    }
}
