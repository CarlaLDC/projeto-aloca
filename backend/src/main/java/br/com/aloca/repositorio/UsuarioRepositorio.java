package br.com.aloca.repositorio;

import br.com.aloca.dominio.Perfil;
import br.com.aloca.dominio.Usuario;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface UsuarioRepositorio extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmailIgnoreCase(String email);
    List<Usuario> findAllByOrderByNome();

    @org.springframework.data.jpa.repository.Lock(jakarta.persistence.LockModeType.PESSIMISTIC_WRITE)
    @Query("select u from Usuario u order by u.id")
    List<Usuario> travarAdministracao();

    @Query("select u from Usuario u join u.perfis p where p = :perfil and u.ativo = true")
    List<Usuario> comPerfil(Perfil perfil);
}
