package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.params.provider.Arguments.arguments;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.time.LocalDate;
import java.util.UUID;
import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * RF-04/RF-05/RF-13: partições e valores de fronteira pela API,
 * com aplicação e PostgreSQL real herdados de ApiTesteBase (Testcontainers).
 */
class ReservaParametrizadaApiTest extends ApiTesteBase {

    private record Periodo(String descricao, String inicio, String fim, int http, int minutos) {}

    static Stream<Arguments> periodosPorRecurso() {
        return Stream.of("Sala", "Material", "Professor").flatMap(tipo -> Stream.of(
            new Periodo("igual ao existente", "10:00", "12:00", 409, 120),
            new Periodo("contido no existente", "10:30", "11:30", 409, 60),
            new Periodo("envolve o existente", "09:00", "13:00", 409, 120),
            new Periodo("sobreposição pela esquerda", "09:00", "11:00", 409, 60),
            new Periodo("sobreposição pela direita", "11:00", "13:00", 409, 60),
            new Periodo("consecutivo antes", "08:00", "10:00", 201, 0),
            new Periodo("consecutivo depois", "12:00", "14:00", 201, 0),
            new Periodo("separado antes", "08:00", "09:00", 201, 0),
            new Periodo("separado depois", "14:00", "15:00", 201, 0)
        ).map(p -> arguments(tipo, p.descricao(), p.inicio(), p.fim(), p.http(), p.minutos())));
    }

    @ParameterizedTest(name = "[{index}] {0}: {1} ({2}–{3}) → HTTP {4}")
    @MethodSource("periodosPorRecurso")
    void verificaSobreposicaoEFronteirasDeSalaMaterialEProfessor(
            String tipo, String descricao, String inicio, String fim, int http, int minutos) throws Exception {
        String ana = token(ANA), admin = token(PATRICIA);
        LocalDate dia = diaLivre();
        long primeiraSala = criarRecurso(admin, "SALA");
        // Para material/docente, salas distintas isolam o conflito na agenda do alvo.
        long segundaSala = tipo.equals("Sala") ? primeiraSala : criarRecurso(admin, "SALA");
        Long professor = tipo.equals("Professor") ? criarProfessor(admin) : null;
        long[] materiais = tipo.equals("Material") ? new long[] { criarRecurso(admin, "EQUIPAMENTO") } : new long[0];

        codigoCriado(ana, reserva(dia, "10:00", "12:00", primeiraSala, professor, materiais));
        var resultado = criar(ana, reserva(dia, inicio, fim, segundaSala, professor, materiais))
            .andExpect(status().is(http));

        if (http == 409) {
            resultado.andExpect(jsonPath("$.conflitos", hasSize(1)))
                .andExpect(jsonPath("$.conflitos[0].tipo").value(tipo))
                .andExpect(jsonPath("$.conflitos[0].sobreposicaoMinutos").value(minutos))
                .andExpect(jsonPath("$.mensagem", not(blankOrNullString())));
        } else {
            resultado.andExpect(jsonPath("$.codigo", startsWith("RES-")))
                .andExpect(jsonPath("$.status").value("APROVADA"));
        }
        // Verifica o resultado persistido: a tentativa recusada não cria outra reserva.
        get(ana, "/api/reservas/minhas")
            .andExpect(jsonPath("$[?(@.data == '%s')]".formatted(dia), hasSize(http == 201 ? 2 : 1)));
    }

    @ParameterizedTest(name = "[{index}] término inválido: {0}–{1}")
    @CsvSource({
        "10:00, 10:00",
        "00:00, 00:00",
        "11:00, 09:00",
        "23:00, 01:00"
    })
    void rejeitaTerminoIgualOuAnteriorSemPersistirReserva(String inicio, String fim) throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        long sala = criarRecurso(token(PATRICIA), "SALA");
        criar(ana, reserva(dia, inicio, fim, sala, null))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem").value("O término deve ser posterior ao início."));
        get(ana, "/api/reservas/minhas")
            .andExpect(jsonPath("$[?(@.data == '%s')]".formatted(dia), empty()));
    }

    @ParameterizedTest(name = "[{index}] funcionamento 07h–22h: {0}–{1} → HTTP {2}")
    @CsvSource({
        "07:00, 08:00, 201",
        "21:00, 22:00, 201",
        "07:00, 22:00, 201",
        "06:59, 08:00, 400",
        "21:00, 22:01, 400",
        "06:00, 23:00, 400"
    })
    void respeitaLimitesDoHorarioDeFuncionamento(String inicio, String fim, int http) throws Exception {
        String ana = token(ANA);
        LocalDate dia = diaLivre();
        long sala = criarRecurso(token(PATRICIA), "SALA");
        var resultado = criar(ana, reserva(dia, inicio, fim, sala, null)).andExpect(status().is(http));
        if (http == 400) {
            resultado.andExpect(jsonPath("$.mensagem", containsString("funciona das")));
        } else {
            resultado.andExpect(jsonPath("$.status").value("APROVADA"));
        }
        get(ana, "/api/reservas/minhas")
            .andExpect(jsonPath("$[?(@.data == '%s')]".formatted(dia), hasSize(http == 201 ? 1 : 0)));
    }

    private long criarRecurso(String admin, String tipo) throws Exception {
        String resposta = postJson(admin, "/api/admin/recursos", """
            {"nome":"Parametrizado %s","tipo":"%s","localizacao":"Testes","capacidade":20,
             "restrito":false,"abertura":"07:00","fechamento":"22:00"}
            """.formatted(UUID.randomUUID(), tipo))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");
        return id.longValue();
    }

    private long criarProfessor(String admin) throws Exception {
        String resposta = postJson(admin, "/api/admin/professores", """
            {"nome":"Docente %s","competencia":"Testes"}
            """.formatted(UUID.randomUUID()))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Number id = JsonPath.read(resposta, "$.id");
        return id.longValue();
    }
}
