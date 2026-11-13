package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/** Conta de professor criada pelo Administrador e ligada ao cadastro: vê a própria agenda (RF-14). */
class ContaDeProfessorTest extends ApiTesteBase {

    @Test
    void contaDeProfessorLigadaAoCadastroVeAPropriaAgenda() throws Exception {
        String patricia = token(PATRICIA);
        Integer professorId = JsonPath.read(postJson(patricia, "/api/admin/professores",
            "{\"nome\":\"Paula Reis\",\"competencia\":\"História\"}").andReturn().getResponse().getContentAsString(), "$.id");
        String email = "paula." + UUID.randomUUID().toString().substring(0, 8) + "@aloca.edu.br";
        postJson(patricia, "/api/admin/usuarios", """
            {"nome":"Paula Reis","email":"%s","unidade":"Testes","senha":"senha-segura-1","perfis":["PROFESSOR"],"professorId":%d}
            """.formatted(email, professorId)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.professor").value("Paula Reis"));

        String codigo = codigoCriado(token(ANA), reserva(diaLivre(), "09:00", "10:00", SALA_204, professorId.longValue()));

        String corpo = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"%s\",\"senha\":\"senha-segura-1\"}".formatted(email)))
            .andReturn().getResponse().getContentAsString();
        String token = JsonPath.read(corpo, "$.token");
        get(token, "/api/reservas/minhas").andExpect(jsonPath("$[*].codigo", contains(codigo)));
        get(token, "/api/reservas/" + codigo).andExpect(status().isOk());
    }
}
