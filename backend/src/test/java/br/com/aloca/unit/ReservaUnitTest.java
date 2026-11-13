package br.com.aloca.unit;

import static org.assertj.core.api.Assertions.assertThat;
import br.com.aloca.dominio.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

@Tag("unitario")
class ReservaUnitTest {

    @ParameterizedTest(name = "sala restrita={0}, material restrito={1}, exige aprovação={2}")
    @CsvSource({ "false,false,false", "false,true,true", "true,false,true", "true,true,true" })
    void exigeAprovacaoQuandoSalaOuQualquerMaterialEhRestrito(boolean salaRestrita, boolean materialRestrito, boolean esperado) {
        Recurso sala = Cenarios.recurso("Sala", TipoRecurso.SALA, salaRestrita);
        Recurso livre = Cenarios.recurso("Notebook", TipoRecurso.EQUIPAMENTO, false);
        Recurso segundo = Cenarios.recurso("Projetor", TipoRecurso.EQUIPAMENTO, materialRestrito);
        Reserva r = new Reserva("Aula", Cenarios.DIA, LocalTime.of(10, 0), LocalTime.of(11, 0),
            sala, null, List.of(livre, segundo), Cenarios.ANA, Cenarios.CRIADA);
        assertThat(r.exigeAprovacao()).isEqualTo(esperado);
    }

    @Test
    void salaLivreSemMateriaisNaoExigeAprovacao() {
        assertThat(Cenarios.reserva(StatusReserva.SOLICITADA).exigeAprovacao()).isFalse();
    }

    @Test
    void transicoesPreservamAuditoriaDeAtorInstanteEEstados() {
        Reserva r = Cenarios.reserva(StatusReserva.SOLICITADA);
        LocalDateTime aprovacao = Cenarios.DIA.atTime(9, 0);
        r.mudarStatus(StatusReserva.APROVADA, "Roberto", Cenarios.ROBERTO, "Aprovada", "Análise concluída", aprovacao);
        r.mudarStatus(StatusReserva.EM_USO, "Sistema", null, "Em uso", "Início", Cenarios.DIA.atTime(10, 0));
        r.mudarStatus(StatusReserva.CONCLUIDA, "Sistema", null, "Concluída", "Fim", Cenarios.DIA.atTime(11, 0));

        assertThat(r.getStatus()).isEqualTo(StatusReserva.CONCLUIDA);
        assertThat(r.getHistorico()).extracting(HistoricoReserva::getEstadoAnterior)
            .containsExactly(StatusReserva.SOLICITADA, StatusReserva.APROVADA, StatusReserva.EM_USO);
        assertThat(r.getHistorico()).extracting(HistoricoReserva::getEstadoNovo)
            .containsExactly(StatusReserva.APROVADA, StatusReserva.EM_USO, StatusReserva.CONCLUIDA);
        HistoricoReserva primeiro = r.getHistorico().getFirst();
        assertThat(primeiro.getReserva()).isSameAs(r);
        assertThat(primeiro.getUsuario()).isSameAs(Cenarios.ROBERTO);
        assertThat(primeiro.getAtor()).isEqualTo("Roberto");
        assertThat(primeiro.getDataHora()).isEqualTo(aprovacao);
        assertThat(primeiro.getDescricao()).isEqualTo("Análise concluída");
    }
}
