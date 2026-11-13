package br.com.aloca.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.aloca.api.dto.ReservaDtos.ReservaRequest;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import br.com.aloca.servico.*;
import java.time.*;
import java.util.*;
import java.util.stream.Stream;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;

@Tag("unitario")
class ReservaServiceUnitTest {
    private ReservaRepositorio reservas;
    private RecursoRepositorio recursos;
    private ProfessorRepositorio professores;
    private BloqueioRepositorio bloqueios;
    private ConflitoRegistradoRepositorio conflitos;
    private ApplicationEventPublisher eventos;
    private NotificacaoService notificacoes;
    private Mapeador mapeador;

    @BeforeEach
    void preparar() {
        reservas = mock(ReservaRepositorio.class);
        recursos = mock(RecursoRepositorio.class);
        professores = mock(ProfessorRepositorio.class);
        bloqueios = mock(BloqueioRepositorio.class);
        conflitos = mock(ConflitoRegistradoRepositorio.class);
        eventos = mock(ApplicationEventPublisher.class);
        notificacoes = mock(NotificacaoService.class);
        mapeador = mock(Mapeador.class);
    }

    private ReservaService servico(LocalDateTime agora) {
        return new ReservaService(reservas, recursos, professores, bloqueios, conflitos, eventos,
            notificacoes, mapeador, Cenarios.relogio(agora));
    }

    static Stream<Arguments> pedidosInvalidos() {
        return Stream.of(
            Arguments.of(Cenarios.DIA, "10:00", "10:00", List.of(), "O término deve ser posterior ao início."),
            Arguments.of(Cenarios.DIA, "11:00", "09:00", List.of(), "O término deve ser posterior ao início."),
            Arguments.of(Cenarios.DIA.minusDays(1), "10:00", "11:00", List.of(), "Escolha a data de hoje ou uma data futura."),
            Arguments.of(Cenarios.DIA, "08:59", "11:00", List.of(), "O horário de início já passou. Escolha um horário futuro."),
            Arguments.of(Cenarios.DIA, "10:00", "11:00", List.of(8L, 8L), "O mesmo material foi selecionado mais de uma vez.")
        );
    }

