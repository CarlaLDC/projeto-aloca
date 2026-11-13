package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
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
@Tag(name = "04 a 07 · Reservas", description = "Criar, consultar, alterar e cancelar reservas, com detecção de conflitos e histórico (RF-04, RF-05, RF-06, RF-10).")
@RequestMapping("/api/reservas")
public class ReservaController {

    private final ReservaService reservas;
    private final UsuarioAtual usuarioAtual;

    public ReservaController(ReservaService reservas, UsuarioAtual usuarioAtual) {
        this.reservas = reservas;
        this.usuarioAtual = usuarioAtual;
    }

    /** Listas de salas, professores e materiais para os campos da tela de nova reserva. */
    @Operation(summary = "Salas, professores e materiais para a nova reserva", description = "Somente recursos e professores ativos. Qualquer perfil.")
    @ApiResponse(responseCode = "200", description = "Listas de opções")
    @GetMapping("/opcoes")
    public OpcoesReserva opcoes() {
        return reservas.opcoes(usuarioAtual.obter());
    }

    @Operation(summary = "Minhas reservas", description = "Reservas pedidas pelo usuário e, para o Professor, as reservas em que ele está alocado.")
    @ApiResponse(responseCode = "200", description = "Lista de reservas")
    @GetMapping("/minhas")
    public List<ReservaResumo> minhas() {
        return reservas.minhas(usuarioAtual.obter());
    }

    @Operation(summary = "Detalhe e histórico auditável da reserva", description = "Visível para quem pediu, para o professor alocado, para o Responsável e para o Administrador.")
    @ApiResponse(responseCode = "200", description = "Reserva com histórico")
    @ApiResponse(responseCode = "403", description = "Reserva de outra pessoa", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Código inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping("/{codigo}")
    public ReservaDetalhe detalhe(@PathVariable String codigo) {
        return reservas.detalhe(codigo, usuarioAtual.obter());
    }

    /** 201 com a reserva criada, ou 409 com os conflitos e alternativas (tela 05). */
    @Operation(summary = "Criar reserva", description = "Perfis: Solicitante, Responsável ou Administrador. Professores e coordenadores reservam com o perfil Solicitante (um professor pode ter os perfis Professor e Solicitante). Verifica horário válido, horário de funcionamento, manutenção e sobreposição de sala, materiais e professor. Recursos restritos ficam SOLICITADA até o Responsável decidir; os demais são aprovados na hora. Pedidos simultâneos para o mesmo recurso resultam em uma única reserva aceita (RF-06).")
    @ApiResponse(responseCode = "201", description = "Reserva criada", content = @Content(schema = @Schema(implementation = ReservaDetalhe.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos (ex.: término antes do início)", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de horário: devolve os conflitos e até 3 alternativas livres", content = @Content(schema = @Schema(implementation = ConflitoResposta.class)))
    @PostMapping
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR','PROFESSOR')")
    public ResponseEntity<?> criar(@Valid @RequestBody ReservaRequest pedido) {
        return responder(reservas.criar(pedido, usuarioAtual.obter()), HttpStatus.CREATED);
    }

    @Operation(summary = "Alterar reserva", description = "Somente quem pediu, antes do início. Os conflitos são verificados de novo; recurso restrito volta para aprovação.")
    @ApiResponse(responseCode = "200", description = "Reserva alterada", content = @Content(schema = @Schema(implementation = ReservaDetalhe.class)))
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "403", description = "Reserva de outra pessoa", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Conflito de horário ou reserva que não pode mais ser alterada", content = @Content(schema = @Schema(implementation = ConflitoResposta.class)))
    @PutMapping("/{codigo}")
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR','PROFESSOR')")
    public ResponseEntity<?> alterar(@PathVariable String codigo, @Valid @RequestBody ReservaRequest pedido) {
        return responder(reservas.alterar(codigo, pedido, usuarioAtual.obter()), HttpStatus.OK);
    }

    @Operation(summary = "Cancelar reserva", description = "Somente quem pediu, antes do início e com motivo. Reservas em uso não podem ser canceladas.")
    @ApiResponse(responseCode = "200", description = "Reserva cancelada")
    @ApiResponse(responseCode = "400", description = "Motivo em branco", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "403", description = "Reserva de outra pessoa", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Reserva já iniciada, concluída, rejeitada ou cancelada", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/{codigo}/cancelamento")
    @PreAuthorize("hasAnyRole('SOLICITANTE','RESPONSAVEL','ADMINISTRADOR','PROFESSOR')")
    public ReservaDetalhe cancelar(@PathVariable String codigo, @Valid @RequestBody CancelamentoRequest pedido) {
        return reservas.cancelar(codigo, pedido.motivo(), usuarioAtual.obter());
    }

    /** Responsável registra ausência documentada, respeitando janela e retirada de materiais. */
    @Operation(summary = "Registrar não comparecimento (com motivo)", description = "Somente o Responsável. De 15 minutos após o início até o fim, para reservas aprovadas ou em uso, sem material retirado. Mesma regra de POST /api/aprovacoes/{codigo}/nao-compareceu.")
    @PostMapping("/{codigo}/nao-compareceu")
    @PreAuthorize("hasRole('RESPONSAVEL')")
    public ReservaDetalhe naoCompareceu(@PathVariable String codigo, @Valid @RequestBody CancelamentoRequest pedido) {
        return reservas.marcarNaoComparecimento(codigo, pedido.motivo(), usuarioAtual.obter());
    }

    private static ResponseEntity<?> responder(Resultado resultado, HttpStatus sucesso) {
        return switch (resultado) {
            case Gravada g -> ResponseEntity.status(sucesso).body(g.reserva());
            case EmConflito c -> ResponseEntity.status(HttpStatus.CONFLICT).body(c.conflito());
        };
    }
}
