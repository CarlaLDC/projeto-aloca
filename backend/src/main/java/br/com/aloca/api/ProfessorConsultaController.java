package br.com.aloca.api;

import br.com.aloca.api.dto.AdminDtos.ProfessorDto;
import br.com.aloca.repositorio.ProfessorRepositorio;
import java.util.List;
import java.util.Locale;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

/** Consulta de docentes ativos por nome ou competência, disponível a usuários autenticados. */
@RestController
@Tag(name = "03 · Disponibilidade", description = "Pesquisa de recursos e professores (RF-03).")
@RequestMapping("/api/professores")
public class ProfessorConsultaController {
    private final ProfessorRepositorio professores;

    public ProfessorConsultaController(ProfessorRepositorio professores) {
        this.professores = professores;
    }

    @Operation(summary = "Buscar professores ativos por nome ou competência", description = "Qualquer usuário autenticado.")
    @GetMapping
    public List<ProfessorDto> buscar(@RequestParam(required = false) String competencia) {
        String termo = competencia == null ? "" : competencia.strip().toLowerCase(Locale.ROOT);
        return professores.findByAtivoTrueOrderByNome().stream()
            .filter(p -> termo.isEmpty() || p.getCompetencia().toLowerCase(Locale.ROOT).contains(termo)
                || p.getNome().toLowerCase(Locale.ROOT).contains(termo))
            .map(p -> new ProfessorDto(p.getId(), p.getNome(), p.getCompetencia(), true))
            .toList();
    }
}
