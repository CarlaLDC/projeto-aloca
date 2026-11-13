package br.com.aloca.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.MovimentacaoRepositorio;
import br.com.aloca.servico.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.*;
import org.springframework.http.HttpStatus;

@Tag("unitario")
class MovimentacaoServiceUnitTest {
    private MovimentacaoRepositorio repositorio;
    private MovimentacaoService servico;
    private static final LocalDateTime AGORA = Cenarios.DIA.atTime(10, 30);

    @BeforeEach
    void preparar() {
        repositorio = mock(MovimentacaoRepositorio.class);
        servico = new MovimentacaoService(repositorio, Cenarios.relogio(AGORA));
    }

    private Movimentacao movimento(Reserva reserva) {
        Movimentacao m = new Movimentacao(reserva, Cenarios.recurso("Projetor", TipoRecurso.EQUIPAMENTO, false));
        m.setCodigo("MOV-TESTE");
        return m;
    }

    @ParameterizedTest(name = "retirada com reserva {0}")
    @EnumSource(StatusReserva.class)
    void retiradaExigeReservaAprovadaOuEmUso(StatusReserva status) {
        Reserva r = Cenarios.reserva(status);
        Movimentacao m = movimento(r);
        when(repositorio.findByCodigo("MOV-TESTE")).thenReturn(Optional.of(m));
        int antes = r.getHistorico().size();
        if (status == StatusReserva.APROVADA || status == StatusReserva.EM_USO) {
            servico.registrarRetirada("MOV-TESTE", Cenarios.ROBERTO);
            assertThat(m.getStatus()).isEqualTo(StatusMovimentacao.EM_USO);
            assertThat(m.getRetiradaEm()).isEqualTo(AGORA);
            assertThat(m.getRetiradaRegistradaPor()).isSameAs(Cenarios.ROBERTO);
            assertThat(r.getHistorico()).hasSize(antes + 1);
            assertThat(r.getHistorico().getLast().getAcao()).isEqualTo("Retirada");
            assertThat(r.getHistorico().getLast().getUsuario()).isSameAs(Cenarios.ROBERTO);
        } else {
            assertThatThrownBy(() -> servico.registrarRetirada("MOV-TESTE", Cenarios.ROBERTO))
                .isInstanceOfSatisfying(RegraNegocioException.class, erro ->
                    assertThat(erro.getStatus()).isEqualTo(HttpStatus.CONFLICT));
            assertThat(m.getStatus()).isEqualTo(StatusMovimentacao.AGUARDANDO_RETIRADA);
            assertThat(m.getRetiradaEm()).isNull();
            assertThat(r.getHistorico()).hasSize(antes);
        }
    }

    @ParameterizedTest(name = "consulta em {0}: {1}")
    @CsvSource({
        "10:59:59,EM_USO",
        "11:00:00,EM_USO",
        "11:00:01,DEVOLUCAO_PENDENTE"
    })
    void atrasoEhCalculadoSomenteAposDevolucaoPrevistaSemAlterarEstadoPersistivel(String hora, StatusMovimentacao esperado) {
        Movimentacao m = movimento(Cenarios.reserva(StatusReserva.APROVADA));
        m.registrarRetirada(Cenarios.ROBERTO, AGORA);
        assertThat(m.statusEm(Cenarios.DIA.atTime(LocalTime.parse(hora)))).isEqualTo(esperado);
        assertThat(m.getStatus()).isEqualTo(StatusMovimentacao.EM_USO);
    }

