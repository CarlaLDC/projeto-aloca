package br.com.aloca.api;

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
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class AdminController {

    private final AdminService admin;
    private final UsuarioAtual usuarioAtual;

    public AdminController(AdminService admin, UsuarioAtual usuarioAtual) {
        this.admin = admin;
        this.usuarioAtual = usuarioAtual;
    }

    @GetMapping("/recursos")
    public List<RecursoDto> recursos() { return admin.listarRecursos(); }

    @PostMapping("/recursos")
    @ResponseStatus(HttpStatus.CREATED)
    public RecursoDto criarRecurso(@Valid @RequestBody RecursoRequest p) { return admin.criarRecurso(p); }

    @PutMapping("/recursos/{id}")
    public RecursoDto alterarRecurso(@PathVariable Long id, @Valid @RequestBody RecursoRequest p) {
        return admin.alterarRecurso(id, p);
    }

    @PostMapping("/recursos/{id}/desativar")
    public RecursoDto desativarRecurso(@PathVariable Long id) { return admin.definirAtivo(id, false); }

    @PostMapping("/recursos/{id}/ativar")
    public RecursoDto ativarRecurso(@PathVariable Long id) { return admin.definirAtivo(id, true); }

    @GetMapping("/professores")
    public List<ProfessorDto> professores() { return admin.listarProfessores(); }

    @PostMapping("/professores")
    @ResponseStatus(HttpStatus.CREATED)
    public ProfessorDto criarProfessor(@Valid @RequestBody ProfessorRequest p) { return admin.criarProfessor(p); }

    @PutMapping("/professores/{id}")
    public ProfessorDto alterarProfessor(@PathVariable Long id, @Valid @RequestBody ProfessorRequest p) {
        return admin.alterarProfessor(id, p);
    }

    @PostMapping("/professores/{id}/desativar")
    public ProfessorDto desativarProfessor(@PathVariable Long id) { return admin.definirProfessorAtivo(id, false); }

    @PostMapping("/professores/{id}/ativar")
    public ProfessorDto ativarProfessor(@PathVariable Long id) { return admin.definirProfessorAtivo(id, true); }

    @GetMapping("/bloqueios")
    public List<BloqueioDto> bloqueios() { return admin.listarBloqueios(); }

    @PostMapping("/bloqueios")
    @ResponseStatus(HttpStatus.CREATED)
    public BloqueioDto bloquear(@Valid @RequestBody BloqueioRequest p) { return admin.bloquear(p, usuarioAtual.obter()); }

    @DeleteMapping("/bloqueios/{id}")
    public ResponseEntity<Void> removerBloqueio(@PathVariable Long id) {
        admin.removerBloqueio(id);
        return ResponseEntity.noContent().build();
    }
}
