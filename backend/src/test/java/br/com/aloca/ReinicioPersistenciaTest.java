package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import org.testcontainers.postgresql.PostgreSQLContainer;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/**
 * RNF-04 (confiabilidade): reservas e histórico continuam salvos depois que a aplicação reinicia.
 *
 * Sobe a aplicação inteira (servidor HTTP real) sobre um PostgreSQL em container, cria e altera uma
 * reserva, desliga a aplicação, sobe de novo sobre o MESMO banco e confere que nada se perdeu e que
 * os dados de demonstração não foram duplicados.
 */
@DisplayName("Dados preservados após reiniciar a aplicação (RNF-04)")
class ReinicioPersistenciaTest {

    private static final PostgreSQLContainer banco = new PostgreSQLContainer(PostgresTestcontainer.IMAGEM);
    private static final HttpClient http = HttpClient.newHttpClient();
    private static final ObjectMapper json = new ObjectMapper();

    @BeforeAll
    static void subirBanco() { banco.start(); }

    @AfterAll
    static void derrubarBanco() { banco.stop(); }

    private static ConfigurableApplicationContext iniciarAplicacao() {
        // Passadas como argumentos (como num servidor real), para valer mais que o application.properties
        // de teste, que usa create-drop e apagaria o esquema ao desligar.
        return new SpringApplicationBuilder(AlocaApplication.class).run(
            "--spring.datasource.url=" + banco.getJdbcUrl(),
            "--spring.datasource.username=" + banco.getUsername(),
            "--spring.datasource.password=" + banco.getPassword(),
            "--server.port=0",
            "--spring.jpa.hibernate.ddl-auto=update");
    }

    private static String base(ConfigurableApplicationContext app) {
        return "http://localhost:" + app.getEnvironment().getProperty("local.server.port");
    }

    private static JsonNode chamar(String metodo, String url, String token, String corpo, int esperado) throws Exception {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(url)).header("Content-Type", "application/json")
            .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        if (token != null) req.header("Authorization", "Bearer " + token);
        HttpResponse<String> resp = http.send(req.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(resp.statusCode()).as("%s %s → %s", metodo, url, resp.body()).isEqualTo(esperado);
        return json.readTree(resp.body());
    }

    private static String login(String base) throws Exception {
        return chamar("POST", base + "/api/auth/login", null,
            "{\"email\":\"ana.martins@aloca.edu.br\",\"senha\":\"aloca123\"}", 200).get("token").asString();
    }

    @Test
    @DisplayName("Reserva alterada, com histórico, continua igual depois de reiniciar")
    void reservaEHistoricoSobrevivemAoReinicio() throws Exception {
        String data = LocalDate.now().plusDays(500).toString();
        String codigo;
        JsonNode antes;
        int recursosAntes;

        try (ConfigurableApplicationContext app = iniciarAplicacao()) {
            String base = base(app), token = login(base);
            codigo = chamar("POST", base + "/api/reservas", token,
                "{\"atividade\":\"Antes do reinício\",\"data\":\"" + data + "\",\"inicio\":\"09:00\",\"fim\":\"10:00\",\"espacoId\":2}",
                201).get("codigo").asString();
            chamar("PUT", base + "/api/reservas/" + codigo, token,
                "{\"atividade\":\"Alterada antes do reinício\",\"data\":\"" + data + "\",\"inicio\":\"09:00\",\"fim\":\"11:00\",\"espacoId\":2}", 200);
            antes = chamar("GET", base + "/api/reservas/" + codigo, token, null, 200);
            recursosAntes = chamar("GET", base + "/api/reservas/opcoes", token, null, 200).get("espacos").size();
        }

        try (ConfigurableApplicationContext app = iniciarAplicacao()) {
            String base = base(app), token = login(base);
            JsonNode depois = chamar("GET", base + "/api/reservas/" + codigo, token, null, 200);

            assertThat(depois.get("atividade").asString()).isEqualTo("Alterada antes do reinício");
            assertThat(depois.get("fim").asString()).isEqualTo("11:00:00");
            assertThat(depois.get("status").asString()).isEqualTo(antes.get("status").asString());
            assertThat(depois.get("historico")).isEqualTo(antes.get("historico"));
            assertThat(chamar("GET", base + "/api/reservas/opcoes", token, null, 200).get("espacos").size())
                .as("dados de demonstração não são duplicados").isEqualTo(recursosAntes);
            // A sala continua ocupada: o horário salvo antes do reinício ainda gera conflito.
            chamar("POST", base + "/api/reservas", token,
                "{\"atividade\":\"Depois do reinício\",\"data\":\"" + data + "\",\"inicio\":\"10:00\",\"fim\":\"10:30\",\"espacoId\":2}", 409);
        }
    }
}
