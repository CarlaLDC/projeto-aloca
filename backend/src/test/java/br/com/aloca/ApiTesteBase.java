package br.com.aloca;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.aloca.config.DadosUsuarios;
import com.jayway.jsonpath.JsonPath;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.MockMvcBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Base dos testes de integração: sobe a aplicação com MySQL em contêiner (Testcontainers)
 * e os dados de demonstração. Sem mocks: persistência e regras rodam no banco real.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(BancoMySqlTeste.class)
public abstract class ApiTesteBase {

    // Ids dos dados de demonstração (ordem de criação em DadosIniciais).
    protected static final long SALA_204 = 1, SALA_108 = 2, AUDITORIO = 4, LAB_02 = 5, LAB_03 = 6, LAB_QUIMICA = 7;
    protected static final long PROJETOR_04 = 8, NOTEBOOK_08 = 10, KIT_ROBOTICA = 12;
    protected static final long PROF_ANDRE = 1, PROF_LUCAS = 2;

    protected static final String ANA = "ana.martins@aloca.edu.br";
    protected static final String MARIANA = "mariana.costa@aloca.edu.br";
    protected static final String ROBERTO = "roberto.almeida@aloca.edu.br";
    protected static final String PATRICIA = "patricia.souza@aloca.edu.br";
    protected static final String ANDRE = "andre.lima@aloca.edu.br";

    /** Cada teste usa um dia futuro diferente para não interferir nos outros. */
    private static final AtomicInteger proximoDia = new AtomicInteger(30);

    @Autowired
    protected MockMvc mvc;

    /** Lê as respostas como UTF-8 (o JSON não informa charset e o padrão do MockMvc é ISO-8859-1). */
    @TestConfiguration
    static class Utf8 {
        @Bean
        MockMvcBuilderCustomizer utf8() {
            return builder -> builder.defaultResponseCharacterEncoding(StandardCharsets.UTF_8);
        }
    }

    protected String token(String email) throws Exception {
        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, DadosUsuarios.SENHA_DEMO)))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.token");
    }

    protected static LocalDate diaLivre() {
        LocalDate d;
        do {
            d = LocalDate.now().plusDays(proximoDia.getAndIncrement());
        } while (d.getDayOfWeek() == DayOfWeek.SUNDAY);
        return d;
    }

    protected static String reserva(LocalDate data, String inicio, String fim, long espaco, Long professor, long... materiais) {
        StringBuilder mats = new StringBuilder();
        for (long m : materiais) mats.append(mats.isEmpty() ? "" : ",").append(m);
        return """
            {"atividade":"Atividade de teste","data":"%s","inicio":"%s","fim":"%s","espacoId":%d,"professorId":%s,"materialIds":[%s]}
            """.formatted(data, inicio, fim, espaco, professor == null ? "null" : professor, mats);
    }

    protected ResultActions criar(String token, String corpo) throws Exception {
        return mvc.perform(post("/api/reservas").header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(corpo));
    }

    protected String codigoCriado(String token, String corpo) throws Exception {
        String resposta = criar(token, corpo).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(resposta, "$.codigo");
    }

    protected ResultActions get(String token, String url) throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(url)
            .header("Authorization", "Bearer " + token));
    }

    protected ResultActions postJson(String token, String url, String corpo) throws Exception {
        return mvc.perform(post(url).header("Authorization", "Bearer " + token)
            .contentType(MediaType.APPLICATION_JSON).content(corpo == null ? "{}" : corpo));
    }
}
