package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.RelatorioDtos.Relatorio;
import br.com.aloca.servico.RelatorioService;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 11 · Relatórios (RF-12). */
@RestController
@Tag(name = "11 · Relatórios", description = "Utilização por recurso, carga horária e conflitos evitados (RF-12). Perfis Responsável e Administrador.")
@RequestMapping("/api/relatorios")
@PreAuthorize("hasAnyRole('RESPONSAVEL','ADMINISTRADOR')")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @Operation(summary = "Relatório do período", description = "Padrão: últimos 30 dias. Utilização = horas reservadas / horas de funcionamento (segunda a sábado).")
    @ApiResponse(responseCode = "200", description = "Relatório")
    @ApiResponse(responseCode = "400", description = "Período inválido ou maior que 12 meses", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping
    public Relatorio gerar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.gerar(inicio, fim);
    }

    @Operation(summary = "Relatório do período em CSV", description = "Separador \";\" e codificação UTF-8 com BOM (abre direto no Excel).")
    @ApiResponse(responseCode = "200", description = "Arquivo CSV")
    @ApiResponse(responseCode = "400", description = "Período inválido ou maior que 12 meses", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @GetMapping("/csv")
    public ResponseEntity<byte[]> csv(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        Relatorio rel = relatorios.gerar(inicio, fim);
        String nome = "relatorio-aloca-" + rel.inicio() + "-a-" + rel.fim() + ".csv";
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
            .contentType(new MediaType("text", "csv", StandardCharsets.UTF_8))
            .body(relatorios.csv(rel).getBytes(StandardCharsets.UTF_8));
    }
}
