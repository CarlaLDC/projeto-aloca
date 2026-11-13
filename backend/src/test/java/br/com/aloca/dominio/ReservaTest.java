package br.com.aloca.dominio;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

/** Testes unitários da entidade Reserva: toda mudança de estado gera um registro de auditoria. */
@DisplayName("Reserva: mudança de estado com auditoria (unitário)")
class ReservaTest {

    private static final LocalDateTime CRIADA = LocalDateTime.of(2030, 3, 1, 10, 0);
    private final Usuario ana = new Usuario("Ana Martins", "ana@aloca.edu.br", "x", "U", Set.of(Perfil.SOLICITANTE));
    private final Usuario roberto = new Usuario("Roberto Almeida", "roberto@aloca.edu.br", "x", "U", Set.of(Perfil.RESPONSAVEL));

    private Reserva nova() {
        Recurso sala = new Recurso("Sala 204", TipoRecurso.SALA, null, "Bloco A", 40, true, LocalTime.of(7, 0), LocalTime.of(22, 0));
        return new Reserva("Aula", LocalDate.of(2030, 3, 12), LocalTime.of(9, 0), LocalTime.of(11, 0), sala, null, List.of(), ana, CRIADA);
    }

    @Test
    @DisplayName("Uma reserva nova começa como Solicitada")
    void comecaSolicitada() {
        assertThat(nova().getStatus()).isEqualTo(StatusReserva.SOLICITADA);
    }

    @Test
    @DisplayName("Ciclo completo SOLICITADA → APROVADA → EM_USO → CONCLUÍDA registra cada passo, em ordem")
    void cicloCompletoAuditado() {
        Reserva r = nova();
        r.mudarStatus(StatusReserva.APROVADA, "Roberto Almeida", roberto, "Aprovada", "Alocação validada", CRIADA.plusHours(1));
        r.mudarStatus(StatusReserva.EM_USO, "Sistema", null, "Em uso", "Período iniciado", LocalDateTime.of(2030, 3, 12, 9, 0));
        r.mudarStatus(StatusReserva.CONCLUIDA, "Sistema", null, "Concluída", "Período encerrado", LocalDateTime.of(2030, 3, 12, 11, 0));

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONCLUIDA);
        assertThat(r.getHistorico()).extracting(HistoricoReserva::getEstadoAnterior, HistoricoReserva::getEstadoNovo, HistoricoReserva::getAtor)
            .containsExactly(
                org.assertj.core.groups.Tuple.tuple(StatusReserva.SOLICITADA, StatusReserva.APROVADA, "Roberto Almeida"),
                org.assertj.core.groups.Tuple.tuple(StatusReserva.APROVADA, StatusReserva.EM_USO, "Sistema"),
                org.assertj.core.groups.Tuple.tuple(StatusReserva.EM_USO, StatusReserva.CONCLUIDA, "Sistema"));
        assertThat(r.getHistorico()).extracting(HistoricoReserva::getDataHora).isSorted();
    }

    @ParameterizedTest(name = "{0} → {1} é recusada e não deixa rastro")
    @CsvSource({
        "SOLICITADA, EM_USO", "SOLICITADA, CONCLUIDA", "SOLICITADA, NAO_COMPARECEU",
        "APROVADA, REJEITADA", "CANCELADA, APROVADA", "REJEITADA, APROVADA", "CONCLUIDA, EM_USO", "CONCLUIDA, NAO_COMPARECEU", "NAO_COMPARECEU, CONCLUIDA"
    })
    void transicaoInvalidaEhRecusada(StatusReserva de, StatusReserva para) {
        Reserva r = nova();
        levarAte(r, de);
        int eventos = r.getHistorico().size();

        assertThatThrownBy(() -> r.mudarStatus(para, "X", null, "X", "X", CRIADA))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining(de.rotulo()).hasMessageContaining(para.rotulo());
        assertThat(r.getStatus()).isEqualTo(de);
        assertThat(r.getHistorico()).hasSize(eventos);
    }

    /** Leva a reserva até o estado pedido por um caminho válido. */
    private void levarAte(Reserva r, StatusReserva alvo) {
        switch (alvo) {
            case SOLICITADA -> { }
            case APROVADA -> r.mudarStatus(StatusReserva.APROVADA, "R", roberto, "Aprovada", "", CRIADA);
            case REJEITADA -> r.mudarStatus(StatusReserva.REJEITADA, "R", roberto, "Rejeitada", "", CRIADA);
            case CANCELADA -> r.mudarStatus(StatusReserva.CANCELADA, "A", ana, "Cancelada", "", CRIADA);
            case EM_USO -> { levarAte(r, StatusReserva.APROVADA); r.mudarStatus(StatusReserva.EM_USO, "S", null, "Em uso", "", CRIADA); }
            case CONCLUIDA -> { levarAte(r, StatusReserva.EM_USO); r.mudarStatus(StatusReserva.CONCLUIDA, "S", null, "Concluída", "", CRIADA); }
            case NAO_COMPARECEU -> { levarAte(r, StatusReserva.APROVADA); r.mudarStatus(StatusReserva.NAO_COMPARECEU, "R", roberto, "Não compareceu", "", CRIADA); }
        }
    }
}
