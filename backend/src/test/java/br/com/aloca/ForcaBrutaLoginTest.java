package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import br.com.aloca.seguranca.ProtecaoForcaBruta;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;

/** Login real pela API: bloqueio após 5 senhas erradas (RNF-03). */
class ForcaBrutaLoginTest extends ApiTesteBase {

    @Autowired ProtecaoForcaBruta protecao;

    @AfterEach
    void liberarTodos() {
        // Os outros testes usam os mesmos usuários: nenhum pode ficar bloqueado.
        protecao.limpar();
    }

    private ResultActions login(String email, String senha) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email, senha)));
    }

    @Test
    void cincoSenhasErradasBloqueiamOEmailInclusiveParaASenhaCerta() throws Exception {
        for (int i = 1; i <= 4; i++) {
            login(MARIANA, "errada-" + i).andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
        }
        login(MARIANA, "errada-5")
            .andExpect(status().isTooManyRequests())
            .andExpect(header().string("Retry-After", "900"))
            .andExpect(jsonPath("$.mensagem").value("Muitas tentativas de login. Tente novamente em 15 minutos."));

        login(MARIANA, "aloca123").andExpect(status().isTooManyRequests());
        // Outra pessoa continua entrando normalmente.
        login(ROBERTO, "aloca123").andExpect(status().isOk());
    }

    @Test
    void loginCertoAntesDoLimiteZeraAContagem() throws Exception {
        for (int i = 0; i < 4; i++) login(MARIANA, "errada").andExpect(status().isUnauthorized());
        login(MARIANA, "aloca123").andExpect(status().isOk());
        for (int i = 0; i < 4; i++) login(MARIANA, "errada").andExpect(status().isUnauthorized());
        login(MARIANA, "aloca123").andExpect(status().isOk());
    }

    @Test
    void emailInexistenteTemOMesmoTratamentoParaNaoRevelarContas() throws Exception {
        String inexistente = "ninguem.aqui@aloca.edu.br";
        for (int i = 0; i < 4; i++) {
            login(inexistente, "qualquer").andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.mensagem").value("E-mail ou senha incorretos."));
        }
        login(inexistente, "qualquer").andExpect(status().isTooManyRequests())
            .andExpect(jsonPath("$.mensagem", containsString("Muitas tentativas")));
    }

    @Test
    void maiusculasNoEmailNaoBurlamOBloqueio() throws Exception {
        for (int i = 0; i < 5; i++) login(i % 2 == 0 ? MARIANA : MARIANA.toUpperCase(), "errada");
        login(MARIANA, "aloca123").andExpect(status().isTooManyRequests());
    }
}
