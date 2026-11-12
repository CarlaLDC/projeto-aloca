package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

/** Evidências da pesquisa por competência e das regras de autorização de ausência. */
class QualidadeFuncionalApiTest extends ApiTesteBase {

    @Test
    void pesquisaDocentesAtivosPorCompetenciaSemExigirAdmin() throws Exception {
        get(token(ANA), "/api/professores?competencia=Matem%C3%A1tica")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[*].nome", hasItem("Lucas Oliveira")))
            .andExpect(jsonPath("$[*].competencia", hasItem("Matemática")));
        get(token(ANA), "/api/professores?competencia=nome-inexistente")
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void somenteResponsavelPodeRegistrarAusenciaENaoAntesDoInicio() throws Exception {
        String ana = token(ANA);
        String codigo = codigoCriado(ana, reserva(diaLivre(), "09:00", "10:00", SALA_204, null));
        String endpoint = "/api/reservas/" + codigo + "/nao-compareceu";
        postJson(ana, endpoint, "{\"motivo\":\"ausência\"}").andExpect(status().isForbidden());
        postJson(token(ROBERTO), endpoint, "{\"motivo\":\"ausência\"}")
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", containsString("15 minutos")));
        get(ana, "/api/reservas/" + codigo)
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status", is("APROVADA")));
    }
}
