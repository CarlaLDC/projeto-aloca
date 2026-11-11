package br.com.aloca.repositorio;

import br.com.aloca.dominio.HistoricoReserva;
import br.com.aloca.dominio.StatusReserva;
import java.time.LocalDateTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface HistoricoReservaRepositorio extends JpaRepository<HistoricoReserva, Long> {

    /** Decisões tomadas por uma pessoa (exclui aprovações automáticas do sistema). */
    @Query("""
        select count(h) from HistoricoReserva h
        where h.estadoNovo = :estado and h.estadoAnterior = br.com.aloca.dominio.StatusReserva.SOLICITADA
          and h.usuario is not null and h.dataHora >= :desde""")
    long decisoesDesde(StatusReserva estado, LocalDateTime desde);
}
