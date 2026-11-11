package br.com.aloca.repositorio;

import br.com.aloca.dominio.Reserva;
import br.com.aloca.dominio.StatusReserva;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservaRepositorio extends JpaRepository<Reserva, Long> {

    Optional<Reserva> findByCodigo(String codigo);

    /**
     * Reservas ativas que se sobrepõem ao intervalo e usam a mesma sala,
     * algum dos mesmos materiais ou o mesmo professor (RF-05). Intervalos
     * consecutivos (fim == início) não se sobrepõem.
     */
    @Query("""
        select distinct r from Reserva r left join r.materiais m
        where r.data = :data
          and r.status in :ativos
          and r.inicio < :fim and r.fim > :inicio
          and r.id <> :ignorarId
          and (r.espaco.id in :recursoIds or m.id in :recursoIds or r.professor.id = :professorId)
        order by r.inicio""")
    List<Reserva> conflitantes(LocalDate data, LocalTime inicio, LocalTime fim, Collection<Long> recursoIds,
                               Long professorId, Long ignorarId, Collection<StatusReserva> ativos);

    @Query("select r from Reserva r where r.data = :data and r.status in :status order by r.inicio")
    List<Reserva> doDia(LocalDate data, Collection<StatusReserva> status);

    @Query("select r from Reserva r where r.data between :inicio and :fim and r.status in :status")
    List<Reserva> noPeriodo(LocalDate inicio, LocalDate fim, Collection<StatusReserva> status);

    List<Reserva> findBySolicitanteIdOrderByDataDescInicioDesc(Long solicitanteId);

    @Query("select r from Reserva r where r.professor.usuario.id = :usuarioId order by r.data desc, r.inicio desc")
    List<Reserva> doProfessor(Long usuarioId);

    List<Reserva> findByStatusOrderByCriadaEm(StatusReserva status);

    long countByStatus(StatusReserva status);

    long countBySolicitanteIdAndStatus(Long solicitanteId, StatusReserva status);

    @Query("select r from Reserva r where r.data <= :hoje and r.status in :status")
    List<Reserva> ateData(LocalDate hoje, Collection<StatusReserva> status);
}
