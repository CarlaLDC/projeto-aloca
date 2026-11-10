package br.com.aloca.servico;

import br.com.aloca.dominio.Reserva;

/**
 * Eventos publicados pelas regras de reserva. Outros módulos (como o de materiais)
 * reagem a eles sem que o módulo de reservas precise conhecê-los.
 * São tratados na mesma transação da reserva.
 */
public final class ReservaEventos {
    private ReservaEventos() {}

    /** A reserva foi aprovada (pelo responsável ou automaticamente). */
    public record Aprovada(Reserva reserva) {}

    /** Os dados da reserva mudaram. */
    public record Alterada(Reserva reserva) {}

    /** A reserva foi cancelada. */
    public record Cancelada(Reserva reserva) {}

    /** Antes de alterar ou cancelar: um ouvinte pode impedir lançando RegraNegocioException. */
    public record AntesDeModificar(Reserva reserva) {}
}
