package br.com.aloca.api;

import br.com.aloca.api.dto.UsuarioAdminDtos.*;
import br.com.aloca.servico.UsuarioAdminService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioAdminController {
    private final UsuarioAdminService servico;
    public UsuarioAdminController(UsuarioAdminService servico) { this.servico=servico; }
    @GetMapping public List<Resposta> listar() { return servico.listar(); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Resposta criar(@Valid @RequestBody Cadastro p) { return servico.criar(p); }
    @PutMapping("/{id}") public Resposta editar(@PathVariable Long id,@Valid @RequestBody Edicao p) { return servico.editar(id,p); }
    @PatchMapping("/{id}/ativo") public Resposta ativo(@PathVariable Long id,@Valid @RequestBody Ativacao p) { return servico.ativar(id,p.ativo()); }
    @PutMapping("/{id}/senha") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void senha(@PathVariable Long id,@Valid @RequestBody Senha p) { servico.senha(id,p.senha()); }
}
