package br.com.aloca.dominio;

import java.util.EnumSet;
import java.util.Set;

public enum StatusReserva {
    SOLICITADA("Solicitada"),
    APROVADA("Aprovada"),
    REJEITADA("Rejeitada"),
    EM_USO("Em uso"),
    CONCLUIDA("Concluída"),
    CANCELADA("Cancelada"),
    NAO_COMPARECEU("Não compareceu");

    /** Estados que ocupam a agenda e, portanto, geram conflito. */
    public static final Set<StatusReserva> OCUPAM_AGENDA = EnumSet.of(SOLICITADA, APROVADA, EM_USO);

    /** Estados contabilizados como uso efetivo nos relatórios. */
    public static final Set<StatusReserva> USO_CONFIRMADO = EnumSet.of(APROVADA, EM_USO, CONCLUIDA);

    private final String rotulo;

    StatusReserva(String rotulo) { this.rotulo = rotulo; }

    public String rotulo() { return rotulo; }

    public boolean gerenciavel() { return this == SOLICITADA || this == APROVADA; }
}
