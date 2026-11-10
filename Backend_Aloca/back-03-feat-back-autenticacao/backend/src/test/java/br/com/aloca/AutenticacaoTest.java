package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

/** Tela 01 · Entrar (RF-01, RNF-03). */
class AutenticacaoTest extends ApiTesteBase {

    @Test
    void loginDevolveTokenEDadosDoUsuario() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ROBERTO.ALMEIDA@aloca.edu.br\",\"senha\":\"aloca123\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.token").isString())
            .andExpect(jsonPath("$.usuario.nome").value("Roberto Almeida"))
            .andExpect(jsonPath("$.usuario.perfis", contains("RESPONSAVEL")));
    }

    @Test
    void loginRecusaSenhaErradaSemRevelarSeOEmailExiste() throws Exception {
        String esperado = "E-mail ou senha incorretos.";
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ana.martins@aloca.edu.br\",\"senha\":\"errada\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensagem").value(esperado));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"email\":\"ninguem@aloca.edu.br\",\"senha\":\"errada\"}"))
            .andExpect(status().isUnauthorized()).andExpect(jsonPath("$.mensagem").value(esperado));
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"invalido\",\"senha\":\"\"}"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void meDevolveOUsuarioDoToken() throws Exception {
        get(token(PATRICIA), "/api/auth/me")
            .andExpect(jsonPath("$.nome").value("Patrícia Souza"))
            .andExpect(jsonPath("$.perfis", contains("ADMINISTRADOR")))
            .andExpect(jsonPath("$.perfilRotulo").value("Administrador"))
            .andExpect(jsonPath("$.iniciais").value("PS"));
    }

    @Test
    void semTokenOuComTokenInvalidoARespostaEh401() throws Exception {
        mvc.perform(MockMvcRequestBuilders.get("/api/auth/me")).andExpect(status().isUnauthorized())
            .andExpect(jsonPath("$.mensagem", containsString("Entre novamente")));
        mvc.perform(MockMvcRequestBuilders.get("/api/auth/me").header("Authorization", "Bearer abc.def.ghi"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void recuperacaoDeSenhaOrientaProcurarOSuporte() throws Exception {
        mvc.perform(post("/api/auth/recuperar-senha").contentType(MediaType.APPLICATION_JSON).content("{}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.mensagem", containsString("suporte")));
    }
}
