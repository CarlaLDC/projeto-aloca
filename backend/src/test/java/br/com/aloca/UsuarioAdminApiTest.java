package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Complementa o UsuariosApiTest (cadastro, validação, desativação, senha e revogação de tokens) com o que
 * a gestão de usuários ganhou na integração: consultar, excluir conta sem histórico e ligar a conta ao
 * cadastro de professor.
 */
@DisplayName("Gestão de usuários: consulta, exclusão e vínculo com professor")
class UsuarioAdminApiTest extends ApiTesteBase {

    private static final String USUARIOS = "/api/admin/usuarios";

    private static String emailNovo() {
        return "teste." + UUID.randomUUID().toString().substring(0, 8) + "@aloca.edu.br";
    }

    private static String cadastro(String email, String perfis, Long professorId) {
        return """
            {"nome":"Pessoa de Teste","email":"%s","unidade":"Testes","perfis":[%s],"senha":"senha-segura-1","professorId":%s}
            """.formatted(email, perfis, professorId);
    }

    private Integer criar(String email, String perfis, Long professorId) throws Exception {
        String corpo = postJson(token(PATRICIA), USUARIOS, cadastro(email, perfis, professorId))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        return JsonPath.read(corpo, "$.id");
    }

    private ResultActions excluir(String token, Object id) throws Exception {
        return mvc.perform(delete(USUARIOS + "/" + id).header("Authorization", "Bearer " + token));
    }

    private String tokenDe(String email) throws Exception {
        return JsonPath.read(mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"senha-segura-1\"}".formatted(email)))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(), "$.token");
    }

    @Test
    @DisplayName("CRUD completo: criar, consultar, alterar e excluir conta sem histórico")
    void crudCompletoDeUsuario() throws Exception {
        String patricia = token(PATRICIA);
        String email = emailNovo();
        Integer id = criar(email, "\"SOLICITANTE\"", null);

        get(patricia, USUARIOS + "/" + id).andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(email))
            .andExpect(jsonPath("$.perfis", contains("SOLICITANTE")))
            .andExpect(jsonPath("$.ativo").value(true));

        mvc.perform(put(USUARIOS + "/" + id).header("Authorization", "Bearer " + patricia).contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"Pessoa de Teste\",\"email\":\"%s\",\"unidade\":\"Testes\",\"perfis\":[\"SOLICITANTE\",\"RESPONSAVEL\"]}".formatted(email)))
            .andExpect(status().isOk());
        get(patricia, USUARIOS + "/" + id).andExpect(jsonPath("$.perfis", containsInAnyOrder("SOLICITANTE", "RESPONSAVEL")));

        excluir(patricia, id).andExpect(status().isNoContent());
        get(patricia, USUARIOS + "/" + id).andExpect(status().isNotFound());
        get(patricia, USUARIOS).andExpect(jsonPath("$[*].email", not(hasItem(email))));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"senha-segura-1\"}".formatted(email)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("Conta com histórico não é excluída: a mensagem orienta desativar para manter a auditoria")
    void contaComHistoricoNaoEhExcluida() throws Exception {
        String email = emailNovo();
        Integer id = criar(email, "\"SOLICITANTE\"", null);
        codigoCriado(tokenDe(email), reserva(diaLivre(), "09:00", "10:00", SALA_108, null));

        excluir(token(PATRICIA), id).andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem", allOf(containsString("1 reservas solicitadas"), containsString("Desative a conta"))));
        get(token(PATRICIA), USUARIOS + "/" + id).andExpect(status().isOk());
    }

    @Test
    @DisplayName("Exclusão recusada para a própria conta, conta inexistente e quem não é administrador")
    void exclusaoProtegida() throws Exception {
        String patricia = token(PATRICIA);
        Integer propria = JsonPath.read(get(patricia, "/api/auth/me").andReturn().getResponse().getContentAsString(), "$.id");
        excluir(patricia, propria).andExpect(status().isConflict())
            .andExpect(jsonPath("$.mensagem").value("Você não pode excluir a própria conta."));
        excluir(patricia, 999999).andExpect(status().isNotFound());

        Integer outro = criar(emailNovo(), "\"SOLICITANTE\"", null);
        for (String email : new String[] {ANA, ROBERTO, ANDRE}) {
            excluir(token(email), outro).andExpect(status().isForbidden());
            get(token(email), USUARIOS + "/" + outro).andExpect(status().isForbidden());
        }
    }

    @Test
    @DisplayName("Conta de professor ligada ao cadastro; vínculo duplicado ou sem perfil Professor é recusado")
    void vinculoComCadastroDeProfessor() throws Exception {
        String patricia = token(PATRICIA);
        Integer professorId = JsonPath.read(postJson(patricia, "/api/admin/professores",
            "{\"nome\":\"Paula Reis\",\"competencia\":\"História\"}").andReturn().getResponse().getContentAsString(), "$.id");

        postJson(patricia, USUARIOS, cadastro(emailNovo(), "\"SOLICITANTE\"", professorId.longValue()))
            .andExpect(status().isBadRequest());
        Integer id = criar(emailNovo(), "\"PROFESSOR\"", professorId.longValue());
        get(patricia, USUARIOS + "/" + id).andExpect(jsonPath("$.professor").value("Paula Reis"));
        postJson(patricia, USUARIOS, cadastro(emailNovo(), "\"PROFESSOR\"", professorId.longValue()))
            .andExpect(status().isConflict()).andExpect(jsonPath("$.mensagem", containsString("já está ligado")));
        postJson(patricia, USUARIOS, cadastro(emailNovo(), "\"PROFESSOR\"", 999999L)).andExpect(status().isNotFound());
    }
}
