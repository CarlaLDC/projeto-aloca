package br.com.aloca.caixapreta;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Caixa-preta · entradas malformadas e mensagens de erro sem detalhes internos (RNF-03). */
@DisplayName("Caixa-preta: erros seguros")
class ErrosSegurosCaixaPretaTest extends CaixaPretaBase {

    /** Nenhuma resposta de erro pode expor classe, pacote, SQL ou pilha de execução. */
    private static void semDetalhesInternos(ClienteHttp.Resposta r) {
        assertThat(r.corpo()).doesNotContain("Exception", "br.com.aloca", "org.springframework", "java.", "SQL", "\tat ", "stackTrace", "trace");
    }

    @Test
    @DisplayName("JSON malformado, tipo errado e campo desconhecido devolvem 400 com mensagem limpa")
    void entradasMalformadas() {
        String ana = api.token(ANA);
        for (String corpo : new String[] {"{", "[]", "{\"atividade\":\"x\",\"data\":\"amanhã\",\"inicio\":\"09:00\",\"fim\":\"10:00\",\"espacoId\":2}",
                "{\"atividade\":\"x\",\"data\":\"2031-01-10\",\"inicio\":\"25:99\",\"fim\":\"10:00\",\"espacoId\":2}",
                "{\"atividade\":\"x\",\"data\":\"2031-01-10\",\"inicio\":\"09:00\",\"fim\":\"10:00\",\"espacoId\":\"abc\"}"}) {
            ClienteHttp.Resposta r = api.post("/api/reservas", ana, corpo).esperar(400);
            assertThat(r.texto("mensagem")).isNotBlank();
            semDetalhesInternos(r);
        }
    }

    @Test
    @DisplayName("Campo grande demais e injeção de SQL em busca não quebram a API")
    void entradasHostis() {
        String ana = api.token(ANA);
        ClienteHttp.Resposta longo = api.post("/api/reservas", ana,
            "{\"atividade\":\"%s\",\"data\":\"2031-01-10\",\"inicio\":\"09:00\",\"fim\":\"10:00\",\"espacoId\":2}".formatted("x".repeat(151))).esperar(400);
        semDetalhesInternos(longo);
        ClienteHttp.Resposta sql = api.get("/api/disponibilidade?busca=%27%20OR%201=1--", ana).esperar(200);
        assertThat(sql.json().path("recursos").size()).isZero();
        semDetalhesInternos(api.get("/api/disponibilidade?data=nao-e-data", ana).esperar(400));
    }

    @Test
    @DisplayName("Reserva inexistente, id inválido e rota inexistente: 404/400 sem detalhes internos")
    void naoEncontrado() {
        String ana = api.token(ANA), patricia = api.token(PATRICIA);
        semDetalhesInternos(api.get("/api/reservas/RES-999999", ana).esperar(404));
        semDetalhesInternos(api.get("/api/admin/usuarios/abc", patricia).esperar(400));
        semDetalhesInternos(api.get("/api/admin/usuarios/999999", patricia).esperar(404));
        assertThat(api.get("/api/rota-que-nao-existe", ana).status()).isEqualTo(404);
    }

    @Test
    @DisplayName("Respostas JSON em UTF-8 e cabeçalhos de segurança presentes")
    void cabecalhos() {
        ClienteHttp.Resposta r = api.get("/api/auth/me", api.token(ANA)).esperar(200);
        assertThat(r.bruta().headers().firstValue("Content-Type")).hasValueSatisfying(v -> assertThat(v).startsWith("application/json"));
        assertThat(r.bruta().headers().firstValue("X-Content-Type-Options")).contains("nosniff");
        assertThat(r.bruta().headers().firstValue("Cache-Control")).hasValueSatisfying(v -> assertThat(v).contains("no-store"));
        assertThat(r.texto("nome")).isEqualTo("Ana Martins");
    }
}
