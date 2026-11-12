package br.com.aloca;

import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/** Fluxo positivo de ausência: autorização, janela, estado persistido e trilha de auditoria. */
class NaoComparecimentoApiTest extends ApiTesteBase {

    private static final ZoneId FUSO = ZoneId.of("America/Sao_Paulo");
    private static final LocalDate DIA = LocalDate.of(2030, 1, 15);

    @MockitoBean
    Clock relogio;

    @BeforeEach
    void fixarRelogioAntesDoInicioDaReserva() {
        when(relogio.instant()).thenReturn(Instant.parse("2030-01-15T12:00:00Z"));
        when(relogio.getZone()).thenReturn(FUSO);
    }

    @Test
    void responsavelRegistraAusenciaEEstadoEAuditoriaFicamPersistidos() throws Exception {
        String solicitante = token(ANA);
        String responsavel = token(ROBERTO);
        String codigo = codigoCriado(solicitante, reserva(DIA, "10:00", "11:00", SALA_204, null));
        String endpoint = "/api/reservas/" + codigo + "/nao-compareceu";

        // Avança o relógio para 10h16 locais, depois de criar uma reserva válida às 10h.
        when(relogio.instant()).thenReturn(Instant.parse("2030-01-15T13:16:00Z"));

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