    @ParameterizedTest(name = "[{index}] pedido inválido: {4}")
    @MethodSource("pedidosInvalidos")
    void recusaPedidoInvalidoAntesDeAcessarPersistencia(LocalDate data, String inicio, String fim,
            List<Long> materiais, String mensagem) {
        ReservaRequest pedido = new ReservaRequest("Aula", data, LocalTime.parse(inicio), LocalTime.parse(fim),
            1L, null, materiais);
        assertThatThrownBy(() -> servico(Cenarios.DIA.atTime(9, 0)).criar(pedido, Cenarios.ANA))
            .isInstanceOfSatisfying(RegraNegocioException.class, erro -> {
                assertThat(erro.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                assertThat(erro.getMessage()).isEqualTo(mensagem);
            });
        verifyNoInteractions(reservas, recursos, professores, bloqueios, conflitos, eventos, notificacoes, mapeador);
    }

    @ParameterizedTest(name = "{0} às {1} → {2}, mudanças={3}")
    @CsvSource({
        "APROVADA,09:59:59,APROVADA,0",
        "APROVADA,10:00:00,EM_USO,1",
        "APROVADA,10:30:00,EM_USO,1",
        "APROVADA,11:00:00,CONCLUIDA,1",
        "APROVADA,11:00:01,CONCLUIDA,1",
        "EM_USO,10:30:00,EM_USO,0",
        "EM_USO,11:00:00,CONCLUIDA,1"
    })
    void cicloTemporalRespeitaFronteirasERegistraSomenteMudancas(StatusReserva inicial, String hora,
            StatusReserva esperado, int quantidade) {
        Reserva r = Cenarios.reserva(inicial);
        int historicosAntes = r.getHistorico().size();
        when(reservas.ateData(eq(Cenarios.DIA), any())).thenReturn(List.of(r));
        LocalDateTime agora = Cenarios.DIA.atTime(LocalTime.parse(hora));

        assertThat(servico(agora).atualizarCiclo()).isEqualTo(quantidade);
        assertThat(r.getStatus()).isEqualTo(esperado);
        assertThat(r.getHistorico()).hasSize(historicosAntes + quantidade);
        if (quantidade == 1) {
            HistoricoReserva evento = r.getHistorico().getLast();
            assertThat(evento.getEstadoAnterior()).isEqualTo(inicial);
            assertThat(evento.getEstadoNovo()).isEqualTo(esperado);
            assertThat(evento.getAtor()).isEqualTo("Sistema");
            assertThat(evento.getDataHora()).isEqualTo(agora);
        }
        verifyNoInteractions(eventos, notificacoes, mapeador);
    }

    @ParameterizedTest(name = "ausência às {0}, permitida={1}")
    @CsvSource({
        "10:14:59,false",
        "10:15:00,true",
        "10:59:59,true",
        "11:00:00,false",
        "11:00:01,false"
    })
    void ausenciaSoEhPermitidaNaJanelaDeQuinzeMinutosAteAntesDoFim(String hora, boolean permitida) {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        when(reservas.findByCodigo("RES-TESTE")).thenReturn(Optional.of(r));
        int antes = r.getHistorico().size();
        ReservaService servico = servico(Cenarios.DIA.atTime(LocalTime.parse(hora)));
        if (permitida) {
            servico.marcarNaoComparecimento("RES-TESTE", "Ausência confirmada", Cenarios.ROBERTO);
            assertThat(r.getStatus()).isEqualTo(StatusReserva.NAO_COMPARECEU);
            assertThat(r.getHistorico()).hasSize(antes + 2); // decisão e notificação simulada
            assertThat(r.getHistorico().get(antes).getEstadoAnterior()).isEqualTo(StatusReserva.APROVADA);
            verify(eventos).publishEvent(any(ReservaEventos.AntesDeModificar.class));
            verify(eventos).publishEvent(any(ReservaEventos.Cancelada.class));
        } else {
            assertThatThrownBy(() -> servico.marcarNaoComparecimento("RES-TESTE", "Ausência", Cenarios.ROBERTO))
                .isInstanceOfSatisfying(RegraNegocioException.class, erro ->
                    assertThat(erro.getStatus()).isEqualTo(HttpStatus.CONFLICT));
            assertThat(r.getStatus()).isEqualTo(StatusReserva.APROVADA);
            assertThat(r.getHistorico()).hasSize(antes);
            verifyNoInteractions(eventos, notificacoes, mapeador);
        }
    }

    @Test
    void solicitanteNaoPodeRegistrarAusenciaMesmoDentroDaJanela() {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        when(reservas.findByCodigo("RES-TESTE")).thenReturn(Optional.of(r));
        int antes = r.getHistorico().size();
        assertThatThrownBy(() -> servico(Cenarios.DIA.atTime(10, 30))
            .marcarNaoComparecimento("RES-TESTE", "Ausência", Cenarios.ANA))
            .isInstanceOfSatisfying(RegraNegocioException.class, erro ->
                assertThat(erro.getStatus()).isEqualTo(HttpStatus.FORBIDDEN));
        assertThat(r.getStatus()).isEqualTo(StatusReserva.APROVADA);
        assertThat(r.getHistorico()).hasSize(antes);
        verifyNoInteractions(eventos, notificacoes, mapeador);
    }

    @Test
    void vetoDeMaterialEmUsoImpedeAusenciaSemMudarEstadoOuAuditoria() {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        when(reservas.findByCodigo("RES-TESTE")).thenReturn(Optional.of(r));
        int antes = r.getHistorico().size();
        doThrow(RegraNegocioException.conflito("Material já retirado"))
            .when(eventos).publishEvent(any(ReservaEventos.AntesDeModificar.class));
        assertThatThrownBy(() -> servico(Cenarios.DIA.atTime(10, 30))
            .marcarNaoComparecimento("RES-TESTE", "Ausência", Cenarios.ROBERTO))
            .isInstanceOf(RegraNegocioException.class).hasMessage("Material já retirado");
        assertThat(r.getStatus()).isEqualTo(StatusReserva.APROVADA);
        assertThat(r.getHistorico()).hasSize(antes);
        verify(eventos, never()).publishEvent(any(ReservaEventos.Cancelada.class));
        verifyNoInteractions(notificacoes, mapeador);
    }
}
