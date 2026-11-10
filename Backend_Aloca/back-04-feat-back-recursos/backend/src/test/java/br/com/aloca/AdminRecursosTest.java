package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;

/** Cadastro de recursos e professores (RF-02) e bloqueios de manutenção (RF-08). */
class AdminRecursosTest extends ApiTesteBase {

    @Test
    void adminCadastraAlteraEDesativaRecurso() throws Exception {
        String patricia = token(PATRICIA);
        String criado = postJson(patricia, "/api/admin/recursos", """
            {"nome":"Sala 301","tipo":"SALA","localizacao":"Bloco A · 3º andar","capacidade":30,"restrito":false,
             "abertura":"07:00","fechamento":"22:00"}""")
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(criado, "$.id");

        get(patricia, "/api/admin/recursos").andExpect(jsonPath("$[*].nome", hasItem("Sala 301")));
        postJson(patricia, "/api/admin/recursos/" + id + "/desativar", null).andExpect(jsonPath("$.ativo").value(false));
        postJson(patricia, "/api/admin/recursos/" + id + "/ativar", null).andExpect(jsonPath("$.ativo").value(true));
    }

    @Test
    void camposObrigatoriosEHorarioDoRecursoSaoValidados() throws Exception {
        String patricia = token(PATRICIA);
        postJson(patricia, "/api/admin/recursos", "{\"nome\":\"\"}")
            .andExpect(status().isBadRequest()).andExpect(jsonPath("$.campos.nome").exists());
        postJson(patricia, "/api/admin/recursos", """
            {"nome":"X","tipo":"SALA","localizacao":"Y","capacidade":10,"abertura":"22:00","fechamento":"07:00"}""")
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.mensagem", containsString("fechamento")));
    }

    @Test
    void adminCadastraProfessor() throws Exception {
        String patricia = token(PATRICIA);
        postJson(patricia, "/api/admin/professores", "{\"nome\":\"Carla Dias\",\"competencia\":\"Física\"}")
            .andExpect(status().isCreated()).andExpect(jsonPath("$.ativo").value(true));
        get(patricia, "/api/admin/professores").andExpect(jsonPath("$[*].nome", hasItem("Carla Dias")));
    }

    @Test
    void bloqueioExigePeriodoValido() throws Exception {
        String patricia = token(PATRICIA);
        var dia = diaLivre();
        String criado = postJson(patricia, "/api/admin/bloqueios", """
            {"recursoId":%d,"inicio":"%sT08:00:00","fim":"%sT12:00:00","motivo":"Pintura"}""".formatted(SALA_108, dia, dia))
            .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        postJson(patricia, "/api/admin/bloqueios", """
            {"recursoId":%d,"inicio":"%sT12:00:00","fim":"%sT08:00:00","motivo":"x"}""".formatted(SALA_108, dia, dia))
            .andExpect(status().isBadRequest());
        Integer id = JsonPath.read(criado, "$.id");
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete("/api/admin/bloqueios/" + id)
            .header("Authorization", "Bearer " + patricia)).andExpect(status().isNoContent());
    }

    @Test
    void somenteOAdministradorAcessaOsCadastros() throws Exception {
        get(token(ANA), "/api/admin/recursos").andExpect(status().isForbidden());
        get(token(ROBERTO), "/api/admin/recursos").andExpect(status().isForbidden());
        postJson(token(ANA), "/api/admin/bloqueios", "{}").andExpect(status().isForbidden());
    }
}
