package br.com.aloca.unit;

import br.com.aloca.dominio.*;
import java.time.*;
import java.util.*;

final class Cenarios {
    static final LocalDate DIA = LocalDate.of(2030, 6, 10);
    static final LocalDateTime CRIADA = DIA.minusDays(1).atTime(9, 0);
    static final Usuario ANA = new Usuario("Ana", "ana@teste", "hash", "Teste", Set.of(Perfil.SOLICITANTE));
    static final Usuario ROBERTO = new Usuario("Roberto", "roberto@teste", "hash", "Teste", Set.of(Perfil.RESPONSAVEL));

    private Cenarios() {}

    static Recurso recurso(String nome, TipoRecurso tipo, boolean restrito) {
        return new Recurso(nome, tipo, null, "Bloco A", 20, restrito, LocalTime.of(7, 0), LocalTime.of(22, 0));
    }

    static Reserva reserva(StatusReserva status) {
        Reserva r = new Reserva("Aula", DIA, LocalTime.of(10, 0), LocalTime.of(11, 0),
            recurso("Sala", TipoRecurso.SALA, false), null, List.of(), ANA, CRIADA);
        r.setCodigo("RES-TESTE");
        // Chega ao estado pedido por um caminho válido da matriz de estados (CicloDeVidaReserva),
        // porque a entidade recusa transições que não existem no fluxo real.
        List<StatusReserva> caminho = switch (status) {
            case SOLICITADA -> List.of();
            case APROVADA, REJEITADA, CANCELADA -> List.of(status);
            case EM_USO -> List.of(StatusReserva.APROVADA, StatusReserva.EM_USO);
            case CONCLUIDA -> List.of(StatusReserva.APROVADA, StatusReserva.EM_USO, StatusReserva.CONCLUIDA);
            case NAO_COMPARECEU -> List.of(StatusReserva.APROVADA, StatusReserva.NAO_COMPARECEU);
        };
        for (StatusReserva passo : caminho) {
            r.mudarStatus(passo, "Preparação", null, "Preparação", "Estado inicial do caso", CRIADA);
        }
        return r;
    }

    static Clock relogio(LocalDateTime agora) {
        ZoneId zona = ZoneId.of("America/Sao_Paulo");
        return Clock.fixed(agora.atZone(zona).toInstant(), zona);
    }
}
