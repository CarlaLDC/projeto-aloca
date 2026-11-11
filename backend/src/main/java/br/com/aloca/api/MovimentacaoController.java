package br.com.aloca.api;

import br.com.aloca.api.dto.MovimentacaoDtos.*;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.MovimentacaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 09 · Retiradas e devoluções (RF-09). */
@RestController
@RequestMapping("/api/movimentacoes")
@PreAuthorize("hasRole('RESPONSAVEL')")
public class MovimentacaoController {

    private final MovimentacaoService movimentacoes;
    private final UsuarioAtual usuarioAtual;

    public MovimentacaoController(MovimentacaoService movimentacoes, UsuarioAtual usuarioAtual) {
        this.movimentacoes = movimentacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public Movimentacoes listar(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String busca) {
        return movimentacoes.listar(status, busca);
    }

    @PostMapping("/{codigo}/retirada")
    public MovimentacaoDto retirada(@PathVariable String codigo) {
        return movimentacoes.registrarRetirada(codigo, usuarioAtual.obter());
    }

    @PostMapping("/{codigo}/devolucao")
    public MovimentacaoDto devolucao(@PathVariable String codigo) {
        return movimentacoes.registrarDevolucao(codigo, usuarioAtual.obter());
    }
}
