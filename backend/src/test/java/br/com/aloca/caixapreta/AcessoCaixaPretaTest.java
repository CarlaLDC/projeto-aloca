package br.com.aloca.caixapreta;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Caixa-preta · autenticação e matriz de acesso por perfil (RF-01, RNF-03). */
@DisplayName("Caixa-preta: autenticação e acesso por perfil")
class AcessoCaixaPretaTest extends CaixaPretaBase {

    private static final Map<String, String> EMAIL = Map.of(
        "SOLICITANTE", ANA, "RESPONSAVEL", ROBERTO, "ADMINISTRADOR", PATRICIA, "PROFESSOR", ANDRE);

    @Test
    @DisplayName("Login certo devolve token e perfil; senha errada devolve 401 sem dizer se a conta existe")
    void login() {
        ClienteHttp.Resposta ok = api.post("/api/auth/login", null, "{\"email\":\"%s\",\"senha\":\"aloca123\"}".formatted(ANA)).esperar(200);
        assertThat(ok.texto("token")).isNotBlank();
        assertThat(ok.json().path("usuario").path("perfis").toString()).contains("SOLICITANTE");

        String senhaErrada = api.post("/api/auth/login", null, "{\"email\":\"%s\",\"senha\":\"errada-1\"}".formatted(MARIANA)).esperar(401).texto("mensagem");
        String contaInexistente = api.post("/api/auth/login", null, "{\"email\":\"ninguem@aloca.edu.br\",\"senha\":\"errada-1\"}").esperar(401).texto("mensagem");
        assertThat(senhaErrada).isEqualTo(contaInexistente);
    }

    @Test
    @DisplayName("Sem token, token adulterado ou token de outro formato: 401")
    void tokensInvalidos() {
        String token = api.token(ANA);
        api.get("/api/auth/me", null).esperar(401);
        api.get("/api/auth/me", token.substring(0, token.length() - 3) + "abc").esperar(401);
        api.get("/api/auth/me", "nao-e-um-jwt").esperar(401);
        api.get("/api/auth/me", token).esperar(200);
    }

    @Test
    @DisplayName("Login funciona mesmo com um token velho ou inválido no cabeçalho (sessão expirada no navegador)")
    void loginIgnoraTokenVelho() {
        String corpo = "{\"email\":\"%s\",\"senha\":\"aloca123\"}".formatted(ANA);
        assertThat(api.chamar("POST", "/api/auth/login", "token-velho-invalido", corpo).status()).isEqualTo(200);
        assertThat(api.chamar("POST", "/api/auth/login", "${token}", corpo).status()).isEqualTo(200);
        // Nas demais rotas, o token inválido continua sendo recusado.
        assertThat(api.get("/api/auth/me", "token-velho-invalido").status()).isEqualTo(401);
    }

    @ParameterizedTest(name = "{0} {1}: anônimo {2} · solicitante {3} · responsável {4} · administrador {5} · professor {6}")
    @CsvSource({
        "GET,  /api/auth/me,                        401, 200, 200, 200, 200",
        "GET,  /api/painel,                         401, 200, 200, 200, 200",
        "GET,  /api/disponibilidade,                401, 200, 200, 200, 200",
        "GET,  /api/disponibilidade/professores,    401, 200, 200, 200, 200",
        "GET,  /api/reservas/minhas,                401, 200, 200, 200, 200",
        "GET,  /api/reservas/opcoes,                401, 200, 200, 200, 200",
        "GET,  /api/notificacoes,                   401, 200, 200, 200, 200",
        "GET,  /api/aprovacoes,                     401, 403, 200, 403, 403",
        "GET,  /api/movimentacoes,                  401, 403, 200, 403, 403",
        "GET,  /api/relatorios,                     401, 403, 200, 200, 403",
        "GET,  /api/relatorios/csv,                 401, 403, 200, 200, 403",
        "GET,  /api/admin/recursos,                 401, 403, 403, 200, 403",
        "GET,  /api/admin/professores,              401, 403, 403, 200, 403",
        "GET,  /api/admin/usuarios,                 401, 403, 403, 200, 403",
        "GET,  /api/admin/bloqueios,                401, 403, 403, 200, 403",
        // Corpo vazio: quem pode passa da autorização e cai na validação (400); quem não pode recebe 403 antes.
        // Professor também reserva (professor/coordenador é solicitante, como no enunciado).
        "POST, /api/reservas,                       401, 400, 400, 400, 400",
        "POST, /api/admin/recursos,                 401, 403, 403, 400, 403",
        "POST, /api/admin/usuarios,                 401, 403, 403, 400, 403",
        "POST, /api/admin/bloqueios,                401, 403, 403, 400, 403",
        "POST, /api/aprovacoes/RES-9999/rejeitar,   401, 403, 400, 403, 403",
        "POST, /api/aprovacoes/RES-9999/aprovar,    401, 403, 404, 403, 403"
    })
    @DisplayName("Matriz perfil × rota")
    void matrizDeAcesso(String metodo, String rota, int anonimo, int solicitante, int responsavel, int administrador, int professor) {
        String corpo = metodo.equals("GET") ? null : "{}";
        assertThat(api.chamar(metodo, rota, null, corpo).status()).as("anônimo").isEqualTo(anonimo);
        Map<String, Integer> esperado = Map.of("SOLICITANTE", solicitante, "RESPONSAVEL", responsavel,
            "ADMINISTRADOR", administrador, "PROFESSOR", professor);
        esperado.forEach((perfil, status) ->
            assertThat(api.chamar(metodo, rota, api.token(EMAIL.get(perfil)), corpo).status()).as(perfil).isEqualTo(status));
    }
}
