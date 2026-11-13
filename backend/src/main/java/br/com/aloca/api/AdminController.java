package br.com.aloca.api;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import br.com.aloca.api.dto.AdminDtos.*;
import br.com.aloca.seguranca.UsuarioAtual;
import br.com.aloca.servico.AdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/**
 * Administração (RF-02 e RF-08). O Figma tem a tela "10 · Administração", mas o
 * front ainda não tem a rota; estes endpoints já ficam prontos para ela.
 */
@RestController
@Tag(name = "10 · Administração", description = "Cadastros e bloqueios de manutenção (RF-02, RF-08). Somente o perfil Administrador.")
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    private final AdminService admin;
    private final UsuarioAtual usuarioAtual;

    public AdminController(AdminService admin, UsuarioAtual usuarioAtual) {
        this.admin = admin;
        this.usuarioAtual = usuarioAtual;
    }

    @Operation(summary = "Listar salas, laboratórios e equipamentos")
    @ApiResponse(responseCode = "200", description = "Recursos")
    @GetMapping("/recursos")
    public List<RecursoDto> recursos() { return admin.listarRecursos(); }

    @Operation(summary = "Cadastrar recurso")
    @ApiResponse(responseCode = "201", description = "Recurso cadastrado")
    @ApiResponse(responseCode = "400", description = "Campos obrigatórios ou horário de funcionamento inválidos", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoDto criarRecurso(@Valid @RequestBody RecursoRequest p) { return admin.criarRecurso(p); }

    @Operation(summary = "Alterar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso alterado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PutMapping("/recursos/{id}")
    public RecursoDto alterarRecurso(@PathVariable Long id, @Valid @RequestBody RecursoRequest p) {
        return admin.alterarRecurso(id, p);
    }

    @Operation(summary = "Desativar recurso", description = "O recurso deixa de aparecer nas pesquisas e não pode ser reservado; o histórico é mantido.")
    @ApiResponse(responseCode = "200", description = "Recurso desativado")
    @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/recursos/{id}/desativar")
    public RecursoDto desativarRecurso(@PathVariable Long id) { return admin.definirAtivo(id, false); }

    @Operation(summary = "Reativar recurso")
    @ApiResponse(responseCode = "200", description = "Recurso ativado")
    @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/recursos/{id}/ativar")
    public RecursoDto ativarRecurso(@PathVariable Long id) { return admin.definirAtivo(id, true); }

    @Operation(summary = "Listar professores")
    @ApiResponse(responseCode = "200", description = "Professores")
    @GetMapping("/professores")
    public List<ProfessorDto> professores() { return admin.listarProfessores(); }

    @Operation(summary = "Cadastrar professor")
    @ApiResponse(responseCode = "201", description = "Professor cadastrado")
    @ApiResponse(responseCode = "400", description = "Campos obrigatórios em branco", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorDto criarProfessor(@Valid @RequestBody ProfessorRequest p) { return admin.criarProfessor(p); }

    @Operation(summary = "Alterar professor")
    @ApiResponse(responseCode = "200", description = "Professor alterado")
    @ApiResponse(responseCode = "400", description = "Dados inválidos", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Professor inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PutMapping("/professores/{id}")
    public ProfessorDto alterarProfessor(@PathVariable Long id, @Valid @RequestBody ProfessorRequest p) {
        return admin.alterarProfessor(id, p);
    }

    @Operation(summary = "Desativar professor")
    @ApiResponse(responseCode = "200", description = "Professor desativado")
    @ApiResponse(responseCode = "404", description = "Professor inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/professores/{id}/desativar")
    public ProfessorDto desativarProfessor(@PathVariable Long id) { return admin.definirProfessorAtivo(id, false); }

    @Operation(summary = "Reativar professor")
    @ApiResponse(responseCode = "200", description = "Professor ativado")
    @ApiResponse(responseCode = "404", description = "Professor inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/professores/{id}/ativar")
    public ProfessorDto ativarProfessor(@PathVariable Long id) { return admin.definirProfessorAtivo(id, true); }

    @Operation(summary = "Listar bloqueios de manutenção")
    @ApiResponse(responseCode = "200", description = "Bloqueios")
    @GetMapping("/bloqueios")
    public List<BloqueioDto> bloqueios() { return admin.listarBloqueios(); }

    @Operation(summary = "Bloquear recurso para manutenção", description = "Durante o período, o recurso não pode ser reservado (RF-08). Reservas Solicitadas ou Aprovadas que usam o recurso no período são canceladas, com auditoria e aviso ao solicitante; a resposta lista os códigos. Se houver reserva Em uso no período, o bloqueio é recusado (409).")
    @ApiResponse(responseCode = "201", description = "Bloqueio criado")
    @ApiResponse(responseCode = "400", description = "Período inválido ou já terminado", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "404", description = "Recurso inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @ApiResponse(responseCode = "409", description = "Reserva em uso no período ou material já retirado", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioDto bloquear(@Valid @RequestBody BloqueioRequest p) { return admin.bloquear(p, usuarioAtual.obter()); }

    @Operation(summary = "Remover bloqueio")
    @ApiResponse(responseCode = "204", description = "Bloqueio removido")
    @ApiResponse(responseCode = "404", description = "Bloqueio inexistente", content = @Content(schema = @Schema(implementation = TratadorErros.Erro.class)))
    @DeleteMapping("/bloqueios/{id}")
    public ResponseEntity<Void> removerBloqueio(@PathVariable Long id) {
        admin.removerBloqueio(id);
        return ResponseEntity.noContent().build();
    }
}
