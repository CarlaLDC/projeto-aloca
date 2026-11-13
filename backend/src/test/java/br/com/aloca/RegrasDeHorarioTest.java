package br.com.aloca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.aloca.servico.RegraNegocioException;
import br.com.aloca.servico.RegrasDeHorario;
import br.com.aloca.servico.RegrasDeHorario.Periodo;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

/**
 * Testes unitários (JUnit 5, sem banco e sem Spring) das regras de horário: validação do pedido,
 * sobreposição, horário de funcionamento, janelas livres e sugestões de horário.
 * Cada tabela cobre os ramos da regra, incluindo as bordas (horários iguais e consecutivos).
 */
@DisplayName("Regras de horário")
class RegrasDeHorarioTest {

    private static LocalTime h(String hora) {
        return LocalTime.parse(hora);
    }

    private static Periodo p(String inicio, String fim) {
        return new Periodo(h(inicio), h(fim));
    }

    // ------------------------------------------------------------------ validação do pedido (RF-05)

    @Nested
    @DisplayName("Validação do horário pedido")
    class ValidacaoDoPedido {

        private static final LocalDateTime AGORA = LocalDateTime.of(2026, 11, 10, 14, 30, 45);

        @ParameterizedTest(name = "[{index}] {0} {1}–{2} → \"{3}\"")
        @CsvSource(delimiter = '|', textBlock = """
            2026-11-11 | 10:00 | 10:00 | O término deve ser posterior ao início.
            2026-11-11 | 10:00 | 09:00 | O término deve ser posterior ao início.
            2026-11-11 | 00:00 | 00:00 | O término deve ser posterior ao início.
            2026-11-09 | 10:00 | 11:00 | Escolha a data de hoje ou uma data futura.
            2025-11-10 | 15:00 | 16:00 | Escolha a data de hoje ou uma data futura.
            2026-11-10 | 14:29 | 15:00 | O horário de início já passou. Escolha um horário futuro.
            2026-11-10 | 08:00 | 09:00 | O horário de início já passou. Escolha um horário futuro.
            """)
        void recusaHorariosInvalidos(LocalDate data, LocalTime inicio, LocalTime fim, String mensagem) {
            assertThatThrownBy(() -> RegrasDeHorario.validarPedido(data, inicio, fim, AGORA))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessage(mensagem);
        }

        @ParameterizedTest(name = "[{index}] {0} {1}–{2} é aceito")
        @CsvSource(delimiter = '|', textBlock = """
            2026-11-10 | 14:30 | 15:00
            2026-11-10 | 14:31 | 14:32
            2026-11-10 | 22:00 | 23:59
            2026-11-11 | 00:00 | 00:01
            2027-01-01 | 07:00 | 22:00
            """)
        void aceitaHorariosValidos(LocalDate data, LocalTime inicio, LocalTime fim) {
            assertThatCode(() -> RegrasDeHorario.validarPedido(data, inicio, fim, AGORA)).doesNotThrowAnyException();
        }
    }

    // ------------------------------------------------------------------ sobreposição (RF-05)

    @Nested
    @DisplayName("Sobreposição entre reservas")
    class Sobreposicao {

        /** Reserva existente: 09:00–11:00. */
        private static final Periodo EXISTENTE = p("09:00", "11:00");

        @ParameterizedTest(name = "[{index}] {0}–{1}: {3}")
        @CsvSource(delimiter = '|', textBlock = """
            09:00 | 11:00 | true  | mesmo horário
            09:30 | 10:30 | true  | dentro da existente
            08:00 | 12:00 | true  | engloba a existente
            08:00 | 09:01 | true  | encosta 1 minuto no início
            10:59 | 12:00 | true  | encosta 1 minuto no fim
            10:00 | 12:00 | true  | começa no meio
            08:00 | 10:00 | true  | termina no meio
            11:00 | 13:00 | false | consecutiva depois (fim = início)
            07:00 | 09:00 | false | consecutiva antes (fim = início)
            12:00 | 13:00 | false | totalmente depois
            06:00 | 08:00 | false | totalmente antes
            """)
        void detectaSobreposicao(LocalTime inicio, LocalTime fim, boolean esperado, String caso) {
            Periodo pedido = new Periodo(inicio, fim);
            assertThat(RegrasDeHorario.sobrepoe(pedido, EXISTENTE)).isEqualTo(esperado);
            // A regra é simétrica: a ordem dos períodos não muda o resultado.
            assertThat(RegrasDeHorario.sobrepoe(EXISTENTE, pedido)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "[{index}] {0}–{1} sobre 09:00–11:00 = {2} min")
        @CsvSource({
            "09:00, 11:00, 120",
            "10:00, 12:00, 60",
            "08:00, 09:30, 30",
            "09:15, 09:45, 30",
            "07:00, 13:00, 120",
            "11:00, 12:00, 0",
            "07:00, 09:00, 0",
            "13:00, 14:00, 0"
        })
        void calculaMinutosSobrepostos(LocalTime inicio, LocalTime fim, long minutos) {
            assertThat(RegrasDeHorario.minutosSobrepostos(new Periodo(inicio, fim), EXISTENTE)).isEqualTo(minutos);
        }
    }

