package br.com.aloca.repositorio;

import br.com.aloca.dominio.Bloqueio;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface BloqueioRepositorio extends JpaRepository<Bloqueio, Long> {

    @Query("""
        select b from Bloqueio b
        where b.recurso.id in :recursoIds and b.inicio < :fim and b.fim > :inicio
        order by b.inicio""")
    List<Bloqueio> sobrepostos(Collection<Long> recursoIds, LocalDateTime inicio, LocalDateTime fim);

    @Query("select b from Bloqueio b where b.inicio < :fim and b.fim > :inicio")
    List<Bloqueio> noIntervalo(LocalDateTime inicio, LocalDateTime fim);

    List<Bloqueio> findAllByOrderByInicioDesc();
}
