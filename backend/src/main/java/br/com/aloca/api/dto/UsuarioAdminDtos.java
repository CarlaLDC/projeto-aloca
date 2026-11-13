package br.com.aloca.api.dto;

import br.com.aloca.dominio.*;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import java.util.Set;

public final class UsuarioAdminDtos {
    private UsuarioAdminDtos() {}
    public record Cadastro(
        @NotBlank @Size(max=100) String nome,
        @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(max=100) String unidade,
        @NotEmpty Set<@NotNull Perfil> perfis,
        @NotBlank @Size(min=8,max=72) String senha,
        @Schema(description = "Opcional, só para o perfil Professor: cadastro de professor ligado à conta (agenda própria)", nullable = true)
        Long professorId) {}
    public record Edicao(
        @NotBlank @Size(max=100) String nome,
        @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(max=100) String unidade,
        @NotEmpty Set<@NotNull Perfil> perfis,
        @Schema(description = "Opcional: cadastro de professor a ligar à conta. Ausente mantém o vínculo atual "
            + "(o vínculo é desfeito se a conta perder o perfil Professor).", nullable = true)
        Long professorId) {}
    public record Senha(@NotBlank @Size(min=8,max=72) String senha) {}
    public record Ativacao(@NotNull Boolean ativo) {}
    public record Resposta(Long id, String nome, String email, String unidade, Set<Perfil> perfis, boolean ativo,
                           @Schema(nullable = true) Long professorId, @Schema(nullable = true) String professor) {
        public static Resposta de(Usuario u) { return de(u, null); }
        public static Resposta de(Usuario u, Professor p) {
            return new Resposta(u.getId(),u.getNome(),u.getEmail(),u.getUnidade(),Set.copyOf(u.getPerfis()),u.isAtivo(),
                p == null ? null : p.getId(), p == null ? null : p.getNome());
        }
    }
}
