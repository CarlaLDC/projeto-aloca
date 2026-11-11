package br.com.aloca.config;

import br.com.aloca.dominio.Perfil;
import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.UsuarioRepositorio;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Usuários de demonstração (as personas de src/docs/personas), criados quando o banco está vazio.
 * Desligue com aloca.dados-demo=false.
 */
@Component
@Order(1)
@ConditionalOnProperty(name = "aloca.dados-demo", havingValue = "true", matchIfMissing = true)
public class DadosUsuarios implements ApplicationRunner {

    public static final String SENHA_DEMO = "aloca123";
    public static final String UNIDADE = "Unidade Santo Amaro";
    private static final Logger log = LoggerFactory.getLogger(DadosUsuarios.class);

    private final UsuarioRepositorio usuarios;
    private final PasswordEncoder senhas;

    public DadosUsuarios(UsuarioRepositorio usuarios, PasswordEncoder senhas) {
        this.usuarios = usuarios;
        this.senhas = senhas;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (usuarios.count() > 0) return;
        criar("Ana Martins", "ana.martins@aloca.edu.br", Perfil.SOLICITANTE);
        criar("Mariana Costa", "mariana.costa@aloca.edu.br", Perfil.SOLICITANTE);
        criar("Roberto Almeida", "roberto.almeida@aloca.edu.br", Perfil.RESPONSAVEL);
        criar("Patrícia Souza", "patricia.souza@aloca.edu.br", Perfil.ADMINISTRADOR);
        criar("André Lima", "andre.lima@aloca.edu.br", Perfil.PROFESSOR);
        log.info("Usuários de demonstração criados. Senha de todos: {}", SENHA_DEMO);
    }

    private void criar(String nome, String email, Perfil perfil) {
        usuarios.save(new Usuario(nome, email, senhas.encode(SENHA_DEMO), UNIDADE, Set.of(perfil)));
    }
}
