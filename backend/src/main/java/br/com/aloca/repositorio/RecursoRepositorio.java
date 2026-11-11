package br.com.aloca.repositorio;

import br.com.aloca.dominio.Recurso;
import jakarta.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

public interface RecursoRepositorio extends JpaRepository<Recurso, Long> {

    List<Recurso> findByAtivoTrueOrderByNome();

    List<Recurso> findAllByOrderByNome();

    /**
     * Trava as linhas dos recursos até o fim da transação. Duas reservas
     * simultâneas para o mesmo recurso passam a ser processadas uma após a
     * outra, então a segunda enxerga a primeira e é recusada (RF-06).
     * A ordenação por id evita deadlock entre transações.
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from Recurso r where r.id in :ids order by r.id")
    List<Recurso> travar(Collection<Long> ids);
}
