package br.com.aloca.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

/** Telas 04 · Nova reserva, 05 · Conflito, 06 · Minhas reservas e 07 · Detalhe e auditoria. */
public final class ReservaDtos {
    private ReservaDtos() {}

    public record OpcaoRecurso(Long id, String nome, String tipo, String tipoRotulo, String localizacao,
                               int capacidade, boolean restrito, String rotulo) {}

    public record OpcaoProfessor(Long id, String nome, String competencia, String rotulo) {}

    public record OpcoesReserva(List<OpcaoRecurso> espacos, List<OpcaoProfessor> professores,
                                List<OpcaoRecurso> materiais) {}

    public record ReservaRequest(
        @NotBlank(message = "Informe a atividade.") @Size(max = 150, message = "A atividade pode ter até 150 caracteres.") String atividade,
        @NotNull(message = "Informe a data.") LocalDate data,
        @NotNull(message = "Informe o horário de início.") LocalTime inicio,
        @NotNull(message = "Informe o horário de término.") LocalTime fim,
        @NotNull(message = "Selecione a sala ou laboratório.") Long espacoId,
        Long professorId,
        List<Long> materialIds) {

        public List<Long> materiais() { return materialIds == null ? List.of() : materialIds; }
    }

    public record CancelamentoRequest(
        @NotBlank(message = "Informe o motivo do cancelamento.") @Size(max = 500, message = "O motivo pode ter até 500 caracteres.") String motivo) {}

    public record ReservaResumo(String codigo, String atividade, String recurso, LocalDate data, LocalTime inicio,
                                LocalTime fim, String status, String statusRotulo) {}

    public record EventoHistorico(LocalDateTime dataHora, String ator, String acao, String descricao,
                                  String estadoAnterior, String estadoNovo) {}

    public record ReservaDetalhe(String codigo, String atividade, LocalDate data, LocalTime inicio, LocalTime fim,
                                 OpcaoRecurso espaco, OpcaoProfessor professor, List<OpcaoRecurso> materiais,
                                 String solicitante, String status, String statusRotulo, String motivoRejeicao,
                                 boolean exigeAprovacao, boolean podeGerenciar, LocalDateTime criadaEm,
                                 List<EventoHistorico> historico) {}

    /** Choque com uma reserva existente ou com um bloqueio de manutenção. */
    public record ItemConflito(String tipo, String recurso, String localizacao, LocalTime inicio, LocalTime fim,
                               String atividade, String responsavel, long sobreposicaoMinutos) {}

    public record Alternativa(LocalDate data, LocalTime inicio, LocalTime fim, Long espacoId, String espaco,
                              String localizacao, String motivo) {}

    /** Corpo da resposta 409 quando a reserva não pode ser feita por conflito (tela 05). */
    public record ConflitoResposta(String mensagem, String atividade, LocalDate data, LocalTime inicio, LocalTime fim,
                                   Long espacoId, Long professorId, List<Long> materialIds,
                                   List<ItemConflito> conflitos, List<Alternativa> alternativas) {}
}
