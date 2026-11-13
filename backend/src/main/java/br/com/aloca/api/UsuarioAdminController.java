package br.com.aloca.api;

import br.com.aloca.api.dto.UsuarioAdminDtos.*;
import br.com.aloca.servico.UsuarioAdminService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

/** Tela 10 · Administração: usuários e perfis. Somente o perfil Administrador. */
@RestController
@Tag(name = "10 · Administração", description = "Cadastros, usuários e bloqueios de manutenção (RF-02, RF-08). Somente o perfil Administrador.")
@RequestMapping("/api/admin/usuarios")
@PreAuthorize("hasRole('ADMINISTRADOR')")
public class UsuarioAdminController {
    private final UsuarioAdminService servico;
    public UsuarioAdminController(UsuarioAdminService servico) { this.servico=servico; }

    @Operation(summary = "Listar usuários", description = "Com perfis, situação e o cadastro de professor ligado.")
    @GetMapping public List<Resposta> listar() { return servico.listar(); }

    @Operation(summary = "Consultar usuário")
    @GetMapping("/{id}") public Resposta consultar(@PathVariable Long id) { return servico.consultar(id); }

    @Operation(summary = "Criar usuário", description = "E-mail único; senha de 8 a 72 caracteres; pelo menos um perfil. professorId (opcional) liga a conta ao cadastro de professor.")
    @PostMapping @ResponseStatus(HttpStatus.CREATED)
    public Resposta criar(@Valid @RequestBody Cadastro p) { return servico.criar(p); }

    @Operation(summary = "Alterar dados e perfis", description = "Vale na hora: os tokens já emitidos deixam de valer. Mantém pelo menos um administrador ativo.")
    @PutMapping("/{id}") public Resposta editar(@PathVariable Long id,@Valid @RequestBody Edicao p) { return servico.editar(id,p); }

    @Operation(summary = "Ativar ou desativar usuário", description = "Desativar encerra o acesso na hora e mantém o histórico.")
    @PatchMapping("/{id}/ativo") public Resposta ativo(@PathVariable Long id,@Valid @RequestBody Ativacao p) { return servico.ativar(id,p.ativo()); }

    @Operation(summary = "Redefinir senha")
    @PutMapping("/{id}/senha") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void senha(@PathVariable Long id,@Valid @RequestBody Senha p) { servico.senha(id,p.senha()); }

    @Operation(summary = "Excluir usuário", description = "Só para conta sem histórico (criada por engano). Conta com histórico deve ser desativada, para manter a auditoria.")
    @DeleteMapping("/{id}") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluir(@PathVariable Long id) { servico.excluir(id); }
}
