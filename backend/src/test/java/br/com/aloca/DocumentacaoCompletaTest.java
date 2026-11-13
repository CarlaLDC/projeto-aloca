package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;

import com.jayway.jsonpath.JsonPath;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.mvc.method.RequestMappingInfo;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Todo endpoint da API aparece na documentação, com resumo e grupo (tela), e as respostas
 * importantes estão descritas. Se alguém criar um endpoint sem documentar, este teste falha.
 */
class DocumentacaoCompletaTest extends ApiTesteBase {

    @Autowired
    @Qualifier("requestMappingHandlerMapping")
    RequestMappingHandlerMapping mapeamentos;

    private String especificacao() throws Exception {
        return mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/v3/api-docs"))
            .andReturn().getResponse().getContentAsString();
    }

    @Test
    void todoEndpointDaApiEstaDocumentadoComResumoEGrupo() throws Exception {
        String doc = especificacao();
        List<String> faltando = new ArrayList<>();
        int total = 0;
        for (Map.Entry<RequestMappingInfo, HandlerMethod> e : mapeamentos.getHandlerMethods().entrySet()) {
            for (String caminho : e.getKey().getPathPatternsCondition().getPatternValues()) {
                if (!caminho.startsWith("/api/")) continue;
                for (var metodo : e.getKey().getMethodsCondition().getMethods()) {
                    total++;
                    String base = "$.paths['" + caminho + "']." + metodo.name().toLowerCase();
                    if (ler(doc, base) == null || ler(doc, base + ".summary") == null || ler(doc, base + ".tags") == null) {
                        faltando.add(metodo + " " + caminho);
                    }
                }
            }
        }
        assertThat(total).isGreaterThanOrEqualTo(36);
        assertThat(faltando).as("endpoints sem documentação").isEmpty();
    }

    @Test
    void loginNaoExigeTokenNaDocumentacao() throws Exception {
        List<?> seguranca = JsonPath.read(especificacao(), "$.paths['/api/auth/login'].post.security");
        assertThat(seguranca).isEmpty();
    }

    @Test
    void criacaoDeReservaDocumentaOConflitoComAlternativas() throws Exception {
        String doc = especificacao();
        String base = "$.paths['/api/reservas'].post.responses";
        List<String> criada = JsonPath.read(doc, base + "['201'].content.*.schema.$ref");
        List<String> conflito = JsonPath.read(doc, base + "['409'].content.*.schema.$ref");
        assertThat(criada).isNotEmpty().allMatch(ref -> ref.endsWith("/ReservaDetalhe"));
        assertThat(conflito).isNotEmpty().allMatch(ref -> ref.endsWith("/ConflitoResposta"));
        Map<String, Object> campos = JsonPath.read(doc, "$.components.schemas.ConflitoResposta.properties");
        assertThat(campos).containsKeys("conflitos", "alternativas");
    }

    private static Object ler(String doc, String caminho) {
        try {
            return JsonPath.read(doc, caminho);
        } catch (com.jayway.jsonpath.PathNotFoundException e) {
            return null;
        }
    }
}
