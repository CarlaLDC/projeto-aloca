package br.com.aloca;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.Matchers.hasItem;

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

/** Fluxo positivo de ausência: autorização, janela, estado persistido e trilha de auditoria. */
class NaoComparecimentoApiTest extends ApiTesteBase {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

    @TestConfiguration
    static class ConfiguracaoRelogio {
        @Bean
        @Primary
        RelogioTeste relogioTeste() {
            return new RelogioTeste(Clock.system(FUSO));
        }
    }

    static class RelogioTeste extends Clock {
        private volatile Instant instante;
        private final ZoneId zona;

        RelogioTeste(Clock inicial) {
            this.instante = inicial.instant();
            this.zona = inicial.getZone();
        }

        void fixar(Instant instante) {
            this.instante = instante;
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
            return new RelogioTeste(Clock.fixed(instante, zone));
        }
    }

    @Autowired
    RelogioTeste relogio;

    @BeforeEach
    void fixarRelogioAntesDoInicioDaReserva() {
        relogio.fixar(Instant.parse("2030-01-15T12:00:00Z")); // 09h em São Paulo
    }

    @Test
    void responsavelRegistraAusenciaEEstadoEAuditoriaFicamPersistidos() throws Exception {
        String solicitante = token(ANA);
        String responsavel = token(ROBERTO);
        String codigo = codigoCriado(solicitante, reserva(DIA, "10:00", "11:00", SALA_204, null));
        String endpoint = "/api/reservas/" + codigo + "/nao-compareceu";

        // Avança para 10h16 locais, depois de criar uma reserva válida às 10h.
        relogio.fixar(Instant.parse("2030-01-15T13:16:00Z"));

        postJson(responsavel, endpoint,
                "{\"motivo\":\"Solicitante ausente após 15 minutos da tolerância\"}")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.codigo").value(codigo))
            .andExpect(jsonPath("$.status").value("NAO_COMPARECEU"))
            .andExpect(jsonPath("$.statusRotulo").value("Não compareceu"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].ator",
                hasItem("Roberto Almeida")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].descricao",
                hasItem("Solicitante ausente após 15 minutos da tolerância")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].estadoAnterior",
                hasItem("Aprovada")))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].estadoNovo",
                hasItem("Não compareceu")));

        // Consulta posterior comprova que a mudança e o evento de auditoria foram persistidos.
        get(solicitante, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("NAO_COMPARECEU"))
            .andExpect(jsonPath("$.historico[?(@.acao == 'Não compareceu')].ator",
                hasItem("Roberto Almeida")));

        // A operação é terminal: não deve ser aplicada novamente.
        postJson(responsavel, endpoint, "{\"motivo\":\"Tentativa duplicada\"}")
            .andExpect(status().isConflict());
    }
}
