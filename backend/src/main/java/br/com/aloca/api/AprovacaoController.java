package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.AprovacaoDtos.*;
import br.com.aloca.api.dto.ReservaDtos.ReservaDetalhe;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.AprovacaoService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 08 · Aprovações. Somente o Responsável aprova recursos restritos (RF-07). */
@RestController
@Tag(name = "08 · Aprovações", description = "Decisões do Responsável sobre recursos restritos (RF-07) e registro de não comparecimento. Somente o perfil Responsável.")
@RequestMapping("/api/aprovacoes")
@PreAuthorize("hasRole('RESPONSAVEL')")
public class AprovacaoController {

    private final AprovacaoService aprovacoes;
    private final UsuarioAtual usuarioAtual;

    public AprovacaoController(AprovacaoService aprovacoes, UsuarioAtual usuarioAtual) {
        this.aprovacoes = aprovacoes;
        this.usuarioAtual = usuarioAtual;
    }

    @Operation(summary = "Solicitações pendentes", description = "Inclui a verificação de disponibilidade de cada pedido e os contadores do dia.")
    @ApiResponse(responseCode = "200", description = "Pendências")
    @GetMapping
    public Aprovacoes listar() {
        return aprovacoes.listar();
    }

    @Operation(summary = "Aprovar solicitação", description = "Recusa se surgiu conflito (ex.: manutenção cadastrada depois do pedido) ou se o horário já passou.")
    @ApiResponse(responseCode = "200", description = "Reserva aprovada")
    @ApiResponse(responseCode = "404", description = "Código inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Já decidida, com conflito ou com horário vencido", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/aprovar")
    public ReservaDetalhe aprovar(@PathVariable String codigo) {
        return aprovacoes.aprovar(codigo, usuarioAtual.obter());
    }

    /** O solicitante não compareceu: encerra a reserva e libera o horário restante. */
    @Operation(summary = "Registrar não comparecimento", description = "Para reserva Aprovada ou Em uso, de 15 minutos após o início até o fim, e sem material retirado. O horário restante fica livre. Corpo opcional: {\"observacao\": \"...\"}. Mesma regra de POST /api/reservas/{codigo}/nao-compareceu.")
    @ApiResponse(responseCode = "200", description = "Reserva marcada como Não compareceu")
    @ApiResponse(responseCode = "400", description = "Observação com mais de 500 caracteres", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Código inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Fora da janela (15 min após o início até o fim), status que não permite ou material já retirado", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/nao-compareceu")
    public ReservaDetalhe naoCompareceu(@PathVariable String codigo,
                                        @Valid @RequestBody(required = false) NaoComparecimentoRequest pedido) {
        return aprovacoes.registrarNaoComparecimento(codigo, pedido == null ? null : pedido.observacao(), usuarioAtual.obter());
    }

    @Operation(summary = "Rejeitar solicitação com justificativa")
    @ApiResponse(responseCode = "200", description = "Reserva rejeitada")
    @ApiResponse(responseCode = "400", description = "Justificativa em branco", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Código inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Já decidida", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/rejeitar")
    public ReservaDetalhe rejeitar(@PathVariable String codigo, @Valid @RequestBody RejeicaoRequest pedido) {
        return aprovacoes.rejeitar(codigo, pedido.justificativa(), usuarioAtual.obter());
    }
}