    @ParameterizedTest
    @EnumSource(value = StatusMovimentacao.class, names = {"DEVOLVIDO", "CANCELADA"})
    void movimentoEncerradoNaoViraPendenteMesmoDepoisDoPrazo(StatusMovimentacao estado) {
        Movimentacao m = movimento(Cenarios.reserva(StatusReserva.APROVADA));
        if (estado == StatusMovimentacao.DEVOLVIDO) {
            m.registrarRetirada(Cenarios.ROBERTO, AGORA);
            m.registrarDevolucao(Cenarios.ROBERTO, AGORA.plusMinutes(10), "DEV-TESTE");
        } else {
            m.cancelar();
        }
        assertThat(m.statusEm(Cenarios.DIA.plusDays(1).atStartOfDay())).isEqualTo(estado);
    }

    @ParameterizedTest
    @EnumSource(value = StatusMovimentacao.class, names = {"AGUARDANDO_RETIRADA", "DEVOLVIDO", "CANCELADA"})
    void devolucaoNaoPodeSerRegistradaSeMaterialNaoEstaEmUso(StatusMovimentacao estado) {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        Movimentacao m = movimento(r);
        if (estado == StatusMovimentacao.DEVOLVIDO) {
            m.registrarRetirada(Cenarios.ROBERTO, AGORA.minusMinutes(10));
            m.registrarDevolucao(Cenarios.ROBERTO, AGORA.minusMinutes(5), "DEV-ANTERIOR");
        } else if (estado == StatusMovimentacao.CANCELADA) {
            m.cancelar();
        }
        when(repositorio.findByCodigo("MOV-TESTE")).thenReturn(Optional.of(m));
        LocalDateTime devolucaoAntes = m.getDevolucaoEm();
        String protocoloAntes = m.getProtocolo();
        int historicosAntes = r.getHistorico().size();
        assertThatThrownBy(() -> servico.registrarDevolucao("MOV-TESTE", Cenarios.ROBERTO))
            .isInstanceOfSatisfying(RegraNegocioException.class, erro ->
                assertThat(erro.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(m.getStatus()).isEqualTo(estado);
        assertThat(m.getDevolucaoEm()).isEqualTo(devolucaoAntes);
        assertThat(m.getProtocolo()).isEqualTo(protocoloAntes);
        assertThat(r.getHistorico()).hasSize(historicosAntes);
    }

    @Test
    void materialJaRetiradoVetaModificacaoDaReservaMesmoAtrasado() {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        Movimentacao emUso = movimento(r);
        emUso.registrarRetirada(Cenarios.ROBERTO, AGORA);
        when(repositorio.findByReservaId(null)).thenReturn(List.of(emUso));
        assertThatThrownBy(() -> servico.antesDeModificar(new ReservaEventos.AntesDeModificar(r)))
            .isInstanceOfSatisfying(RegraNegocioException.class, erro ->
                assertThat(erro.getStatus()).isEqualTo(HttpStatus.CONFLICT));
        assertThat(emUso.getStatus()).isEqualTo(StatusMovimentacao.EM_USO);
    }

    @Test
    void cancelamentoAfetaSomenteMateriaisAindaNaoRetirados() {
        Reserva r = Cenarios.reserva(StatusReserva.APROVADA);
        Movimentacao pendente = movimento(r);
        Movimentacao emUso = movimento(r);
        emUso.registrarRetirada(Cenarios.ROBERTO, AGORA);
        Movimentacao devolvido = movimento(r);
        devolvido.registrarRetirada(Cenarios.ROBERTO, AGORA.minusMinutes(10));
        devolvido.registrarDevolucao(Cenarios.ROBERTO, AGORA, "DEV-PRESERVADO");
        when(repositorio.findByReservaId(null)).thenReturn(List.of(pendente, emUso, devolvido));
        servico.cancelarPendentes(r);
        assertThat(pendente.getStatus()).isEqualTo(StatusMovimentacao.CANCELADA);
        assertThat(emUso.getStatus()).isEqualTo(StatusMovimentacao.EM_USO);
        assertThat(devolvido.getStatus()).isEqualTo(StatusMovimentacao.DEVOLVIDO);
        assertThat(devolvido.getProtocolo()).isEqualTo("DEV-PRESERVADO");
    }
}
