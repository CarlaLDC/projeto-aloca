package br.com.aloca.servico;

import br.com.aloca.api.dto.UsuarioAdminDtos.*;
import br.com.aloca.dominio.*;
import br.com.aloca.repositorio.ProfessorRepositorio;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.seguranca.UsuarioAtual;
import jakarta.persistence.EntityManager;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Tela 10 · Administração: usuários e perfis. Toda operação fica na auditoria administrativa e as mudanças
 * de perfil, senha ou situação invalidam os tokens já emitidos (versão do token).
 */
@Service
public class UsuarioAdminService {
    private final UsuarioRepositorio usuarios;
    private final ProfessorRepositorio professores;
    private final PasswordEncoder senhas;
    private final AuditoriaService auditoria;
    private final UsuarioAtual atual;
    private final EntityManager banco;
    public UsuarioAdminService(UsuarioRepositorio usuarios, ProfessorRepositorio professores, PasswordEncoder senhas,
                               AuditoriaService auditoria, UsuarioAtual atual, EntityManager banco) {
        this.usuarios=usuarios; this.professores=professores; this.senhas=senhas; this.auditoria=auditoria; this.atual=atual;
        this.banco=banco;
    }
    @Transactional(readOnly=true)
    public List<Resposta> listar() { return usuarios.findAllByOrderByNome().stream().map(this::resposta).toList(); }
    @Transactional(readOnly=true)
    public Resposta consultar(Long id) { return resposta(buscar(id)); }
    @Transactional
    public Resposta criar(Cadastro p) {
        usuarios.travarAdministracao();
        String email=email(p.email()); validarEmail(email,null); validarSenha(p.senha());
        Professor professor = professorParaVincular(p.perfis(), p.professorId(), null);
        Usuario u=usuarios.save(new Usuario(p.nome().strip(),email,senhas.encode(p.senha()),p.unidade().strip(),p.perfis()));
        if (professor != null) professor.vincularUsuario(u);
        registrar(u,"CRIAR"); return resposta(u);
    }
    @Transactional
    public Resposta editar(Long id, Edicao p) {
        usuarios.travarAdministracao(); Usuario u=buscar(id);
        validarEmail(email(p.email()),id);
        protegerAdministrador(u,u.isAtivo(),p.perfis());
        Professor novo = professorParaVincular(p.perfis(), p.professorId(), u);
        u.atualizar(p.nome().strip(),email(p.email()),p.unidade().strip(),p.perfis());
        professores.findByUsuarioId(u.getId())
            .filter(atualVinculo -> !p.perfis().contains(Perfil.PROFESSOR) || (novo != null && !atualVinculo.getId().equals(novo.getId())))
            .ifPresent(atualVinculo -> atualVinculo.vincularUsuario(null));
        if (novo != null) novo.vincularUsuario(u);
        registrar(u,"ATUALIZAR"); return resposta(u);
    }
    @Transactional
    public Resposta ativar(Long id, boolean ativo) {
        usuarios.travarAdministracao(); Usuario u=buscar(id);
        protegerAdministrador(u,ativo,u.getPerfis());
        u.setAtivo(ativo); registrar(u,ativo?"ATIVAR":"DESATIVAR"); return resposta(u);
    }
    @Transactional
    public void senha(Long id, String senha) {
        usuarios.travarAdministracao(); Usuario u=buscar(id); validarSenha(senha);
        u.trocarSenha(senhas.encode(senha)); registrar(u,"REDEFINIR_SENHA");
    }

    /** Registros que ligam a conta ao histórico do sistema: com eles, a conta só pode ser desativada. */
    private static final List<Map.Entry<String, String>> REFERENCIAS = List.of(
        Map.entry("reservas solicitadas", "select count(r) from Reserva r where r.solicitante.id = :id"),
        Map.entry("eventos de auditoria de reservas", "select count(h) from HistoricoReserva h where h.usuario.id = :id"),
        Map.entry("movimentações de material",
            "select count(m) from Movimentacao m where m.retiradaRegistradaPor.id = :id or m.devolucaoRegistradaPor.id = :id"),
        Map.entry("bloqueios de manutenção", "select count(b) from Bloqueio b where b.criadoPor.id = :id"),
        Map.entry("conflitos registrados", "select count(c) from ConflitoRegistrado c where c.solicitante.id = :id"),
        Map.entry("operações administrativas", "select count(e) from EventoAuditoria e where e.usuario.id = :id"));

    /** Exclui de vez uma conta criada por engano, que ainda não participou de nada (a auditoria fica íntegra). */
    @Transactional
    public void excluir(Long id) {
        usuarios.travarAdministracao(); Usuario u=buscar(id);
        if (u.getId().equals(atual.obter().getId()))
            throw RegraNegocioException.conflito("Você não pode excluir a própria conta.");
        protegerAdministrador(u,false,Set.of());
        List<String> usos=new ArrayList<>();
        for (Map.Entry<String, String> ref : REFERENCIAS) {
            long n=banco.createQuery(ref.getValue(), Long.class).setParameter("id", id).getSingleResult();
            if (n>0) usos.add(n+" "+ref.getKey());
        }
        if (!usos.isEmpty())
            throw RegraNegocioException.conflito("Esta conta tem histórico ("+String.join(", ",usos)
                +") e não pode ser excluída. Desative a conta para manter a auditoria.");
        professores.findByUsuarioId(id).ifPresent(p -> p.vincularUsuario(null));
        banco.createQuery("delete from Notificacao n where n.destinatario.id = :id").setParameter("id", id).executeUpdate();
        auditoria.registrar(atual.obter(),"EXCLUIR","USUARIO",id,"Conta excluída: "+u.getEmail());
        usuarios.delete(u);
    }

    private Professor professorParaVincular(Set<Perfil> perfis, Long professorId, Usuario usuario) {
        if (professorId == null) return null;
        if (!perfis.contains(Perfil.PROFESSOR))
            throw RegraNegocioException.invalida("Só é possível ligar um cadastro de professor a contas com o perfil Professor.");
        Professor professor=professores.findById(professorId)
            .orElseThrow(()->RegraNegocioException.naoEncontrada("Professor não encontrado."));
        Usuario dono=professor.getUsuario();
        if (dono!=null && (usuario==null || !dono.getId().equals(usuario.getId())))
            throw RegraNegocioException.conflito(professor.getNome()+" já está ligado à conta "+dono.getEmail()+".");
        return professor;
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
    private Resposta resposta(Usuario u) { return Resposta.de(u, professores.findByUsuarioId(u.getId()).orElse(null)); }
    private String email(String e) { return e.strip().toLowerCase(Locale.ROOT); }
    private Usuario buscar(Long id) { return usuarios.findById(id).orElseThrow(()->RegraNegocioException.naoEncontrada("Usuário não encontrado.")); }
    private void registrar(Usuario u,String acao) { auditoria.registrar(atual.obter(),acao,"USUARIO",u.getId(),"Gestão de usuário: "+u.getNome()); }
}
