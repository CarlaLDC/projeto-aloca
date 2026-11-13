package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.PainelDtos.Painel;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.PainelService;
import org.springframework.web.bind.annotation.*;

/** Tela 02 · Visão geral. */
@RestController
@Tag(name = "02 · Visão geral", description = "Números do dia e agenda do usuário.")
@RequestMapping("/api/painel")
public class PainelController {

    private final PainelService painel;
    private final UsuarioAtual usuarioAtual;

    public PainelController(PainelService painel, UsuarioAtual usuarioAtual) {
        this.painel = painel;
        this.usuarioAtual = usuarioAtual;
    }

    @Operation(summary = "Painel do dia", description = "Reservas de hoje, recursos disponíveis, pendências e a agenda do usuário (para o Professor, a agenda em que ele está alocado, RF-14). Qualquer perfil.")
    @ApiResponse(responseCode = "200", description = "Painel montado")
    @GetMapping
    public Painel painel() {
        return painel.montar(usuarioAtual.obter());
    }
}
