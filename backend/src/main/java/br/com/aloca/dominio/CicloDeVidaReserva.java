package br.com.aloca.dominio;

import static br.com.aloca.dominio.StatusReserva.*;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Regras de estado da reserva, sem dependência de Spring ou banco (testadas em CicloDeVidaReservaTest).
 *
 * <pre>
 * SOLICITADA ──aprovar──▶ APROVADA ──início──▶ EM_USO ──fim──▶ CONCLUÍDA
 *     │ rejeitar/cancelar      │ cancelar/não compareceu   │ não compareceu
 *     ▼                        ▼                           ▼
 * REJEITADA, CANCELADA     CANCELADA, NÃO COMPARECEU   NÃO COMPARECEU
 * </pre>
 * Alterar uma reserva registra o evento sem mudar o estado (SOLICITADA → SOLICITADA, APROVADA → APROVADA)
 * ou devolve uma reserva aprovada para nova análise (APROVADA → SOLICITADA) quando passa a ter recurso restrito.
 * APROVADA → CONCLUÍDA acontece quando o ciclo automático só roda depois do fim do horário.
 */
public final class CicloDeVidaReserva {

    /** Tolerância depois do início: antes disso, o solicitante ainda pode chegar. */
    public static final Duration TOLERANCIA_NAO_COMPARECIMENTO = Duration.ofMinutes(15);

    private static final Map<StatusReserva, Set<StatusReserva>> TRANSICOES = new EnumMap<>(StatusReserva.class);

    static {
        TRANSICOES.put(SOLICITADA, EnumSet.of(SOLICITADA, APROVADA, REJEITADA, CANCELADA));
        TRANSICOES.put(APROVADA, EnumSet.of(APROVADA, SOLICITADA, EM_USO, CONCLUIDA, CANCELADA, NAO_COMPARECEU));
        TRANSICOES.put(EM_USO, EnumSet.of(CONCLUIDA, NAO_COMPARECEU));
        TRANSICOES.put(CONCLUIDA, EnumSet.noneOf(StatusReserva.class));
        TRANSICOES.put(REJEITADA, EnumSet.noneOf(StatusReserva.class));
        TRANSICOES.put(CANCELADA, EnumSet.noneOf(StatusReserva.class));
        TRANSICOES.put(NAO_COMPARECEU, EnumSet.noneOf(StatusReserva.class));
    }

    private CicloDeVidaReserva() {}

    public static Set<StatusReserva> destinos(StatusReserva de) {
        return EnumSet.copyOf(TRANSICOES.get(de).isEmpty() ? EnumSet.noneOf(StatusReserva.class) : TRANSICOES.get(de));
    }

    public static boolean permitida(StatusReserva de, StatusReserva para) {
        return TRANSICOES.get(de).contains(para);
    }

    public static boolean finalizado(StatusReserva status) {
        return TRANSICOES.get(status).isEmpty();
    }

    /** Próximo estado do ciclo automático: Em uso no início, Concluída no fim. */
    public static Optional<StatusReserva> proximoPeloRelogio(StatusReserva atual, LocalDateTime agora,
                                                             LocalDateTime inicio, LocalDateTime fim) {
        if (atual != APROVADA && atual != EM_USO) return Optional.empty();
        if (!agora.isBefore(fim)) return Optional.of(CONCLUIDA);
        if (atual == APROVADA && !agora.isBefore(inicio)) return Optional.of(EM_USO);
        return Optional.empty();
    }

    /** Motivo que impede alterar ou cancelar; vazio quando é permitido. */
    public static Optional<String> impedimentoParaGerenciar(StatusReserva status, LocalDateTime agora, LocalDateTime inicio) {
        if (!status.gerenciavel()) {
            return Optional.of("Reservas com status \"" + status.rotulo() + "\" não podem ser alteradas nem canceladas.");
        }
        if (!agora.isBefore(inicio)) {
            return Optional.of("Esta reserva já está em uso e permanece no histórico.");
        }
        return Optional.empty();
    }

    /**
     * Motivo que impede registrar o não comparecimento; vazio quando é permitido.
     * Janela: de 15 minutos depois do início até antes do fim, para reservas aprovadas ou em uso.
     */
    public static Optional<String> impedimentoParaNaoComparecimento(StatusReserva status, LocalDateTime agora,
                                                                    LocalDateTime inicio, LocalDateTime fim) {
        if (status != APROVADA && status != EM_USO) {
            return Optional.of("Somente reservas aprovadas ou em uso admitem não comparecimento.");
        }
        if (agora.isBefore(inicio.plus(TOLERANCIA_NAO_COMPARECIMENTO)) || !agora.isBefore(fim)) {
            return Optional.of("A ausência pode ser registrada somente de 15 minutos após o início até o fim da reserva.");
        }
        return Optional.empty();
    }
}
