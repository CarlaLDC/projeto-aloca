package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.DisponibilidadeDtos.Disponibilidade;
import br.com.aloca.api.dto.DisponibilidadeDtos.DisponibilidadeProfessores;
import br.com.aloca.dominio.TipoRecurso;
import br.com.aloca.servico.DisponibilidadeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Tela 03 · Disponibilidade. */
@RestController
@Tag(name = "03 · Disponibilidade", description = "Pesquisa de recursos e professores com horários livres (RF-03).")
@RequestMapping("/api/disponibilidade")
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidade;

    public DisponibilidadeController(DisponibilidadeService disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    @Operation(summary = "Recursos e horários livres do dia", description = "Filtros opcionais: tipo (SALA, LABORATORIO, EQUIPAMENTO), busca por nome ou localização (sem diferenciar acentos) e capacidade mínima. O resumo conta todos os recursos do dia. Qualquer perfil.")
    @ApiResponse(responseCode = "200", description = "Recursos do dia")
    @ApiResponse(responseCode = "400", description = "Data ou tipo em formato inválido", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping
    public Disponibilidade consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer capacidadeMinima) {
        return disponibilidade.consultar(data, tipo, busca, capacidadeMinima);
    }

    /** Professores com a agenda do dia, filtrados por competência e por nome (RF-03). */
    @Operation(summary = "Professores por competência e agenda do dia", description = "Filtros opcionais: competência e busca por nome ou competência (sem diferenciar acentos). Qualquer perfil.")
    @ApiResponse(responseCode = "200", description = "Professores do dia")
    @ApiResponse(responseCode = "400", description = "Data em formato inválido", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping("/professores")
    public DisponibilidadeProfessores professores(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String competencia,
            @RequestParam(required = false) String busca) {
        return disponibilidade.consultarProfessores(data, competencia, busca);
    }
}
