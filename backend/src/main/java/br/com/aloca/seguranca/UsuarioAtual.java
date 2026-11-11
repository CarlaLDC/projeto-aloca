package br.com.aloca.seguranca;

import br.com.aloca.dominio.Usuario;
import br.com.aloca.repositorio.UsuarioRepositorio;
import br.com.aloca.servico.RegraNegocioException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/** Carrega o usuário dono do token da requisição atual. */
@Component
public class UsuarioAtual {

    private final UsuarioRepositorio usuarios;

    public UsuarioAtual(UsuarioRepositorio usuarios) {
        this.usuarios = usuarios;
    }

    public Usuario obter() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getName() == null) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Entre no sistema para continuar.");
        }
        long id;
        try {
            id = Long.parseLong(auth.getName());
        } catch (NumberFormatException e) {
            throw new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Entre no sistema para continuar.");
        }
        return usuarios.findById(id)
            .filter(Usuario::isAtivo)
            .orElseThrow(() -> new RegraNegocioException(HttpStatus.UNAUTHORIZED, "Usuário inativo ou inexistente."));
    }
}
