package br.com.aloca.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Tela 08 · Aprovações. */
public final class AprovacaoDtos {
    private AprovacaoDtos() {}

    public record Solicitacao(String codigo, String atividade, String solicitante, LocalDateTime criadaEm,
                              String recursos, LocalDate data, LocalTime inicio, LocalTime fim, String professor,
                              boolean disponibilidadeValidada, List<String> problemas) {}

    public record Aprovacoes(long pendentes, long aprovadasHoje, long rejeitadasHoje, List<Solicitacao> solicitacoes) {}

    /** Corpo opcional do registro de não comparecimento. */
    public record NaoComparecimentoRequest(
        @Size(max = 500, message = "A observação pode ter até 500 caracteres.") String observacao) {}

    public record RejeicaoRequest(
        @NotBlank(message = "Informe a justificativa.") @Size(max = 500, message = "A justificativa pode ter até 500 caracteres.") String justificativa) {}
}
