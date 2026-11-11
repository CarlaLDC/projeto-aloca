package br.com.aloca.repositorio;

import br.com.aloca.dominio.Professor;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface ProfessorRepositorio extends JpaRepository<Professor, Long> {

    List<Professor> findByAtivoTrueOrderByNome();

    List<Professor> findAllByOrderByNome();

    Optional<Professor> findByUsuarioId(Long usuarioId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Professor p where p.id = :id")
    Optional<Professor> travar(Long id);
}
