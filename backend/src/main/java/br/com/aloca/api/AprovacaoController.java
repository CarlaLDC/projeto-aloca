package br.com.aloca.api;

import br.com.aloca.api.dto.AprovacaoDtos.*;
import br.com.aloca.api.dto.ReservaDtos.ReservaDetalhe;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.AprovacaoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 08 · Aprovações. Somente o Responsável aprova recursos restritos (RF-07). */
@RestController
@RequestMapping("/api/aprovacoes")
@PreAuthorize("hasRole('RESPONSAVEL')")
public class AprovacaoController {

    private final AprovacaoService aprovacoes;
    private final UsuarioAtual usuarioAtual;

    public AprovacaoController(AprovacaoService aprovacoes, UsuarioAtual usuarioAtual) {
        this.aprovacoes = aprovacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping
    public Aprovacoes listar() {
        return aprovacoes.listar();
    }

    @PostMapping("/{codigo}/aprovar")
    public ReservaDetalhe aprovar(@PathVariable String codigo) {
        return aprovacoes.aprovar(codigo, usuarioAtual.obter());
    }

    @PostMapping("/{codigo}/rejeitar")
    public ReservaDetalhe rejeitar(@PathVariable String codigo, @Valid @RequestBody RejeicaoRequest pedido) {
        return aprovacoes.rejeitar(codigo, pedido.justificativa(), usuarioAtual.obter());
    }
}
