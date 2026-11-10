package br.com.aloca.api;

import br.com.aloca.api.dto.AuthDtos.*;
import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.seguranca.TokenService;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.RegraNegocioException;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

/** Tela 01 · Entrar. */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;
    private final TokenService tokens;
    private final UsuarioAtual usuarioAtual;

    public AuthController(UsuarioRepositorio usuarios, PasswordEncoder senhas, TokenService tokens,
                          UsuarioAtual usuarioAtual) {
        this.usuarios = usuarios;
        this.senhas = senhas;
        this.tokens = tokens;
        this.usuarioAtual = usuarioAtual;
    }

    @PostMapping("/login")
    @Transactional(readOnly = true)
    public LoginResponse login(@Valid @RequestBody LoginRequest pedido) {
        Usuario u = usuarios.findByEmailIgnoreCase(pedido.email().trim())
            .filter(Usuario::isAtivo)
            .filter(x -> senhas.matches(pedido.senha(), x.getSenhaHash()))
            // Mesma mensagem para e-mail inexistente e senha errada, para não revelar contas (RNF-03).
            .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "E-mail ou senha incorretos."));
        TokenService.Token token = tokens.emitir(u);
        return new LoginResponse(token.valor(), token.expiraEm(), UsuarioDto.de(u));
    }

    @GetMapping("/me")
    @Transactional(readOnly = true)
    public UsuarioDto eu() {
        return UsuarioDto.de(usuarioAtual.obter());
    }

    /** A recuperação é feita pelo suporte da instituição; a API só devolve a orientação. */
    @PostMapping("/recuperar-senha")
    public MensagemDto recuperarSenha(@RequestBody(required = false) RecuperarSenhaRequest pedido) {
        return new MensagemDto("Para recuperar sua senha, entre em contato com o suporte da sua instituição.");
    }
}
