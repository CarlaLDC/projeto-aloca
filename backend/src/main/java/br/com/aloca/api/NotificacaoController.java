package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.NotificacaoDtos.Notificacoes;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Contador e lista do botão "Notificações" do topo das telas. */
@RestController
@Tag(name = "Notificações", description = "Notificações do usuário (RF-11). Também são enviadas ao serviço de e-mail externo, se configurado.")
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacoes;
    private final UsuarioAtual usuarioAtual;

    public NotificacaoController(NotificacaoService notificacoes, UsuarioAtual usuarioAtual) {
        this.notificacoes = notificacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Operation(summary = "Minhas notificações", description = "As 20 mais recentes e o total de não lidas.")
    @ApiResponse(responseCode = "200", description = "Notificações")
    @GetMapping
    public Notificacoes listar() {
        return notificacoes.listar(usuarioAtual.obter());
    }

    @Operation(summary = "Marcar todas como lidas")
    @ApiResponse(responseCode = "204", description = "Notificações marcadas como lidas")
    @PostMapping("/lidas")
    public ResponseEntity<Void> marcarLidas() {
        notificacoes.marcarLidas(usuarioAtual.obter());
        return ResponseEntity.noContent().build();
    }
}
