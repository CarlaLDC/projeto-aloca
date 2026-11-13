package br.com.aloca.api.dto;

import br.com.aloca.dominio.Perfil;
import br.com.aloca.dominio.Usuario;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import java.time.Instant;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/** Tela 01 · Entrar. */
public final class AuthDtos {
    private AuthDtos() {}

    public record LoginRequest(
        @Schema(description = "Usuário de demonstração", example = "ana.martins@aloca.edu.br")
        @NotBlank(message = "Informe o e-mail.") @Email(message = "Informe um e-mail válido.") String email,
        @Schema(description = "Senha dos usuários de demonstração", example = "aloca123")
        @NotBlank(message = "Informe a senha.") String senha) {}

    public record LoginResponse(String token, Instant expiraEm, UsuarioDto usuario) {}

    public record RecuperarSenhaRequest(String email) {}

    public record UsuarioDto(Long id, String nome, String email, String unidade, List<String> perfis,
                             String perfilRotulo, String iniciais) {

        public static UsuarioDto de(Usuario u) {
            String rotulo = u.getPerfis().stream()
                .sorted(Comparator.comparingInt(Perfil::ordinal))
                .map(Perfil::rotulo)
                .collect(Collectors.joining(" / "));
            String iniciais = Arrays.stream(u.getNome().trim().split("\\s+"))
                .filter(p -> !p.isEmpty())
                .map(p -> p.substring(0, 1).toUpperCase())
                .limit(2)
                .collect(Collectors.joining());
            return new UsuarioDto(u.getId(), u.getNome(), u.getEmail(), u.getUnidade(),
                u.getPerfis().stream().map(Perfil::name).sorted().toList(), rotulo, iniciais);
        }
    }

    public record MensagemDto(String mensagem) {}
}
