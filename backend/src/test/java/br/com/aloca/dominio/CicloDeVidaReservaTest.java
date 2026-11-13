package br.com.aloca.dominio;

import static br.com.aloca.dominio.StatusReserva.*;
import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes unitários (sem Spring e sem banco) das regras de estado da reserva:
 * SOLICITADA → APROVADA → EM_USO → CONCLUÍDA, com os desvios REJEITADA, CANCELADA e NÃO COMPARECEU.
 */
@DisplayName("Ciclo de vida da reserva (regras puras)")
class CicloDeVidaReservaTest {

    /** Matriz esperada: para cada estado, os únicos estados para onde ele pode ir. */
    private static final Map<StatusReserva, Set<StatusReserva>> ESPERADO = Map.of(
        SOLICITADA, EnumSet.of(SOLICITADA, APROVADA, REJEITADA, CANCELADA),
        APROVADA, EnumSet.of(APROVADA, SOLICITADA, EM_USO, CONCLUIDA, CANCELADA, NAO_COMPARECEU),
        EM_USO, EnumSet.of(CONCLUIDA, NAO_COMPARECEU),
        CONCLUIDA, EnumSet.noneOf(StatusReserva.class),
        REJEITADA, EnumSet.noneOf(StatusReserva.class),
        CANCELADA, EnumSet.noneOf(StatusReserva.class),
        NAO_COMPARECEU, EnumSet.noneOf(StatusReserva.class));

    static Stream<Arguments> todosOsPares() {
        return Stream.of(StatusReserva.values()).flatMap(de -> Stream.of(StatusReserva.values())
            .map(para -> Arguments.of(de, para, ESPERADO.get(de).contains(para))));
    }

    @ParameterizedTest(name = "{0} → {1}: permitida = {2}")
    @MethodSource("todosOsPares")
    @DisplayName("Matriz completa de transições (7 × 7 = 49 pares)")
    void matrizDeTransicoes(StatusReserva de, StatusReserva para, boolean permitida) {
        assertThat(CicloDeVidaReserva.permitida(de, para)).isEqualTo(permitida);
    }

    @ParameterizedTest(name = "{0} é estado final")
    @EnumSource(value = StatusReserva.class, names = {"REJEITADA", "CANCELADA", "NAO_COMPARECEU", "CONCLUIDA"})
    void estadosFinaisNaoMudamMais(StatusReserva status) {
        assertThat(CicloDeVidaReserva.finalizado(status)).isTrue();
        assertThat(CicloDeVidaReserva.destinos(status)).isEmpty();
    }

    @ParameterizedTest(name = "{0} não é estado final")
    @EnumSource(value = StatusReserva.class, names = {"SOLICITADA", "APROVADA", "EM_USO"})
    void estadosQueAindaPodemMudar(StatusReserva status) {
        assertThat(CicloDeVidaReserva.finalizado(status)).isFalse();
    }

    @Nested
    @DisplayName("Ciclo automático pelo relógio (reserva das 09:00 às 11:00)")
    class PeloRelogio {
        private final LocalDateTime inicio = LocalDateTime.of(2030, 3, 12, 9, 0);
        private final LocalDateTime fim = LocalDateTime.of(2030, 3, 12, 11, 0);

        @ParameterizedTest(name = "{0} às {1} → {2}")
        @CsvSource({
            "APROVADA,   08:59, ",
            "APROVADA,   09:00, EM_USO",
            "APROVADA,   10:59, EM_USO",
            "APROVADA,   11:00, CONCLUIDA",
            "EM_USO,     10:00, ",
            "EM_USO,     11:00, CONCLUIDA",
            "SOLICITADA, 09:30, ",
            "SOLICITADA, 12:00, ",
            "CONCLUIDA,  12:00, ",
            "CANCELADA,  09:30, ",
            "REJEITADA,  09:30, ",
            "NAO_COMPARECEU, 12:00, "
        })
        void proximoEstado(StatusReserva atual, String hora, StatusReserva esperado) {
            LocalDateTime agora = LocalDateTime.of(2030, 3, 12, Integer.parseInt(hora.substring(0, 2)), Integer.parseInt(hora.substring(3)));
            assertThat(CicloDeVidaReserva.proximoPeloRelogio(atual, agora, inicio, fim)).isEqualTo(Optional.ofNullable(esperado));
        }
    }

