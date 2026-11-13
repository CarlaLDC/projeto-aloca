package br.com.aloca.caixapreta;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

/** Cliente HTTP mínimo da suíte caixa-preta: só URL, método, token, corpo JSON e resposta. */
final class ClienteHttp {

    static final String SENHA = "aloca123";
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private static final ObjectMapper JSON = new ObjectMapper();

    private final String base;

    ClienteHttp(String base) { this.base = base; }

    record Resposta(int status, String corpo, HttpResponse<String> bruta) {
        JsonNode json() { return corpo.isBlank() ? JSON.nullNode() : JSON.readTree(corpo); }
        String texto(String campo) { return json().path(campo).asString(); }
        Resposta esperar(int esperado) {
            assertThat(status).as("HTTP %d esperado, veio %d: %s", esperado, status, corpo).isEqualTo(esperado);
            return this;
        }
    }

    Resposta chamar(String metodo, String caminho, String token, String corpo) {
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create(base + caminho)).timeout(Duration.ofSeconds(15))
            .method(metodo, corpo == null ? HttpRequest.BodyPublishers.noBody() : HttpRequest.BodyPublishers.ofString(corpo));
        if (corpo != null) req.header("Content-Type", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        try {
            HttpResponse<String> r = HTTP.send(req.build(), HttpResponse.BodyHandlers.ofString());
            return new Resposta(r.statusCode(), r.body(), r);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao chamar " + metodo + " " + caminho, e);
        }
    }

    Resposta get(String caminho, String token) { return chamar("GET", caminho, token, null); }
    Resposta post(String caminho, String token, String corpo) { return chamar("POST", caminho, token, corpo == null ? "{}" : corpo); }
    Resposta put(String caminho, String token, String corpo) { return chamar("PUT", caminho, token, corpo); }
    Resposta delete(String caminho, String token) { return chamar("DELETE", caminho, token, null); }

    String token(String email) {
        return post("/api/auth/login", null, "{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, SENHA))
            .esperar(200).texto("token");
    }
}
