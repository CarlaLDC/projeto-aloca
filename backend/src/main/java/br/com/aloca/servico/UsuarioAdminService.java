package br.com.aloca.servico;

import br.com.aloca.api.dto.UsuarioAdminDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.seguranca.UsuarioAtual;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UsuarioAdminService {
    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;
    private final AuditoriaService auditoria;
    private final UsuarioAtual atual;
    public UsuarioAdminService(UsuarioRepositorio usuarios, PasswordEncoder senhas, AuditoriaService auditoria, UsuarioAtual atual) {
        this.usuarios=usuarios; this.senhas=senhas; this.auditoria=auditoria; this.atual=atual;
    }
    @Transactional(readOnly=true)
    public List<Resposta> listar() { return usuarios.findAllByOrderByNome().stream().map(Resposta::de).toList(); }
    @Transactional
    public Resposta criar(Cadastro p) {
        usuarios.travarAdministracao();
        String email=email(p.email()); validarEmail(email,null); validarSenha(p.senha());
        Usuario u=usuarios.save(new Usuario(p.nome().strip(),email,senhas.encode(p.senha()),p.unidade().strip(),p.perfis()));
        registrar(u,"CRIAR"); return Resposta.de(u);
    }
    @Transactional
    public Resposta editar(Long id, Edicao p) {
        usuarios.travarAdministracao(); Usuario u=buscar(id);
        validarEmail(email(p.email()),id);
        protegerAdministrador(u,u.isAtivo(),p.perfis());
        u.atualizar(p.nome().strip(),email(p.email()),p.unidade().strip(),p.perfis());
        registrar(u,"ATUALIZAR"); return Resposta.de(u);
    }
    @Transactional
    public Resposta ativar(Long id, boolean ativo) {
        usuarios.travarAdministracao(); Usuario u=buscar(id);
        protegerAdministrador(u,ativo,u.getPerfis());
        u.setAtivo(ativo); registrar(u,ativo?"ATIVAR":"DESATIVAR"); return Resposta.de(u);
    }
    @Transactional
    public void senha(Long id, String senha) {
        usuarios.travarAdministracao(); Usuario u=buscar(id); validarSenha(senha);
        u.trocarSenha(senhas.encode(senha)); registrar(u,"REDEFINIR_SENHA");
    }
    private void protegerAdministrador(Usuario u, boolean ativo, Set<Perfil> perfis) {
        if (u.possui(Perfil.ADMINISTRADOR) && (!ativo || !perfis.contains(Perfil.ADMINISTRADOR))) {
            if (u.getId().equals(atual.obter().getId()))
                throw RegraNegocioException.conflito("Você não pode remover seu próprio acesso de administrador.");
            if (usuarios.comPerfil(Perfil.ADMINISTRADOR).stream().noneMatch(x -> !x.getId().equals(u.getId())))
                throw RegraNegocioException.conflito("Mantenha pelo menos um administrador ativo.");
        }
    }
    private void validarEmail(String email, Long id) {
        usuarios.findByEmailIgnoreCase(email).filter(u->!Objects.equals(u.getId(),id)).ifPresent(u->{
            throw RegraNegocioException.conflito("Este e-mail já está cadastrado.");
        });
    }
    private void validarSenha(String senha) {
        if (senha.getBytes(StandardCharsets.UTF_8).length>72)
            throw RegraNegocioException.invalida("A senha deve ocupar no máximo 72 bytes UTF-8.");
    }
    private String email(String e) { return e.strip().toLowerCase(Locale.ROOT); }
    private Usuario buscar(Long id) { return usuarios.findById(id).orElseThrow(()->RegraNegocioException.naoEncontrada("Usuário não encontrado.")); }
    private void registrar(Usuario u,String acao) { auditoria.registrar(atual.obter(),acao,"USUARIO",u.getId(),"Gestão de usuário: "+u.getNome()); }
}