    @Nested
    @DisplayName("Alterar ou cancelar (reserva começa às 09:00)")
    class Gerenciar {
        private final LocalDateTime inicio = LocalDateTime.of(2030, 3, 12, 9, 0);

        @ParameterizedTest(name = "{0} às {1}: {2}")
        @CsvSource(delimiter = '|', value = {
            "SOLICITADA     | 08:59 | ",
            "APROVADA       | 08:59 | ",
            "APROVADA       | 09:00 | Esta reserva já está em uso e permanece no histórico.",
            "SOLICITADA     | 10:00 | Esta reserva já está em uso e permanece no histórico.",
            "EM_USO         | 08:00 | Reservas com status \"Em uso\" não podem ser alteradas nem canceladas.",
            "CONCLUIDA      | 08:00 | Reservas com status \"Concluída\" não podem ser alteradas nem canceladas.",
            "CANCELADA      | 08:00 | Reservas com status \"Cancelada\" não podem ser alteradas nem canceladas.",
            "REJEITADA      | 08:00 | Reservas com status \"Rejeitada\" não podem ser alteradas nem canceladas.",
            "NAO_COMPARECEU | 08:00 | Reservas com status \"Não compareceu\" não podem ser alteradas nem canceladas."
        })
        void impedimento(StatusReserva status, String hora, String mensagem) {
            LocalDateTime agora = inicio.withHour(Integer.parseInt(hora.substring(0, 2))).withMinute(Integer.parseInt(hora.substring(3)));
            assertThat(CicloDeVidaReserva.impedimentoParaGerenciar(status, agora, inicio)).isEqualTo(Optional.ofNullable(mensagem));
        }
    }

    @Nested
    @DisplayName("Não compareceu (reserva das 09:00 às 11:00 de 12/03: de 09:15 até antes das 11:00)")
    class NaoCompareceu {
        private final LocalDateTime inicio = LocalDateTime.of(2030, 3, 12, 9, 0);
        private final LocalDateTime fim = LocalDateTime.of(2030, 3, 12, 11, 0);

        @ParameterizedTest(name = "{0} em {1}: {2}")
        @CsvSource(delimiter = '|', value = {
            "APROVADA       | 2030-03-12T09:15 | ",
            "EM_USO         | 2030-03-12T10:30 | ",
            "EM_USO         | 2030-03-12T10:59 | ",
            "APROVADA       | 2030-03-12T09:14 | A ausência pode ser registrada somente de 15 minutos após o início até o fim da reserva.",
            "APROVADA       | 2030-03-12T08:59 | A ausência pode ser registrada somente de 15 minutos após o início até o fim da reserva.",
            "EM_USO         | 2030-03-12T11:00 | A ausência pode ser registrada somente de 15 minutos após o início até o fim da reserva.",
            "CONCLUIDA      | 2030-03-12T11:30 | Somente reservas aprovadas ou em uso admitem não comparecimento.",
            "SOLICITADA     | 2030-03-12T09:30 | Somente reservas aprovadas ou em uso admitem não comparecimento.",
            "CANCELADA      | 2030-03-12T09:30 | Somente reservas aprovadas ou em uso admitem não comparecimento.",
            "REJEITADA      | 2030-03-12T09:30 | Somente reservas aprovadas ou em uso admitem não comparecimento.",
            "NAO_COMPARECEU | 2030-03-12T09:30 | Somente reservas aprovadas ou em uso admitem não comparecimento."
        })
        void impedimento(StatusReserva status, LocalDateTime agora, String mensagem) {
            assertThat(CicloDeVidaReserva.impedimentoParaNaoComparecimento(status, agora, inicio, fim))
                .isEqualTo(Optional.ofNullable(mensagem));
        }
    }
}
