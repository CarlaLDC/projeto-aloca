package br.com.aloca.http;

import static org.assertj.core.api.Assertions.*;

import br.com.aloca.PostgresTestcontainer;
import com.jayway.jsonpath.*;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;

/**
 * Caixa-preta: só contratos HTTP/JSON. Nenhum service, repository ou entidade
 * da aplicação é chamado para preparar dados ou verificar resultados.
 */
@Tag("http")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Import(PostgresTestcontainer.class)
class RegrasCriticasHttpTest {
    @Value("${local.server.port}") private int porta;
    private final HttpClient cliente = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private String ana, mariana, responsavel, admin, professor;
    private static final String DIA = LocalDate.now(ZoneId.of("America/Sao_Paulo")).plusDays(180).toString();

    private record Resposta(int status, String corpo, HttpHeaders headers) {
        DocumentContext json() { return JsonPath.parse(corpo); }
        String texto(String campo) { return json().read("$." + campo, String.class); }
        long numero(String campo) { return ((Number) json().read("$." + campo)).longValue(); }
    }

    private Resposta enviar(String metodo, String caminho, String token, Object dados) throws Exception {
        String corpo = dados == null ? "" : dados instanceof String s ? s :
            Configuration.defaultConfiguration().jsonProvider().toJson(dados);
        HttpRequest.Builder req = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + porta + caminho))
            .timeout(Duration.ofSeconds(25)).header("Content-Type", "application/json");
        if (token != null) req.header("Authorization", "Bearer " + token);
        HttpResponse<String> r = cliente.send(req.method(metodo,
            corpo.isEmpty() ? HttpRequest.BodyPublishers.noBody() :
                HttpRequest.BodyPublishers.ofString(corpo, StandardCharsets.UTF_8)).build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        return new Resposta(r.statusCode(), r.body(), r.headers());
    }

    private Resposta esperar(String metodo, String caminho, String token, Object dados, int status) throws Exception {
        Resposta r = enviar(metodo, caminho, token, dados);
        assertThat(r.status()).as("%s %s: %s", metodo, caminho, r.corpo()).isEqualTo(status);
        return r;
    }

    private void erroSeguro(Resposta r) {
        assertThat(r.headers().firstValue("content-type").orElse("")).contains("application/json");
        assertThat(r.texto("mensagem")).isNotBlank();
        assertThat(r.corpo()).doesNotContain("stackTrace", "org.springframework", "java.lang.", "senhaHash");
    }

    private String login(String email) throws Exception {
        return esperar("POST", "/api/auth/login", null,
            Map.of("email", email, "senha", "aloca123"), 200).texto("token");
    }

    @BeforeEach
    void autenticar() throws Exception {
        ana = login("ana.martins@aloca.edu.br");
        mariana = login("mariana.costa@aloca.edu.br");
        responsavel = login("roberto.almeida@aloca.edu.br");
        admin = login("patricia.souza@aloca.edu.br");
        professor = login("andre.lima@aloca.edu.br");
    }

    private String token(String perfil) {
        return switch (perfil) {
            case "solicitante" -> ana;
            case "responsavel" -> responsavel;
            case "administrador" -> admin;
            case "professor" -> professor;
            default -> throw new IllegalArgumentException(perfil);
        };
    }

    private long recurso(String tipo, boolean restrito) throws Exception {
        return esperar("POST", "/api/admin/recursos", admin, Map.of(
            "nome", "HTTP " + UUID.randomUUID(), "tipo", tipo, "localizacao", "Teste HTTP",
            "capacidade", 20, "restrito", restrito, "abertura", "07:00", "fechamento", "22:00"), 201).numero("id");
    }

    private long docente() throws Exception {
        return esperar("POST", "/api/admin/professores", admin,
            Map.of("nome", "HTTP docente " + UUID.randomUUID(), "competencia", "Testes"), 201).numero("id");
    }

    private Map<String, Object> pedido(long sala, Long docente, List<Long> materiais) {
        Map<String, Object> p = new LinkedHashMap<>();
        p.put("atividade", "HTTP " + UUID.randomUUID());
        p.put("data", DIA); p.put("inicio", "10:00"); p.put("fim", "11:00");
        p.put("espacoId", sala); p.put("professorId", docente); p.put("materialIds", materiais);
        return p;
    }

    private Resposta criar(Map<String, Object> p) throws Exception {
        return esperar("POST", "/api/reservas", ana, p, 201);
    }

    private Resposta detalhe(String codigo) throws Exception {
        return esperar("GET", "/api/reservas/" + codigo, ana, null, 200);
    }

    private List<Map<String, Object>> minhas() throws Exception {
        return esperar("GET", "/api/reservas/minhas", ana, null, 200).json().read("$");
    }

    private long ocorrencias(Object atividade) throws Exception {
        return minhas().stream().filter(r -> atividade.equals(r.get("atividade"))).count();
    }

    @ParameterizedTest
    @NullSource
    @ValueSource(strings = {"token-invalido"})
    void semTokenValidoNaoAcessaApi(String token) throws Exception {
        erroSeguro(esperar("GET", "/api/reservas/minhas", token, null, 401));
    }

    @Test
    void loginNaoRevelaSeContaExiste() throws Exception {
        Resposta existente = esperar("POST", "/api/auth/login", null,
            Map.of("email", "ana.martins@aloca.edu.br", "senha", "errada"), 401);
        Resposta inexistente = esperar("POST", "/api/auth/login", null,
            Map.of("email", "inexistente@aloca.edu.br", "senha", "errada"), 401);
        erroSeguro(existente); erroSeguro(inexistente);
        assertThat(existente.texto("mensagem")).isEqualTo(inexistente.texto("mensagem"));
    }

    @ParameterizedTest(name = "{0} em {1} → {2}")
    @CsvSource({
        "solicitante,/api/admin/recursos,403",
        "responsavel,/api/admin/recursos,403",
        "professor,/api/admin/recursos,403",
        "administrador,/api/aprovacoes,403",
        "solicitante,/api/movimentacoes,403",
        "solicitante,/api/relatorios,403",
        "professor,/api/reservas/opcoes,200"
    })
    void aplicaMatrizDePerfisNasRotasPublicas(String perfil, String caminho, int status) throws Exception {
        Resposta r = esperar("GET", caminho, token(perfil), null, status);
        if (status == 403) erroSeguro(r);
    }

    @ParameterizedTest
    @ValueSource(strings = {"igual", "invertido", "passado", "atividadeVazia", "semSala", "jsonMalformado"})
    void entradaInvalidaNaoCriaReserva(String caso) throws Exception {
        Map<String, Object> p = pedido(recurso("SALA", false), null, List.of());
        Object atividadeOriginal = p.get("atividade");
        switch (caso) {
            case "igual" -> p.put("fim", "10:00");
            case "invertido" -> p.put("fim", "09:00");
            case "passado" -> p.put("data", LocalDate.now(ZoneId.of("America/Sao_Paulo")).minusDays(1).toString());
            case "atividadeVazia" -> p.put("atividade", "");
            case "semSala" -> p.remove("espacoId");
            case "jsonMalformado" -> { }
            default -> throw new IllegalArgumentException(caso);
        }
        long antes = minhas().size();
        Resposta r = esperar("POST", "/api/reservas", ana, caso.equals("jsonMalformado") ? "{invalido" : p, 400);
        erroSeguro(r);
        assertThat(ocorrencias(atividadeOriginal)).isZero();
        assertThat(minhas()).hasSize((int) antes);
    }

    @ParameterizedTest
    @ValueSource(strings = {"Sala", "Material", "Professor"})
    void sobreposicaoRecusadaMasConsecutividadeEOutroDiaPermitidos(String alvo) throws Exception {
        long sala = recurso("SALA", false);
        long outraSala = alvo.equals("Sala") ? sala : recurso("SALA", false);
        Long docente = alvo.equals("Professor") ? docente() : null;
        List<Long> materiais = alvo.equals("Material") ? List.of(recurso("EQUIPAMENTO", false)) : List.of();
        Map<String, Object> original = pedido(sala, docente, materiais);
        criar(original);
        Map<String, Object> disputa = pedido(outraSala, docente, materiais);
        disputa.put("inicio", "10:30"); disputa.put("fim", "11:30");
        Resposta conflito = esperar("POST", "/api/reservas", ana, disputa, 409);
        List<Map<String, Object>> conflitos = conflito.json().read("$.conflitos");
        assertThat(conflitos).hasSize(1);
        assertThat(conflitos.getFirst().get("tipo")).isEqualTo(alvo);
        assertThat(((Number) conflitos.getFirst().get("sobreposicaoMinutos")).intValue()).isEqualTo(30);
        assertThat(ocorrencias(disputa.get("atividade"))).isZero();
        disputa.put("inicio", "11:00"); disputa.put("fim", "12:00");
        criar(disputa);
        disputa.put("atividade", "Outro dia " + UUID.randomUUID());
        disputa.put("data", LocalDate.parse(DIA).plusDays(1).toString());
        criar(disputa);
    }

    @ParameterizedTest(name = "HTTP simultâneo disputa {0}")
    @ValueSource(strings = {"Sala", "Material", "Professor"})
    void doisPedidosHttpSimultaneosAceitamUmaUnicaReserva(String alvo) throws Exception {
        long sala = recurso("SALA", false);
        long segundaSala = alvo.equals("Sala") ? sala : recurso("SALA", false);
        Long docente = alvo.equals("Professor") ? docente() : null;
        List<Long> materiais = alvo.equals("Material") ? List.of(recurso("EQUIPAMENTO", false)) : List.of();
        Map<String, Object> a = pedido(sala, docente, materiais);
        Map<String, Object> b = pedido(segundaSala, docente, materiais);
        CyclicBarrier largada = new CyclicBarrier(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            List<Future<Resposta>> futuros = new ArrayList<>();
            for (Map<String, Object> p : List.of(a, b)) {
                futuros.add(executor.submit(() -> {
                    largada.await(10, TimeUnit.SECONDS);
                    return enviar("POST", "/api/reservas", ana, p);
                }));
            }
            List<Resposta> respostas = new ArrayList<>();
            for (Future<Resposta> f : futuros) respostas.add(f.get(30, TimeUnit.SECONDS));
            assertThat(respostas).extracting(Resposta::status).containsExactlyInAnyOrder(201, 409);
            assertThat(ocorrencias(a.get("atividade")) + ocorrencias(b.get("atividade"))).isEqualTo(1);
            Resposta aceita = respostas.stream().filter(r -> r.status() == 201).findFirst().orElseThrow();
            assertThat(detalhe(aceita.texto("codigo")).texto("status")).isEqualTo("APROVADA");
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"solicitante", "administrador"})
    void perfilNaoAutorizadoNaoAprovaNemAlteraHistorico(String perfil) throws Exception {
        String codigo = criar(pedido(recurso("SALA", true), null, List.of())).texto("codigo");
        List<?> antes = detalhe(codigo).json().read("$.historico");
        erroSeguro(esperar("POST", "/api/aprovacoes/" + codigo + "/aprovar", token(perfil), null, 403));
        Resposta depois = detalhe(codigo);
        assertThat(depois.texto("status")).isEqualTo("SOLICITADA");
        assertThat((List<?>) depois.json().read("$.historico")).isEqualTo(antes);
    }

    @Test
    void aprovacaoRestritaRegistraDecisaoENotificacaoEImpedeSegundaDecisao() throws Exception {
        String codigo = criar(pedido(recurso("SALA", true), null, List.of())).texto("codigo");
        assertThat(detalhe(codigo).texto("status")).isEqualTo("SOLICITADA");
        esperar("POST", "/api/aprovacoes/" + codigo + "/aprovar", responsavel, null, 200);
        assertThat(detalhe(codigo).texto("status")).isEqualTo("APROVADA");
        List<Map<String, Object>> historico = detalhe(codigo).json().read("$.historico");
        assertThat(historico).anySatisfy(e -> {
            assertThat(e.get("estadoAnterior")).isEqualTo("Solicitada");
            assertThat(e.get("estadoNovo")).isEqualTo("Aprovada");
            assertThat(e.get("ator")).isEqualTo("Roberto Almeida");
            assertThat(e.get("dataHora")).isNotNull();
        });
        esperar("POST", "/api/aprovacoes/" + codigo + "/aprovar", responsavel, null, 409);
        List<Map<String, Object>> avisos = esperar("GET", "/api/notificacoes", ana, null, 200).json().read("$.itens");
        assertThat(avisos).anySatisfy(n -> assertThat(n.get("mensagem").toString()).contains(codigo, "aprovada"));
    }

    @Test
    void rejeicaoExigeJustificativaELiberaAgenda() throws Exception {
        Map<String, Object> p = pedido(recurso("SALA", true), null, List.of());
        String codigo = criar(p).texto("codigo");
        esperar("POST", "/api/aprovacoes/" + codigo + "/rejeitar", responsavel, Map.of("justificativa", ""), 400);
        assertThat(detalhe(codigo).texto("status")).isEqualTo("SOLICITADA");
        esperar("POST", "/api/aprovacoes/" + codigo + "/rejeitar", responsavel, Map.of("justificativa", "Recurso indisponível"), 200);
        assertThat(detalhe(codigo).texto("status")).isEqualTo("REJEITADA");
        assertThat(detalhe(codigo).texto("motivoRejeicao")).isEqualTo("Recurso indisponível");
        p.put("atividade", "Após rejeição " + UUID.randomUUID());
        criar(p);
    }

    private long bloquear(long sala) throws Exception {
        return esperar("POST", "/api/admin/bloqueios", admin, Map.of("recursoId", sala,
            "inicio", DIA + "T09:00:00", "fim", DIA + "T12:00:00", "motivo", "Manutenção HTTP"), 201).numero("id");
    }

    @Test
    void manutencaoBloqueiaCriacaoERemocaoDoBloqueioLibera() throws Exception {
        long sala = recurso("SALA", false);
        long bloqueio = bloquear(sala);
        Map<String, Object> p = pedido(sala, null, List.of());
        Resposta conflito = esperar("POST", "/api/reservas", ana, p, 409);
        assertThat(conflito.texto("conflitos[0].tipo")).isEqualTo("Manutenção");
        assertThat(ocorrencias(p.get("atividade"))).isZero();
        esperar("DELETE", "/api/admin/bloqueios/" + bloqueio, admin, null, 204);
        criar(p);
    }

    @Test
    void manutencaoPosteriorImpedeAprovacaoSemMudarEstadoOuHistorico() throws Exception {
        long sala = recurso("SALA", true);
        String codigo = criar(pedido(sala, null, List.of())).texto("codigo");
        List<?> antes = detalhe(codigo).json().read("$.historico");
        bloquear(sala);
        Resposta resposta = esperar("POST", "/api/aprovacoes/" + codigo + "/aprovar", responsavel, null, 409);
        assertThat(resposta.texto("mensagem")).containsIgnoringCase("manutenção");
        assertThat(detalhe(codigo).texto("status")).isEqualTo("SOLICITADA");
        assertThat((List<?>) detalhe(codigo).json().read("$.historico")).isEqualTo(antes);
    }

    @Test
    void somenteDonoAlteraECancelaEHistoricoPermaneceConsultavel() throws Exception {
        Map<String, Object> p = pedido(recurso("SALA", false), null, List.of());
        String codigo = criar(p).texto("codigo");
        esperar("GET", "/api/reservas/" + codigo, mariana, null, 403);
        p.put("inicio", "12:00"); p.put("fim", "13:00");
        esperar("PUT", "/api/reservas/" + codigo, mariana, p, 403);
        esperar("POST", "/api/reservas/" + codigo + "/cancelamento", mariana, Map.of("motivo", "Teste"), 403);
        esperar("PUT", "/api/reservas/" + codigo, ana, p, 200);
        assertThat(detalhe(codigo).texto("inicio")).isEqualTo("12:00:00");
        esperar("POST", "/api/reservas/" + codigo + "/cancelamento", ana, Map.of("motivo", ""), 400);
        esperar("POST", "/api/reservas/" + codigo + "/cancelamento", ana, Map.of("motivo", "Remarcada"), 200);
        assertThat(detalhe(codigo).texto("status")).isEqualTo("CANCELADA");
        List<Map<String, Object>> historico = detalhe(codigo).json().read("$.historico");
        assertThat(historico).extracting(e -> e.get("acao")).contains("Alterada", "Cancelada");
        p.put("atividade", "Após cancelamento " + UUID.randomUUID());
        criar(p);
    }

    @Test
    void retiradaEDevolucaoProtegemReservaEmUsoEPreservamAuditoria() throws Exception {
        Map<String, Object> p = pedido(recurso("SALA", false), null, List.of(recurso("EQUIPAMENTO", false)));
        String codigo = criar(p).texto("codigo");
        List<Map<String, Object>> movimentos = esperar("GET", "/api/movimentacoes", responsavel, null, 200)
            .json().read("$.movimentacoes");
        String mov = movimentos.stream().filter(m -> codigo.equals(m.get("reserva"))).findFirst().orElseThrow().get("codigo").toString();
        esperar("POST", "/api/movimentacoes/" + mov + "/retirada", ana, null, 403);
        esperar("POST", "/api/movimentacoes/" + mov + "/devolucao", responsavel, null, 409);
        assertThat(esperar("POST", "/api/movimentacoes/" + mov + "/retirada", responsavel, null, 200).texto("status")).isEqualTo("EM_USO");
        esperar("POST", "/api/movimentacoes/" + mov + "/retirada", responsavel, null, 409);
        List<?> antes = detalhe(codigo).json().read("$.historico");
        p.put("atividade", "Tentativa de alterar material retirado");
        esperar("PUT", "/api/reservas/" + codigo, ana, p, 409);
        esperar("POST", "/api/reservas/" + codigo + "/cancelamento", ana, Map.of("motivo", "Teste"), 409);
        assertThat((List<?>) detalhe(codigo).json().read("$.historico")).isEqualTo(antes);
        Resposta devolucao = esperar("POST", "/api/movimentacoes/" + mov + "/devolucao", responsavel, null, 200);
        assertThat(devolucao.texto("status")).isEqualTo("DEVOLVIDO");
        assertThat(devolucao.texto("protocolo")).startsWith("DEV-");
        esperar("POST", "/api/movimentacoes/" + mov + "/devolucao", responsavel, null, 409);
        List<Map<String, Object>> historico = detalhe(codigo).json().read("$.historico");
        assertThat(historico).extracting(e -> e.get("acao")).contains("Retirada", "Devolução");
    }

    @Test
    void relatorioMostraHorasReservasEConflitosDoRecurso() throws Exception {
        long sala = recurso("SALA", false);
        Map<String, Object> p = pedido(sala, null, List.of());
        criar(p);
        esperar("POST", "/api/reservas", ana, p, 409);
        // Conflitos são contabilizados na data da tentativa; horas na data da reserva.
        String inicioRelatorio = LocalDate.now(ZoneId.of("America/Sao_Paulo")).toString();
        Resposta relatorio = esperar("GET", "/api/relatorios?inicio=" + inicioRelatorio + "&fim=" + DIA, responsavel, null, 200);
        List<Map<String, Object>> linhas = relatorio.json().read("$.recursos");
        Map<String, Object> linha = linhas.stream().filter(l -> ((Number) l.get("id")).longValue() == sala).findFirst().orElseThrow();
        assertThat(((Number) linha.get("reservas")).longValue()).isEqualTo(1);
        assertThat(((Number) linha.get("horas")).doubleValue()).isEqualTo(1.0);
        assertThat(((Number) linha.get("conflitos")).longValue()).isEqualTo(1);
        Resposta csv = esperar("GET", "/api/relatorios/csv?inicio=" + inicioRelatorio + "&fim=" + DIA, admin, null, 200);
        assertThat(csv.headers().firstValue("content-type").orElse("")).contains("text/csv");
        assertThat(csv.corpo()).contains(linha.get("nome").toString());
    }

    @Test
    void reservaInexistenteRetornaErroSeguro() throws Exception {
        erroSeguro(esperar("GET", "/api/reservas/RES-HTTP-INEXISTENTE", ana, null, 404));
    }
}
