package br.com.aloca.api;

import br.com.aloca.api.dto.DisponibilidadeDtos.Disponibilidade;
import br.com.aloca.dominio.TipoRecurso;
import br.com.aloca.servico.DisponibilidadeService;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

/** Tela 03 · Disponibilidade. */
@RestController
@RequestMapping("/api/disponibilidade")
public class DisponibilidadeController {

    private final DisponibilidadeService disponibilidade;

    public DisponibilidadeController(DisponibilidadeService disponibilidade) {
        this.disponibilidade = disponibilidade;
    }

    @GetMapping
    public Disponibilidade consultar(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) TipoRecurso tipo,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) Integer capacidadeMinima) {
        return disponibilidade.consultar(data, tipo, busca, capacidadeMinima);
    }
}
