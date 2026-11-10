package br.com.aloca.api;

import br.com.aloca.api.dto.NotificacaoDtos.Notificacoes;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.NotificacaoService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Contador e lista do botão "Notificações" do topo das telas. */
@RestController
@RequestMapping("/api/notificacoes")
public class NotificacaoController {

    private final NotificacaoService notificacoes;
    private final UsuarioAtual usuarioAtual;

    public NotificacaoController(NotificacaoService notificacoes, UsuarioAtual usuarioAtual) {
        this.notificacoes = notificacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public Notificacoes listar() {
        return notificacoes.listar(usuarioAtual.obter());
    }

    @PostMapping("/lidas")
    public ResponseEntity<Void> marcarLidas() {
        notificacoes.marcarLidas(usuarioAtual.obter());
        return ResponseEntity.noContent().build();
    }
}
