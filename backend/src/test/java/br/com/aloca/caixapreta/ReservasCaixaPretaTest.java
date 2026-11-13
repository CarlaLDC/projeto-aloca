package br.com.aloca.caixapreta;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Caixa-preta · regras críticas de reserva, aprovação e manutenção (RF-04 a RF-08, RF-10, RF-11). */
@DisplayName("Caixa-preta: regras críticas de reserva")
class ReservasCaixaPretaTest extends CaixaPretaBase {

    @Test
    @DisplayName("Sala livre sem restrição: 201, aprovada na hora, histórico e notificação")
    void reservaAprovadaNaHora() {
        String ana = api.token(ANA);
        ClienteHttp.Resposta r = api.post("/api/reservas", ana, reserva(diaLivre(), "09:00", "11:00", SALA_108)).esperar(201);
        assertThat(r.texto("status")).isEqualTo("APROVADA");
        assertThat(r.texto("codigo")).matches("RES-\\d{4,}");
        assertThat(r.json().path("historico").toString()).contains("Reserva criada", "aprovada automaticamente");
        assertThat(api.get("/api/notificacoes", ana).corpo()).contains(r.texto("codigo"));
    }

    @Test
    @DisplayName("Sobreposição na mesma sala: 409 com o conflito e alternativas; horário encostado é aceito")
    void conflitoComAlternativas() {
        LocalDate dia = diaLivre();
        api.post("/api/reservas", api.token(MARIANA), reserva(dia, "09:00", "11:00", SALA_204)).esperar(201);
        ClienteHttp.Resposta conflito = api.post("/api/reservas", api.token(ANA), reserva(dia, "10:00", "12:00", SALA_204)).esperar(409);
        assertThat(conflito.json().path("conflitos").get(0).path("recurso").asString()).isEqualTo("Sala 204");
        assertThat(conflito.json().path("conflitos").get(0).path("sobreposicaoMinutos").asInt()).isEqualTo(60);
        assertThat(conflito.json().path("alternativas").size()).isPositive();
        api.post("/api/reservas", api.token(ANA), reserva(dia, "11:00", "12:00", SALA_204)).esperar(201);
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(delimiter = '|', value = {
        "término antes do início | 11:00 | 09:00 | 0    | posterior ao início",
        "duração zero            | 09:00 | 09:00 | 0    | posterior ao início",
        "data passada            | 09:00 | 10:00 | -400 | data de hoje ou uma data futura",
        "fora do funcionamento   | 05:00 | 06:00 | 0    | funciona das 07:00 às 22:00"
    })
    @DisplayName("Horários inválidos são recusados com 400 e mensagem clara")
    void horariosInvalidos(String caso, String inicio, String fim, int deslocamento, String mensagem) {
        LocalDate dia = deslocamento == 0 ? diaLivre() : LocalDate.now().plusDays(deslocamento);
        ClienteHttp.Resposta r = api.post("/api/reservas", api.token(ANA), reserva(dia, inicio, fim, SALA_108)).esperar(400);
        assertThat(r.texto("mensagem")).contains(mensagem);
    }

    @Test
    @DisplayName("Recurso restrito: fica Solicitada; só o Responsável aprova; rejeição exige justificativa")
    void aprovacaoDeRestrito() {
        String ana = api.token(ANA), roberto = api.token(ROBERTO);
        String aprovar = api.post("/api/reservas", ana, reserva(diaLivre(), "09:00", "11:00", AUDITORIO)).esperar(201).texto("codigo");
        String rejeitar = api.post("/api/reservas", ana, reserva(diaLivre(), "09:00", "11:00", AUDITORIO)).esperar(201).texto("codigo");
        assertThat(api.get("/api/reservas/" + aprovar, ana).texto("status")).isEqualTo("SOLICITADA");

        api.post("/api/aprovacoes/" + aprovar + "/aprovar", api.token(PATRICIA), null).esperar(403);
        api.post("/api/aprovacoes/" + aprovar + "/aprovar", roberto, null).esperar(200);
        api.post("/api/aprovacoes/" + aprovar + "/aprovar", roberto, null).esperar(409);
        assertThat(api.get("/api/reservas/" + aprovar, ana).texto("status")).isEqualTo("APROVADA");

        api.post("/api/aprovacoes/" + rejeitar + "/rejeitar", roberto, "{\"justificativa\":\"  \"}").esperar(400);
        ClienteHttp.Resposta rej = api.post("/api/aprovacoes/" + rejeitar + "/rejeitar", roberto, "{\"justificativa\":\"Evento institucional\"}").esperar(200);
        assertThat(rej.texto("status")).isEqualTo("REJEITADA");
        assertThat(rej.texto("motivoRejeicao")).isEqualTo("Evento institucional");
    }

    @Test
    @DisplayName("Alterar e cancelar: só o dono, cancelamento exige motivo e libera o horário")
    void alterarECancelar() {
        LocalDate dia = diaLivre();
        String ana = api.token(ANA), mariana = api.token(MARIANA);
        String codigo = api.post("/api/reservas", ana, reserva(dia, "09:00", "10:00", SALA_REUNIOES)).esperar(201).texto("codigo");

        api.put("/api/reservas/" + codigo, mariana, reserva(dia, "09:00", "11:00", SALA_REUNIOES)).esperar(403);
        api.post("/api/reservas/" + codigo + "/cancelamento", mariana, "{\"motivo\":\"x\"}").esperar(403);
        api.get("/api/reservas/" + codigo, mariana).esperar(403);

        ClienteHttp.Resposta alterada = api.put("/api/reservas/" + codigo, ana, reserva(dia, "09:00", "11:00", SALA_REUNIOES)).esperar(200);
        assertThat(alterada.texto("fim")).isEqualTo("11:00:00");

        api.post("/api/reservas/" + codigo + "/cancelamento", ana, "{\"motivo\":\"\"}").esperar(400);
        api.post("/api/reservas/" + codigo + "/cancelamento", ana, "{\"motivo\":\"Remarcada\"}").esperar(200);
        api.post("/api/reservas/" + codigo + "/cancelamento", ana, "{\"motivo\":\"De novo\"}").esperar(409);
        api.post("/api/reservas", mariana, reserva(dia, "09:00", "11:00", SALA_REUNIOES)).esperar(201);
    }

    @Test
    @DisplayName("Manutenção: recurso bloqueado não pode ser reservado; removido o bloqueio, volta a aceitar")
    void bloqueioDeManutencao() {
        LocalDate dia = diaLivre();
        String patricia = api.token(PATRICIA), ana = api.token(ANA);
        long bloqueio = api.post("/api/admin/bloqueios", patricia, "{\"recursoId\":%d,\"inicio\":\"%sT08:00:00\",\"fim\":\"%sT12:00:00\",\"motivo\":\"Pintura\"}"
            .formatted(LAB_03, dia, dia)).esperar(201).json().path("id").asLong();

        ClienteHttp.Resposta r = api.post("/api/reservas", ana, reserva(dia, "09:00", "10:00", LAB_03)).esperar(409);
        assertThat(r.texto("mensagem")).contains("manutenção");

        api.delete("/api/admin/bloqueios/" + bloqueio, patricia).esperar(204);
        api.post("/api/reservas", ana, reserva(dia, "09:00", "10:00", LAB_03)).esperar(201);
    }

    @Test
    @DisplayName("Professor também é solicitante: cria a própria reserva, mas não aprova nem administra")
    void professorReservaMasNaoAprova() {
        String andre = api.token(ANDRE);
        api.post("/api/reservas", andre, reserva(diaLivre(), "09:00", "10:00", SALA_108)).esperar(201);
        api.get("/api/aprovacoes", andre).esperar(403);
        api.get("/api/admin/usuarios", andre).esperar(403);
    }
}