    // ------------------------------------------------------------------ funcionamento e instantes

    @Nested
    @DisplayName("Horário de funcionamento")
    class Funcionamento {

        @ParameterizedTest(name = "[{index}] {0}–{1} no recurso 07:00–22:00 → {2}")
        @CsvSource({
            "07:00, 22:00, true",
            "07:00, 08:00, true",
            "21:00, 22:00, true",
            "06:59, 08:00, false",
            "21:00, 22:01, false",
            "05:00, 06:00, false",
            "22:00, 23:00, false"
        })
        void verificaSeCabeNoFuncionamento(LocalTime inicio, LocalTime fim, boolean esperado) {
            assertThat(RegrasDeHorario.dentroDoFuncionamento(new Periodo(inicio, fim), h("07:00"), h("22:00")))
                .isEqualTo(esperado);
        }

        @ParameterizedTest(name = "[{index}] às {0}, a reserva 09:00–11:00 está em andamento? {1}")
        @CsvSource({"08:59, false", "09:00, true", "10:30, true", "10:59, true", "11:00, false"})
        void verificaSeOInstanteEstaNoPeriodo(LocalTime instante, boolean esperado) {
            assertThat(RegrasDeHorario.contem(p("09:00", "11:00"), instante)).isEqualTo(esperado);
        }

        @ParameterizedTest(name = "[{index}] bloqueio {0}–{1} cobre o dia 07:00–22:00? {2}")
        @CsvSource({
            "00:00, 23:59, true",
            "07:00, 22:00, true",
            "07:01, 22:00, false",
            "07:00, 21:59, false",
            "10:00, 12:00, false"
        })
        void verificaSeOBloqueioCobreODiaInteiro(LocalTime inicio, LocalTime fim, boolean esperado) {
            assertThat(RegrasDeHorario.cobre(new Periodo(inicio, fim), h("07:00"), h("22:00"))).isEqualTo(esperado);
        }
    }

    // ------------------------------------------------------------------ janelas livres (RF-03)

    @Nested
    @DisplayName("Janelas livres do dia")
    class JanelasLivres {

        static Stream<Arguments> casos() {
            return Stream.of(
                Arguments.of("dia sem reservas", "07:00", "", "07:00-22:00"),
                Arguments.of("reserva no meio divide o dia", "07:00", "09:00-11:00", "07:00-09:00 11:00-22:00"),
                Arguments.of("reserva logo na abertura", "07:00", "07:00-08:00", "08:00-22:00"),
                Arguments.of("reserva até o fechamento", "07:00", "20:00-22:00", "07:00-20:00"),
                Arguments.of("reservas consecutivas viram um bloco só", "07:00", "09:00-10:00 10:00-11:00", "07:00-09:00 11:00-22:00"),
                Arguments.of("reservas sobrepostas (ex.: sala e material)", "07:00", "09:00-11:00 10:00-12:00", "07:00-09:00 12:00-22:00"),
                Arguments.of("ocupações fora de ordem", "07:00", "15:00-16:00 09:00-10:00", "07:00-09:00 10:00-15:00 16:00-22:00"),
                Arguments.of("intervalo menor que 30 min é descartado", "07:00", "07:20-12:00 12:20-22:00", ""),
                Arguments.of("intervalo de exatos 30 min é mantido", "07:00", "07:30-21:30", "07:00-07:30 21:30-22:00"),
                Arguments.of("busca começa agora (dia de hoje)", "14:05", "09:00-11:00", "14:05-22:00"),
                Arguments.of("busca começa dentro de uma reserva", "10:00", "09:00-11:00", "11:00-22:00"),
                Arguments.of("manutenção o dia inteiro", "07:00", "00:00-23:59", ""),
                Arguments.of("dia que já passou (busca no fechamento)", "22:00", "", "")
            );
        }

