package br.com.aloca.unit;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import br.com.aloca.api.dto.ReservaDtos.ReservaRequest;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.*;
import br.com.aloca.servico.*;
import br.com.aloca.api.dto.ReservaDtos.Alternativa;
import br.com.aloca.servico.ReservaService.EmConflito;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * DEF-PERF-001: no 409 por professor ou material, a sugestão "outro local" não pode varrer as salas,
 * porque o professor/material continua ocupado em qualquer sala. Antes, cada sala ativa era testada
 * no banco sob a trava do pedido (~0,9 s por 409 com 190 espaços na carga).
 */
@Tag("unitario")
class AlternativasConflitoUnitTest {
    private static final String OUTRO_LOCAL = "Outro local, mesmo horário";

    private final ReservaRepositorio reservas = mock(ReservaRepositorio.class);
    private final RecursoRepositorio recursos = mock(RecursoRepositorio.class);
    private final ProfessorRepositorio professores = mock(ProfessorRepositorio.class);
    private final BloqueioRepositorio bloqueios = mock(BloqueioRepositorio.class);

    private final Recurso sala = comId(Cenarios.recurso("Sala 101", TipoRecurso.SALA, false), 1L);
    private final Recurso outraSala = comId(Cenarios.recurso("Sala 102", TipoRecurso.SALA, false), 2L);
    private final Recurso material = comId(Cenarios.recurso("Projetor", TipoRecurso.EQUIPAMENTO, false), 3L);
    private final Professor professor = comId(new Professor("Lucas", "Física", null), 7L);

    private ReservaService servico;

    @BeforeEach
    void preparar() {
        servico = new ReservaService(reservas, recursos, professores, bloqueios, mock(ConflitoRegistradoRepositorio.class),
            mock(ApplicationEventPublisher.class), mock(NotificacaoService.class), mock(Mapeador.class),
            Cenarios.relogio(Cenarios.DIA.atTime(8, 0)));
        when(recursos.travar(any())).thenAnswer(inv -> {
            Collection<Long> ids = inv.getArgument(0);
            return List.of(sala, outraSala, material).stream().filter(r -> ids.contains(r.getId())).toList();
        });
        when(professores.travar(7L)).thenReturn(Optional.of(professor));
        when(recursos.findByAtivoTrueOrderByNome()).thenReturn(List.of(material, sala, outraSala));
    }

    @Test
    void conflitoDeProfessorNaoVarreAsSalas() {
        // O professor está em outra atividade das 10h às 11h em qualquer dia pesquisado; a sala está livre.
        Reserva ocupado = new Reserva("Outra aula", Cenarios.DIA, LocalTime.of(10, 0), LocalTime.of(11, 0),
            outraSala, professor, List.of(), Cenarios.ANA, Cenarios.CRIADA);
        when(reservas.conflitantes(any(), any(), any(), any(), eq(7L), any(), any())).thenAnswer(inv ->
            sobrepoe(inv.getArgument(1), inv.getArgument(2)) ? List.of(ocupado) : List.of());

        EmConflito conflito = (EmConflito) servico.criar(pedido(7L, List.of()), Cenarios.ANA);

        assertThat(conflito.conflito().conflitos()).extracting("tipo").containsOnly("Professor");
        assertThat(motivos(conflito)).doesNotContain(OUTRO_LOCAL).contains("Mesmo local, outro horário");
        verify(recursos, never()).findByAtivoTrueOrderByNome();
    }

    @Test
    void conflitoDeMaterialNaoVarreAsSalas() {
        Reserva ocupado = new Reserva("Outra aula", Cenarios.DIA, LocalTime.of(10, 0), LocalTime.of(11, 0),
            outraSala, null, List.of(material), Cenarios.ANA, Cenarios.CRIADA);
        when(reservas.conflitantes(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv ->
            sobrepoe(inv.getArgument(1), inv.getArgument(2)) && ((Collection<?>) inv.getArgument(3)).contains(3L)
                ? List.of(ocupado) : List.of());

        EmConflito conflito = (EmConflito) servico.criar(pedido(null, List.of(3L)), Cenarios.ANA);

        assertThat(conflito.conflito().conflitos()).extracting("tipo").containsOnly("Material");
        assertThat(motivos(conflito)).doesNotContain(OUTRO_LOCAL);
        verify(recursos, never()).findByAtivoTrueOrderByNome();
    }

    @Test
    void conflitoSoDaSalaContinuaSugerindoOutroLocal() {
        Reserva ocupada = new Reserva("Outra aula", Cenarios.DIA, LocalTime.of(10, 0), LocalTime.of(11, 0),
            sala, null, List.of(), Cenarios.ANA, Cenarios.CRIADA);
        when(reservas.conflitantes(any(), any(), any(), any(), any(), any(), any())).thenAnswer(inv ->
            Cenarios.DIA.equals(inv.getArgument(0)) && sobrepoe(inv.getArgument(1), inv.getArgument(2))
                && ((Collection<?>) inv.getArgument(3)).contains(1L) ? List.of(ocupada) : List.of());

        EmConflito conflito = (EmConflito) servico.criar(pedido(null, List.of()), Cenarios.ANA);

        assertThat(conflito.conflito().conflitos()).extracting("tipo").containsOnly("Sala");
        assertThat(conflito.conflito().alternativas())
            .filteredOn(a -> OUTRO_LOCAL.equals(a.motivo()))
            .singleElement()
            .satisfies(a -> assertThat(a.espacoId()).isEqualTo(2L));
    }

    private static ReservaRequest pedido(Long professorId, List<Long> materiais) {
        return new ReservaRequest("Aula", Cenarios.DIA, LocalTime.of(10, 0), LocalTime.of(11, 0), 1L, professorId, materiais);
    }

    private static boolean sobrepoe(LocalTime inicio, LocalTime fim) {
        return inicio.isBefore(LocalTime.of(11, 0)) && fim.isAfter(LocalTime.of(10, 0));
    }

    private static List<String> motivos(EmConflito conflito) {
        return conflito.conflito().alternativas().stream().map(Alternativa::motivo).toList();
    }

    private static <T> T comId(T entidade, long id) {
        ReflectionTestUtils.setField(entidade, "id", id);
        return entidade;
    }
}
