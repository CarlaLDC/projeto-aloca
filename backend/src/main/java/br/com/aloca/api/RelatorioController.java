package br.com.aloca.api;

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
@RequestMapping("/api/relatorios")
@PreAuthorize("hasAnyRole('RESPONSAVEL','ADMINISTRADOR')")
public class RelatorioController {

    private final RelatorioService relatorios;

    public RelatorioController(RelatorioService relatorios) {
        this.relatorios = relatorios;
    }

    @GetMapping
    public Relatorio gerar(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
                           @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        return relatorios.gerar(inicio, fim);
    }

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