        @ParameterizedTest(name = "[{index}] {0}")
        @MethodSource("casos")
        void calculaJanelasLivres(String caso, String inicioBusca, String ocupacoes, String esperado) {
            List<Periodo> livres = RegrasDeHorario.janelasLivres(h(inicioBusca), h("22:00"), periodos(ocupacoes),
                Duration.ofMinutes(30));
            assertThat(livres).containsExactlyElementsOf(periodos(esperado));
        }

        private static List<Periodo> periodos(String texto) {
            if (texto.isBlank()) return List.of();
            return Arrays.stream(texto.trim().split("\\s+"))
                .map(par -> par.split("-"))
                .map(par -> p(par[0], par[1]))
                .toList();
        }
    }

    // ------------------------------------------------------------------ sugestões de horário (tela 05)

    @Nested
    @DisplayName("Horários alternativos para sugerir")
    class HorariosAlternativos {

        @Test
        void ordenaDoMaisProximoParaOMaisDistanteSemRepetirOPedido() {
            List<LocalTime> horarios = RegrasDeHorario.horariosAlternativos(h("07:00"), h("12:00"), Duration.ofHours(2),
                Duration.ofMinutes(60), h("09:00"));
            assertThat(horarios).containsExactly(h("08:00"), h("10:00"), h("07:00"));
        }

        @ParameterizedTest(name = "[{index}] funcionamento {0}–{1}, duração {2} min → último início {3}")
        @CsvSource({
            "07:00, 22:00, 120, 20:00",
            "07:00, 22:00, 30, 21:30",
            "08:00, 21:00, 60, 20:00",
            // Recurso aberto até 23:59: a sugestão não pode terminar no dia seguinte.
            "07:00, 23:59, 120, 21:30"
        })
        void respeitaOFechamentoESemPassarDaMeiaNoite(LocalTime abertura, LocalTime fechamento, long duracaoMin,
                                                     LocalTime ultimoInicio) {
            List<LocalTime> horarios = RegrasDeHorario.horariosAlternativos(abertura, fechamento,
                Duration.ofMinutes(duracaoMin), Duration.ofMinutes(30), h("03:00"));
            assertThat(horarios).isNotEmpty().allSatisfy(t -> {
                assertThat(t).isAfterOrEqualTo(abertura);
                assertThat(t.plusMinutes(duracaoMin)).isAfter(t).isBeforeOrEqualTo(fechamento);
            });
            assertThat(horarios.stream().max(LocalTime::compareTo)).contains(ultimoInicio);
        }

        @ParameterizedTest(name = "[{index}] duração {0} min, passo {1} min → nenhuma sugestão")
        @CsvSource({"0, 30", "60, 0", "-30, 30", "960, 30"})
        void semSugestaoQuandoNaoCabeOuOsParametrosSaoInvalidos(long duracaoMin, long passoMin) {
            assertThat(RegrasDeHorario.horariosAlternativos(h("07:00"), h("22:00"), Duration.ofMinutes(duracaoMin),
                Duration.ofMinutes(passoMin), h("09:00"))).isEmpty();
        }
    }

    // ------------------------------------------------------------------ arredondamento

    @ParameterizedTest(name = "[{index}] {0} arredondado para cima de 5 em 5 min = {1}")
    @CsvSource({
        "09:00:00, 09:00",
        "09:00:30, 09:05",
        "09:01:00, 09:05",
        "09:04:59, 09:05",
        "09:05:00, 09:05",
        "09:56:00, 10:00",
        "23:55:00, 23:55",
        "23:57:00, 23:59:59"
    })
    void arredondaParaOProximoMultiplo(LocalTime hora, LocalTime esperado) {
        assertThat(RegrasDeHorario.arredondarParaCima(hora, 5)).isEqualTo(esperado);
    }
}
