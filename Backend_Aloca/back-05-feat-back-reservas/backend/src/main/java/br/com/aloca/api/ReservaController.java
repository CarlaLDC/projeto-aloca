package br.com.aloca.api;

import br.com.aloca.api.dto.ReservaDtos.*;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.ReservaService;
import br.com.aloca.servico.ReservaService.EmConflito;
import br.com.aloca.servico.ReservaService.Gravada;
import br.com.aloca.servico.ReservaService.Resultado;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Telas 04 · Nova reserva, 05 · Conflito de horário, 06 · Minhas reservas e 07 · Detalhe e auditoria. */
@RestController
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;
    private final UsuarioAtual usuarioAtual;

    public ReservaController(ReservaService reservas, UsuarioAtual usuarioAtual) {
        this.reservas = reservas;
        this.usuarioAtual = usuarioAtual;
    }

    /** Listas de salas, professores e materiais para os campos da tela de nova reserva. */
    @GetMapping("/opcoes")
    public OpcoesReserva opcoes() {
        return reservas.opcoes();
    }

    @GetMapping("/minhas")
    public List<ReservaResumo> minhas() {
        return reservas.minhas(usuarioAtual.obter());
    }

    @GetMapping("/{codigo}")
    public ReservaDetalhe detalhe(@PathVariable String codigo) {
        return reservas.detalhe(codigo, usuarioAtual.obter());
    }

    /** 201 com a reserva criada, ou 409 com os conflitos e alternativas (tela 05). */
    @PostMapping
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR')")
    public ResponseEntity<?> criar(@Valid @RequestBody ReservaRequest pedido) {
        return responder(reservas.criar(pedido, usuarioAtual.obter()), HttpStatus.CREATED);
    }

    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR')")
    public ResponseEntity<?> alterar(@PathVariable String codigo, @Valid @RequestBody ReservaRequest pedido) {
        return responder(reservas.alterar(codigo, pedido, usuarioAtual.obter()), HttpStatus.OK);
    }

    @PostMapping("/{codigo}/cancelamento")
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR')")
    public ReservaDetalhe cancelar(@PathVariable String codigo, @Valid @RequestBody CancelamentoRequest pedido) {
        return reservas.cancelar(codigo, pedido.motivo(), usuarioAtual.obter());
    }

    private static ResponseEntity<?> responder(Resultado resultado, HttpStatus sucesso) {
        return switch (resultado) {
            case Gravada g -> ResponseEntity.status(sucesso).body(g.reserva());
            case EmConflito c -> ResponseEntity.status(HttpStatus.CONFLICT).body(c.conflito());
        };
    }
}
