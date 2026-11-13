package br.com.aloca;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import com.jayway.jsonpath.JsonPath;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

class UsuariosApiTest extends ApiTesteBase {
    private String email() { return "gestao-"+UUID.randomUUID()+"@aloca.edu.br"; }
    private String cadastro(String email,String perfil) {
        return """
            {"nome":"Usuário de teste","email":"%s","unidade":"Testes","perfis":["%s"],"senha":"Teste-12345"}
            """.formatted(email,perfil);
    }
    private org.springframework.test.web.servlet.ResultActions anonLogin(String corpo) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(corpo));
    }
    private String login(String email,String senha) throws Exception {
        return JsonPath.read(anonLogin("{\"email\":\"%s\",\"senha\":\"%s\"}".formatted(email,senha))
            .andExpect(status().isOk()).andReturn().getResponse().getContentAsString(),"$.token");
    }
    private long criarUsuario(String admin,String email,String perfil) throws Exception {
        String json=postJson(admin,"/api/admin/usuarios",cadastro(email,perfil)).andExpect(status().isCreated())
            .andExpect(jsonPath("$.senha").doesNotExist()).andExpect(jsonPath("$.senhaHash").doesNotExist())
            .andReturn().getResponse().getContentAsString();
        return ((Number)JsonPath.read(json,"$.id")).longValue();
    }
    @Test void apenasAdminGerenciaUsuariosEValidaCadastro() throws Exception {
        String admin=token(PATRICIA), email=email();
        get(token(ANA),"/api/admin/usuarios").andExpect(status().isForbidden());
        postJson(token(ANDRE),"/api/admin/usuarios",cadastro(email,"ADMINISTRADOR")).andExpect(status().isForbidden());
        long id=criarUsuario(admin,email,"SOLICITANTE");
        postJson(admin,"/api/admin/usuarios",cadastro(email.toUpperCase(),"SOLICITANTE")).andExpect(status().isConflict());
        postJson(admin,"/api/admin/usuarios",cadastro(email(),"SOLICITANTE").replace("Teste-12345","curta")).andExpect(status().isBadRequest());
        postJson(admin,"/api/admin/usuarios",cadastro(email(),"SOLICITANTE").replace("[\"SOLICITANTE\"]","[]")).andExpect(status().isBadRequest());
        get(admin,"/api/admin/usuarios").andExpect(jsonPath("$[?(@.id == %d)].email".formatted(id),hasItem(email)));
    }
    @Test void desativacaoETrocaDeSenhaRevogamTokensAnteriores() throws Exception {
        String admin=token(PATRICIA),email=email();long id=criarUsuario(admin,email,"SOLICITANTE");
        String antigo=login(email,"Teste-12345");
        mvc.perform(patch("/api/admin/usuarios/"+id+"/ativo").header("Authorization","Bearer "+admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}")).andExpect(status().isOk());
        get(antigo,"/api/reservas/opcoes").andExpect(status().isUnauthorized());
        anonLogin("{\"email\":\""+email+"\",\"senha\":\"Teste-12345\"}").andExpect(status().isUnauthorized());
        mvc.perform(patch("/api/admin/usuarios/"+id+"/ativo").header("Authorization","Bearer "+admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":true}")).andExpect(status().isOk());
        String reativado=login(email,"Teste-12345");
        mvc.perform(put("/api/admin/usuarios/"+id+"/senha").header("Authorization","Bearer "+admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"senha\":\"Outra-12345\"}")).andExpect(status().isNoContent());
        get(reativado,"/api/reservas/opcoes").andExpect(status().isUnauthorized());
        get(login(email,"Outra-12345"),"/api/reservas/opcoes").andExpect(status().isOk());
    }
    @Test void editarPerfisRevogaAcessoAdminSemExporSenha() throws Exception {
        String admin=token(PATRICIA),email=email();long id=criarUsuario(admin,email,"ADMINISTRADOR");
        String anterior=login(email,"Teste-12345");
        mvc.perform(put("/api/admin/usuarios/"+id).header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Editado\",\"email\":\""+email+"\",\"unidade\":\"Outra\",\"perfis\":[\"PROFESSOR\"]}"))
            .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Editado"));
        get(anterior,"/api/admin/usuarios").andExpect(status().isUnauthorized());
        get(login(email,"Teste-12345"),"/api/admin/usuarios").andExpect(status().isForbidden());
    }
    @Test void adminNaoRemoveSeuProprioAcesso() throws Exception {
        String admin=token(PATRICIA);Number id=JsonPath.read(get(admin,"/api/auth/me").andReturn().getResponse().getContentAsString(),"$.id");
        mvc.perform(patch("/api/admin/usuarios/"+id+"/ativo").header("Authorization","Bearer "+admin)
            .contentType(MediaType.APPLICATION_JSON).content("{\"ativo\":false}")).andExpect(status().isConflict());
        mvc.perform(put("/api/admin/usuarios/"+id).header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON)
            .content("{\"nome\":\"Admin\",\"email\":\""+PATRICIA+"\",\"unidade\":\"Testes\",\"perfis\":[\"SOLICITANTE\"]}"))
            .andExpect(status().isConflict());
        get(admin,"/api/admin/usuarios").andExpect(status().isOk());
    }
    @Test void professorCriaAlteraECancelaSuaReservaMasNaoAprovaNemAlteraADeOutro() throws Exception {
        String andre=token(ANDRE),ana=token(ANA);var dia=diaLivre();
        String codigo=codigoCriado(andre,reserva(dia,"09:00","10:00",SALA_204,null));
        mvc.perform(put("/api/reservas/"+codigo).header("Authorization","Bearer "+andre).contentType(MediaType.APPLICATION_JSON)
            .content(reserva(dia,"10:00","11:00",SALA_204,null))).andExpect(status().isOk());
        postJson(andre,"/api/reservas/"+codigo+"/cancelamento","{\"motivo\":\"Reagendamento\"}").andExpect(status().isOk());
        String alheia=codigoCriado(ana,reserva(diaLivre(),"09:00","10:00",LAB_02,null));
        postJson(andre,"/api/reservas/"+alheia+"/cancelamento","{\"motivo\":\"Teste\"}").andExpect(status().isForbidden());
        postJson(andre,"/api/aprovacoes/"+alheia+"/aprovar",null).andExpect(status().isForbidden());
    }
}
