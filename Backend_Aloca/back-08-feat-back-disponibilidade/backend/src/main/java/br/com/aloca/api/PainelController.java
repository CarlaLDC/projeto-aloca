package br.com.aloca.api;

import br.com.aloca.api.dto.PainelDtos.Painel;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.PainelService;
import org.springframework.web.bind.annotation.*;

/** Tela 02 · Visão geral. */
@RestController
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painel;
    private final UsuarioAtual usuarioAtual;

    public PainelController(PainelService painel, UsuarioAtual usuarioAtual) {
        this.painel = painel;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public Painel painel() {
        return painel.montar(usuarioAtual.obter());
    }
}
