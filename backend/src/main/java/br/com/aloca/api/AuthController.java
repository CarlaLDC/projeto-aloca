package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.AuthDtos.*;
import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.seguranca.ProtecaoForcaBruta;
import br.com.aloca.seguranca.TokenService;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.RegraNegocioException;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.UUID;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Tela 01 · Entrar. */
@RestController
@Tag(name = "01 · Entrar", description = "Login e dados do usuário logado (RF-01).")
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    private final UsuarioAtual usuarioAtual;
    private final ProtecaoForcaBruta protecao;
    /** Hash usado quando o e-mail não existe, para o tempo de resposta não revelar se a conta existe. */
    private final String hashFicticio;

    public AuthController(UsuarioRepositorio usuarios, PasswordEncoder senhas, TokenService tokens,
                          UsuarioAtual usuarioAtual, ProtecaoForcaBruta protecao) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.usuarioAtual = usuarioAtual;
        this.protecao = protecao;
        // Hash de um valor aleatório: e-mail inexistente leva o mesmo tempo que senha errada.
        this.hashFicticio = senhas.encode(UUID.randomUUID().toString());
    }

    @Operation(summary = "Entrar e receber o token JWT", description = "Devolve o token (válido por 8 h) e os dados do usuário. Não exige token.")
    @ApiResponse(responseCode = "200", description = "Login feito")
    @ApiResponse(responseCode = "400", description = "E-mail ou senha em branco ou e-mail inválido", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "401", description = "E-mail ou senha incorretos (a mesma mensagem nos dois casos)", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "429", description = "Muitas senhas erradas para este e-mail: bloqueado por 15 minutos (cabeçalho Retry-After em segundos)", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @SecurityRequirements
    @PostMapping("/login")
    @Transactional(readOnly = true)
    public LoginResponse login(@Valid @RequestBody LoginRequest pedido) {
        String email = pedido.email().trim();
        protecao.tempoRestante(email).ifPresent(resta -> { throw bloqueado(resta); });

        Optional<Usuario> usuario = usuarios.findByEmailIgnoreCase(email).filter(Usuario::isAtivo);
        // Sempre confere uma senha (a do usuário ou um hash fictício): o tempo de resposta é o mesmo
        // para e-mail inexistente e para senha errada.
        boolean senhaCerta = senhas.matches(pedido.senha(), usuario.map(Usuario::getSenhaHash).orElse(hashFicticio))
            && usuario.isPresent();
        if (!senhaCerta) {
            if (protecao.registrarFalha(email)) throw bloqueado(protecao.getBloqueio());
            // Mesma mensagem para e-mail inexistente e senha errada, para não revelar contas (RNF-03).
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos.");
        }
        protecao.registrarSucesso(email);
        Usuario u = usuario.get();
        TokenService.Token token = tokens.emitir(u);
        return new LoginResponse(token.valor(), token.expiraEm(), UsuarioDto.de(u));
    }

    private static RegraNegocioException bloqueado(Duration resta) {
        long minutos = Math.max(1, (resta.toSeconds() + 59) / 60);
        return RegraNegocioException.muitasTentativas("Muitas tentativas de login. Tente novamente em " + minutos
            + (minutos == 1 ? " minuto." : " minutos."), resta);
    }

    @Operation(summary = "Usuário do token", description = "Qualquer perfil.")
    @ApiResponse(responseCode = "200", description = "Dados do usuário logado")
    @ApiResponse(responseCode = "401", description = "Sem token ou token expirado", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UsuarioDto eu() {
        return UsuarioDto.de(usuarioAtual.obter());
    }

    /** A recuperação é feita pelo suporte da instituição; a API só devolve a orientação. */
    @Operation(summary = "Orientação para recuperar a senha", description = "A recuperação é feita pelo suporte da instituição. Não exige token.")
    @ApiResponse(responseCode = "200", description = "Mensagem de orientação")
    @SecurityRequirements
    @PostMapping("/recuperar-senha")
    public MensagemDto recuperarSenha(@RequestBody(required = false) RecuperarSenhaRequest pedido) {
        return new MensagemDto("Para recuperar sua senha, entre em contato com o suporte da sua instituição.");
    }
}
