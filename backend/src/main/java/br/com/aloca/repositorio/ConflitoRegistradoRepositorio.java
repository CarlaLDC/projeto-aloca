package br.com.aloca.repositorio;

import br.com.aloca.dominio.ConflitoRegistrado;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ConflitoRegistradoRepositorio extends JpaRepository<ConflitoRegistrado, Long> {

    /** Tentativas recusadas registradas dentro do período [inicio, fim). */
    @Query("select c from ConflitoRegistrado c where c.registradoEm >= :inicio and c.registradoEm < :fim")
    List<ConflitoRegistrado> registradosEntre(LocalDateTime inicio, LocalDateTime fim);
}
