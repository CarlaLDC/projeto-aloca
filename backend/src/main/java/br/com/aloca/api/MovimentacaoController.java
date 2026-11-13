package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.MovimentacaoDtos.*;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.MovimentacaoService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 09 · Retiradas e devoluções (RF-09). */
@RestController
@Tag(name = "09 · Retiradas e devoluções", description = "Movimentação de materiais das reservas aprovadas (RF-09). Somente o perfil Responsável.")
@RequestMapping("/api/movimentacoes")
@PreAuthorize("hasRole('RESPONSAVEL')")
public class MovimentacaoController {

    private final MovimentacaoService movimentacoes;
    private final UsuarioAtual usuarioAtual;

    public MovimentacaoController(MovimentacaoService movimentacoes, UsuarioAtual usuarioAtual) {
        this.movimentacoes = movimentacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Operation(summary = "Movimentações de materiais", description = "Filtros opcionais: status (AGUARDANDO_RETIRADA, EM_USO, DEVOLUCAO_PENDENTE, DEVOLVIDO) e busca por código, reserva, material, responsável ou destino.")
    @ApiResponse(responseCode = "200", description = "Movimentações e contadores")
    @GetMapping
    public Movimentacoes listar(@RequestParam(required = false) String status,
                                @RequestParam(required = false) String busca) {
        return movimentacoes.listar(status, busca);
    }

    @Operation(summary = "Registrar retirada")
    @ApiResponse(responseCode = "200", description = "Retirada registrada")
    @ApiResponse(responseCode = "404", description = "Movimentação inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Material não está aguardando retirada ou reserva não aprovada", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/retirada")
    public MovimentacaoDto retirada(@PathVariable String codigo) {
        return movimentacoes.registrarRetirada(codigo, usuarioAtual.obter());
    }

    @Operation(summary = "Registrar devolução", description = "Gera o protocolo de devolução.")
    @ApiResponse(responseCode = "200", description = "Devolução registrada")
    @ApiResponse(responseCode = "404", description = "Movimentação inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Material não foi retirado", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/devolucao")
    public MovimentacaoDto devolucao(@PathVariable String codigo) {
        return movimentacoes.registrarDevolucao(codigo, usuarioAtual.obter());
    }
}
