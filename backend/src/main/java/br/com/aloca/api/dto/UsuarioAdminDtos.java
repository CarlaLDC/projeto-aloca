package br.com.aloca.api.dto;

import br.com.aloca.dominio.*;
import jakarta.validation.constraints.*;
import java.util.Set;

public final class UsuarioAdminDtos {
    private UsuarioAdminDtos() {}
    public record Cadastro(
        @NotBlank @Size(max=100) String nome,
        @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(max=100) String unidade,
        @NotEmpty Set<@NotNull Perfil> perfis,
        @NotBlank @Size(min=8,max=72) String senha) {}
    public record Edicao(
        @NotBlank @Size(max=100) String nome,
        @NotBlank @Email @Size(max=150) String email,
        @NotBlank @Size(max=100) String unidade,
        @NotEmpty Set<@NotNull Perfil> perfis) {}
    public record Senha(@NotBlank @Size(min=8,max=72) String senha) {}
    public record Ativacao(@NotNull Boolean ativo) {}
    public record Resposta(Long id, String nome, String email, String unidade, Set<Perfil> perfis, boolean ativo) {
        public static Resposta de(Usuario u) {
            return new Resposta(u.getId(),u.getNome(),u.getEmail(),u.getUnidade(),Set.copyOf(u.getPerfis()),u.isAtivo());
        }
    }
}
