package br.com.aloca.api.dto;

import br.com.aloca.dominio.TipoRecurso;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;
import java.time.LocalTime;

/** Cadastros e bloqueios (RF-02 e RF-08). Ainda não há tela no front para estes endpoints. */
public final class AdminDtos {
    private AdminDtos() {}

    public record RecursoRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 150) String nome,
        @NotNull(message = "Informe o tipo.") TipoRecurso tipo,
        @Size(max = 60) String categoria,
        @NotBlank(message = "Informe a localização.") @Size(max = 150) String localizacao,
        @NotNull(message = "Informe a capacidade.") @Min(value = 1, message = "A capacidade deve ser pelo menos 1.") Integer capacidade,
        Boolean restrito,
        @NotNull(message = "Informe o horário de abertura.") LocalTime abertura,
        @NotNull(message = "Informe o horário de fechamento.") LocalTime fechamento) {

        /** Campo opcional: quando não vem no JSON, o recurso não é restrito. */
        public boolean ehRestrito() { return Boolean.TRUE.equals(restrito); }
    }

    public record RecursoDto(Long id, String nome, String tipo, String categoria, String localizacao, int capacidade,
                             boolean restrito, boolean ativo, LocalTime abertura, LocalTime fechamento) {}

    public record ProfessorRequest(
        @NotBlank(message = "Informe o nome.") @Size(max = 100) String nome,
        @NotBlank(message = "Informe a competência.") @Size(max = 100) String competencia) {}

    public record ProfessorDto(Long id, String nome, String competencia, boolean ativo) {}

    public record BloqueioRequest(
        @NotNull(message = "Selecione o recurso.") Long recursoId,
        @NotNull(message = "Informe o início.") LocalDateTime inicio,
        @NotNull(message = "Informe o fim.") LocalDateTime fim,
        @NotBlank(message = "Informe o motivo.") @Size(max = 300) String motivo) {}

    public record BloqueioDto(Long id, Long recursoId, String recurso, LocalDateTime inicio, LocalDateTime fim,
                              String motivo, String criadoPor) {}
}
