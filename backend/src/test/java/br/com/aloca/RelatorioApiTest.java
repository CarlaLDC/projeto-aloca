package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tela 11 · Relatórios (RF-12) e a matriz de acesso por perfil (RF-01 / RNF-03). */
class RelatorioApiTest extends ApiTesteBase {

    @Test
    void relatorioContaConflitosEvitadosEExportaCsv() throws Exception {
        String ana = token(ANA), patricia = token(PATRICIA);
        var dia = diaLivre();
        codigoCriado(ana, reserva(dia, "09:00", "11:00", SALA_108, null));
        criar(ana, reserva(dia, "10:00", "11:00", SALA_108, null)).andExpect(status().isConflict());

        String corpo = get(patricia, "/api/relatorios").andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        int conflitos = JsonPath.read(corpo, "$.conflitosEvitados");
        org.assertj.core.api.Assertions.assertThat(conflitos).isGreaterThan(0);

        get(patricia, "/api/relatorios?inicio=" + dia + "&fim=" + dia)
            .andExpect(jsonPath("$.totalReservas").value(greaterThanOrEqualTo(1)))
            .andExpect(jsonPath("$.recursos[?(@.nome == 'Sala 108')].horas", hasItem(greaterThanOrEqualTo(2.0))));
        get(patricia, "/api/relatorios?inicio=2026-02-10&fim=2026-01-01").andExpect(status().isBadRequest());
        get(patricia, "/api/relatorios/csv")
            .andExpect(status().isOk())
            .andExpect(header().string("Content-Disposition", containsString(".csv")))
            .andExpect(content().string(containsString("Conflitos evitados")));
    }

    /** Cada perfil só acessa as funções permitidas. */
    @Test
    void matrizDeAcessoPorPerfil() throws Exception {
        record Caso(String email, String url, int status) {}
        List<Caso> casos = List.of(
            new Caso(ANA, "/api/painel", 200),
            new Caso(ANA, "/api/disponibilidade", 200),
            new Caso(ANA, "/api/aprovacoes", 403),
            new Caso(ANA, "/api/movimentacoes", 403),
            new Caso(ANA, "/api/relatorios", 403),
            new Caso(ANA, "/api/admin/recursos", 403),
            new Caso(ROBERTO, "/api/aprovacoes", 200),
            new Caso(ROBERTO, "/api/movimentacoes", 200),
            new Caso(ROBERTO, "/api/relatorios", 200),
            new Caso(ROBERTO, "/api/admin/recursos", 403),
            new Caso(PATRICIA, "/api/aprovacoes", 403),
            new Caso(PATRICIA, "/api/movimentacoes", 403),
            new Caso(PATRICIA, "/api/relatorios", 200),
            new Caso(PATRICIA, "/api/admin/recursos", 200),
            new Caso(ANDRE, "/api/painel", 200),
            new Caso(ANDRE, "/api/reservas/minhas", 200),
            new Caso(ANDRE, "/api/aprovacoes", 403),
            new Caso(ANDRE, "/api/relatorios", 403));
        for (Caso c : casos) {
            get(token(c.email()), c.url()).andExpect(status().is(c.status()));
        }
    }
}
