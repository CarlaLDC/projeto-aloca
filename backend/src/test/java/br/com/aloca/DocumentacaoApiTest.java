package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

/** A documentação da API (RF-13) fica disponível sem login e descreve a autenticação por token. */
@SpringBootTest
@AutoConfigureMockMvc
@Import(PostgresTestcontainer.class)
class DocumentacaoApiTest {

    @Autowired MockMvc mvc;

    @Test
    void especificacaoOpenApiEstaDisponivelSemLogin() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.openapi", startsWith("3.")))
            .andExpect(jsonPath("$.info.title").value("Aloca · API de reservas"))
            .andExpect(jsonPath("$.components.securitySchemes.jwt.type").value("http"))
            .andExpect(jsonPath("$.components.securitySchemes.jwt.scheme").value("bearer"))
            .andExpect(jsonPath("$.security[0].jwt").isArray());
    }

    @Test
    void paginaDoSwaggerAbreSemLogin() throws Exception {
        mvc.perform(get("/swagger-ui.html")).andExpect(status().is3xxRedirection());
        mvc.perform(get("/swagger-ui/index.html")).andExpect(status().isOk());
    }
}
